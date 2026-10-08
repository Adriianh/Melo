package com.github.adriianh.melo.service

import android.util.Log
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.DiscordRpcManager
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "MeloDiscordRpc"

class AndroidDiscordRpcManager(
    playbackManager: PlaybackManager? = null,
    settingsRepository: SettingsRepository? = null,
    providedScope: CoroutineScope? = null,
) : DiscordRpcManager {
    private val scope: CoroutineScope =
        providedScope ?: CoroutineScope(
            Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, _ -> },
        )

    private val clientId = "1485113215905042515"
    private var gatewayClient: DiscordGatewayClient? = null
    private var currentToken: String? = null
    private var isEnabled: Boolean = false

    private var currentTrack: Track? = null
    private var isPlaying: Boolean = false
    private var currentPositionMs: Long? = null

    private var pendingTrackId: String? = null
    private var lastSentTrackId: String? = null
    private var lastSentArtworkUrl: String? = null
    private var lastSentIsPlaying: Boolean = false
    private var lastSentStartMillis: Long? = null
    private var pauseDebounceJob: Job? = null

    private var observer: Job? = null
    private var activityJob: Job? = null

    init {
        Log.d(TAG, "Initialized with playbackManager=${playbackManager != null}, settingsRepo=${settingsRepository != null}")
        if (playbackManager != null && settingsRepository != null) {
            startObservers(playbackManager, settingsRepository)
        }
    }

    private fun startObservers(
        playbackManager: PlaybackManager,
        settingsRepository: SettingsRepository,
    ) {
        observer =
            scope.launch {
                launch {
                    settingsRepository
                        .getSettingsFlow()
                        .map { Pair(it.discordRpcEnabled, it.discordRpcToken) }
                        .distinctUntilChanged()
                        .collect { (enabled, token) ->
                            Log.d(TAG, "Settings update: enabled=$enabled, token=${token?.take(6)}...")
                            isEnabled = enabled
                            val tokenChanged = token != currentToken
                            currentToken = token

                            if (!enabled || token.isNullOrBlank()) {
                                disconnect()
                            } else if (tokenChanged || gatewayClient == null) {
                                connect()
                            }
                        }
                }
                launch {
                    playbackManager.playbackState.collect { state ->
                        updateActivityInternal(
                            track = state.currentTrack,
                            playing = state.isPlaying,
                            positionMs = state.progressMs,
                            durationMs = state.durationMs,
                        )
                    }
                }
            }
    }

    override fun connect() {
        Log.d(TAG, "connect() called: isEnabled=$isEnabled, hasToken=${!currentToken.isNullOrBlank()}")
        if (!isEnabled || currentToken.isNullOrBlank()) return

        gatewayClient?.disconnect()
        gatewayClient =
            DiscordGatewayClient(currentToken!!, scope).also {
                it.connect()
            }
        currentTrack?.let { track ->
            if (isPlaying) {
                sendActivity(track, currentPositionMs ?: 0L, null)
            }
        }
    }

    override fun updateActivity(
        track: Track?,
        playing: Boolean,
        positionMs: Long?,
    ) {
        updateActivityInternal(track, playing, positionMs, null)
    }

    private fun updateActivityInternal(
        track: Track?,
        playing: Boolean,
        positionMs: Long?,
        durationMs: Long?,
    ) {
        if (!isEnabled || currentToken.isNullOrBlank()) return

        currentTrack = track
        isPlaying = playing
        currentPositionMs = positionMs

        if (track == null) {
            handlePauseOrStop(immediate = true)
            return
        }

        if (!playing) {
            handlePauseOrStop(immediate = false)
            return
        }

        pauseDebounceJob?.cancel()
        pauseDebounceJob = null

        val pos = (positionMs ?: 0L).coerceAtLeast(0L)
        val expectedPos = lastSentStartMillis?.let { System.currentTimeMillis() - it }
        val seeked = expectedPos != null && abs(pos - expectedPos) > 4000L

        val trackChanged = track.id != lastSentTrackId
        val playStateChanged = !lastSentIsPlaying
        val artworkChanged = track.artworkUrl != null && track.artworkUrl != lastSentArtworkUrl

        if (pendingTrackId == track.id && activityJob?.isActive == true && !seeked) {
            return
        }

        if (trackChanged || playStateChanged || seeked || artworkChanged) {
            Log.d(TAG, "updateActivity sending presence: track=${track.title}, seeked=$seeked")
            sendActivity(track, pos, durationMs)
        }
    }

    private fun handlePauseOrStop(immediate: Boolean) {
        pauseDebounceJob?.cancel()
        if (immediate) {
            activityJob?.cancel()
            activityJob = null
            pendingTrackId = null
            Log.d(TAG, "handlePauseOrStop: immediately clearing presence")
            gatewayClient?.updatePresence(null)
            lastSentTrackId = null
            lastSentArtworkUrl = null
            lastSentIsPlaying = false
            lastSentStartMillis = null
        } else {
            pauseDebounceJob =
                scope.launch {
                    delay(1500.milliseconds)
                    if (!isPlaying) {
                        activityJob?.cancel()
                        activityJob = null
                        pendingTrackId = null
                        Log.d(TAG, "handlePauseOrStop: clearing presence after debounce")
                        gatewayClient?.updatePresence(null)
                        lastSentTrackId = null
                        lastSentArtworkUrl = null
                        lastSentIsPlaying = false
                        lastSentStartMillis = null
                    }
                }
        }
    }

    private fun sendActivity(
        track: Track,
        positionMs: Long,
        durationMs: Long?,
    ) {
        activityJob?.cancel()
        pendingTrackId = track.id
        activityJob =
            scope.launch {
                try {
                    val client = gatewayClient ?: return@launch
                    val now = System.currentTimeMillis()
                    val startMillis = now - positionMs
                    val totalDuration =
                        if (track.durationMs > 0) track.durationMs else (durationMs ?: 0L)
                    val endMillis = if (totalDuration > 0) startMillis + totalDuration else null

                    val rawImageUrl =
                        track.artworkUrl?.takeIf {
                            it.startsWith("http://") || it.startsWith("https://")
                        }

                    val largeImage =
                        if (rawImageUrl != null) {
                            client.resolveExternalAsset(rawImageUrl, clientId) ?: "melo_logo"
                        } else {
                            "melo_logo"
                        }

                    val youtubeId = resolveYouTubeId(track)
                    val buttons = if (!youtubeId.isNullOrBlank()) listOf("Listen on YouTube") else null
                    val metadata =
                        if (!youtubeId.isNullOrBlank()) {
                            ActivityMetadata(buttonUrls = listOf("https://youtube.com/watch?v=$youtubeId"))
                        } else {
                            null
                        }

                    val activity =
                        ActivityData(
                            name = "Melo",
                            state = "by ${track.artist}".take(128),
                            details = track.title.take(128),
                            type = 2,
                            timestamps = TimestampsData(start = startMillis, end = endMillis),
                            assets =
                                AssetsData(
                                    largeImage = largeImage,
                                    largeText = track.album.takeIf { it.isNotBlank() } ?: track.title,
                                ),
                            buttons = buttons,
                            metadata = metadata,
                            applicationId = clientId,
                            statusDisplayType = 1,
                        )

                    Log.d(TAG, "sendActivity: updating presence for ${activity.name} with asset $largeImage")
                    client.updatePresence(PresenceData(activities = listOf(activity)))
                    lastSentTrackId = track.id
                    lastSentArtworkUrl = track.artworkUrl
                    lastSentIsPlaying = true
                    lastSentStartMillis = startMillis
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "sendActivity exception", e)
                } finally {
                    if (pendingTrackId == track.id) {
                        pendingTrackId = null
                    }
                }
            }
    }

    private fun resolveYouTubeId(track: Track): String? =
        when {
            !track.id.startsWith("local:") && !track.id.startsWith("piped:") -> track.id
            track.sourceId != null && !track.sourceId!!.startsWith("local:") -> track.sourceId
            else -> null
        }

    override fun disconnect() {
        pauseDebounceJob?.cancel()
        pauseDebounceJob = null
        activityJob?.cancel()
        activityJob = null
        pendingTrackId = null
        lastSentTrackId = null
        lastSentArtworkUrl = null
        lastSentIsPlaying = false
        lastSentStartMillis = null
        try {
            gatewayClient?.disconnect()
        } catch (_: Exception) {
        }
        gatewayClient = null
    }

    override fun release() {
        observer?.cancel()
        observer = null
        disconnect()
    }
}
