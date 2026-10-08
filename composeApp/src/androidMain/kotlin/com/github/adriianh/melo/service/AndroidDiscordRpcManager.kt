package com.github.adriianh.melo.service

import android.util.Log
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.DiscordRpcManager
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
                        updateActivity(
                            track = state.currentTrack,
                            playing = state.isPlaying,
                            positionMs = state.progressMs,
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
        sendActivity(currentTrack, isPlaying, currentPositionMs)
    }

    override fun updateActivity(
        track: Track?,
        playing: Boolean,
        positionMs: Long?,
    ) {
        val trackChanged = track?.id != currentTrack?.id
        val playStateChanged = playing != isPlaying

        currentTrack = track
        isPlaying = playing
        currentPositionMs = positionMs

        if (!isEnabled || currentToken.isNullOrBlank()) return
        if (!trackChanged && !playStateChanged) return

        Log.d(TAG, "updateActivity triggered: track=${track?.title}, playing=$playing")
        sendActivity(track, playing, positionMs)
    }

    private fun sendActivity(
        track: Track?,
        playing: Boolean,
        positionMs: Long?,
    ) {
        activityJob?.cancel()
        activityJob =
            scope.launch {
                try {
                    val client = gatewayClient ?: return@launch
                    if (track == null || !playing) {
                        Log.d(TAG, "sendActivity: clearing presence")
                        client.updatePresence(null)
                        return@launch
                    }

                    val activity = createActivity(track, positionMs)
                    Log.d(TAG, "sendActivity: updating presence for ${activity.name}")
                    client.updatePresence(PresenceData(activities = listOf(activity)))
                } catch (e: Exception) {
                    Log.e(TAG, "sendActivity exception", e)
                }
            }
    }

    private fun createActivity(
        track: Track,
        positionMs: Long?,
    ): ActivityData {
        val now = System.currentTimeMillis()
        val pos = (positionMs ?: 0L).coerceAtLeast(0L)
        val startMillis = now - pos
        val endMillis = if (track.durationMs > 0) startMillis + track.durationMs else null

        val imageUrl =
            track.artworkUrl?.takeIf {
                it.startsWith("http://") || it.startsWith("https://")
            }

        val youtubeId = resolveYouTubeId(track)
        val buttons = if (!youtubeId.isNullOrBlank()) listOf("Listen on YouTube") else null
        val metadata =
            if (!youtubeId.isNullOrBlank()) {
                ActivityMetadata(buttonUrls = listOf("https://youtube.com/watch?v=$youtubeId"))
            } else {
                null
            }

        return ActivityData(
            name = "Melo",
            state = "by ${track.artist}".take(128),
            details = track.title.take(128),
            type = 2,
            timestamps = TimestampsData(start = startMillis, end = endMillis),
            assets =
                AssetsData(
                    largeImage = imageUrl ?: "melo_logo",
                    largeText = track.album.takeIf { it.isNotBlank() } ?: track.title,
                ),
            buttons = buttons,
            metadata = metadata,
            applicationId = clientId,
        )
    }

    private fun resolveYouTubeId(track: Track): String? =
        when {
            !track.id.startsWith("local:") && !track.id.startsWith("piped:") -> track.id
            track.sourceId != null && !track.sourceId!!.startsWith("local:") -> track.sourceId
            else -> null
        }

    override fun disconnect() {
        activityJob?.cancel()
        activityJob = null
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
