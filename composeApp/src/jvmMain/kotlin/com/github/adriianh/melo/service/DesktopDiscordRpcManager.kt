package com.github.adriianh.melo.service

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.DiscordRpcManager
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.repository.SettingsRepository
import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.event.impl.DisconnectedEvent
import dev.cbyrne.kdiscordipc.core.event.impl.ErrorEvent
import dev.cbyrne.kdiscordipc.core.event.impl.ReadyEvent
import dev.cbyrne.kdiscordipc.data.activity.ActivityType
import dev.cbyrne.kdiscordipc.data.activity.StatusDisplayType
import dev.cbyrne.kdiscordipc.data.activity.activity
import dev.cbyrne.kdiscordipc.data.activity.button
import dev.cbyrne.kdiscordipc.data.activity.largeImage
import dev.cbyrne.kdiscordipc.data.activity.timestamps
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
import kotlin.time.Duration.Companion.milliseconds

class DesktopDiscordRpcManager(
    playbackManager: PlaybackManager? = null,
    settingsRepository: SettingsRepository? = null,
    providedScope: CoroutineScope? = null,
) : DiscordRpcManager {
    private var ipc: KDiscordIPC? = null
    private var isConnected: Boolean = false

    private val scope: CoroutineScope =
        providedScope ?: CoroutineScope(
            Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, _ -> },
        )

    private val clientId = "1485113215905042515"

    private var currentTrack: Track? = null
    private var isPlaying: Boolean = false
    private var activityJob: Job? = null
    private var reconnectJob: Job? = null
    private var observerJob: Job? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 6
    private val initialReconnectDelayMs = 1_000L
    private val maxReconnectDelayMs = 30_000L
    private var manuallyDisconnected = false
    private var currentPositionMs: Long? = null

    init {
        if (playbackManager != null && settingsRepository != null) {
            startObservers(playbackManager, settingsRepository)
        }
    }

    private fun startObservers(
        playbackManager: PlaybackManager,
        settingsRepository: SettingsRepository,
    ) {
        observerJob =
            scope.launch {
                launch {
                    settingsRepository
                        .getSettingsFlow()
                        .map { it.discordRpcEnabled }
                        .distinctUntilChanged()
                        .collect { enabled ->
                            if (enabled) {
                                connect()
                            } else {
                                disconnect()
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
        scope.launch {
            try {
                manuallyDisconnected = false
                if (isConnected || ipc != null) return@launch

                reconnectJob?.cancel()
                reconnectJob = null

                val newIpc = KDiscordIPC(clientId).also { ipc = it }

                newIpc.on<ReadyEvent> {
                    isConnected = true
                    reconnectAttempts = 0
                    reconnectJob?.cancel()
                    reconnectJob = null
                    sendActivity(currentTrack, isPlaying, currentPositionMs)
                }

                newIpc.on<DisconnectedEvent> {
                    isConnected = false
                    ipc = null
                    scheduleReconnect()
                }

                newIpc.on<ErrorEvent> {
                    isConnected = false
                    ipc = null
                    scheduleReconnect()
                }

                newIpc.connect()
            } catch (_: Throwable) {
                isConnected = false
                ipc = null
                scheduleReconnect()
            }
        }
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

        if (!isConnected || ipc == null) return
        if (!trackChanged && !playStateChanged) return

        sendActivity(track, playing, positionMs)
    }

    private fun sendActivity(
        track: Track?,
        playing: Boolean,
        positionMs: Long?,
    ) {
        val currentIpc = ipc ?: return
        activityJob?.cancel()
        activityJob =
            scope.launch {
                try {
                    if (track == null || !playing) {
                        currentIpc.activityManager.clearActivity()
                        return@launch
                    }

                    val activity =
                        activity(
                            details = track.title.take(128),
                            state = "by ${track.artist}".take(128),
                        ) {
                            type = ActivityType.Listening
                            statusDisplayType(StatusDisplayType.State)

                            val imageUrl =
                                track.artworkUrl?.takeIf {
                                    it.startsWith("http://") || it.startsWith("https://")
                                }
                            if (imageUrl != null) {
                                val imageText = track.album.takeIf { it.isNotBlank() } ?: track.title
                                largeImage(imageUrl, imageText)
                            } else {
                                largeImage("melo_logo", "Melo")
                            }

                            val now = System.currentTimeMillis()
                            val pos = (positionMs ?: 0L).coerceAtLeast(0L)
                            val startMillis = now - pos
                            if (track.durationMs > 0) {
                                timestamps(startMillis, startMillis + track.durationMs)
                            } else {
                                timestamps(startMillis)
                            }

                            val youtubeId = resolveYouTubeId(track)
                            if (!youtubeId.isNullOrBlank()) {
                                button("Listen on YouTube", "https://youtube.com/watch?v=$youtubeId")
                            }
                        }

                    currentIpc.activityManager.setActivity(activity)
                } catch (_: Exception) {
                }
            }
    }

    private fun resolveYouTubeId(track: Track): String? =
        when {
            !track.id.startsWith("local:") && !track.id.startsWith("piped:") -> track.id
            track.sourceId != null && !track.sourceId!!.startsWith("local:") -> track.sourceId
            else -> null
        }

    private val canReconnect: Boolean
        get() = !manuallyDisconnected && !isConnected && ipc == null

    private fun scheduleReconnect() {
        if (!canReconnect) return
        if (reconnectJob?.isActive == true) return

        reconnectJob =
            scope.launch {
                while (reconnectAttempts < maxReconnectAttempts && canReconnect) {
                    val delayMs =
                        (initialReconnectDelayMs * (1L shl reconnectAttempts))
                            .coerceAtMost(maxReconnectDelayMs)
                    try {
                        delay(delayMs.milliseconds)
                    } catch (_: CancellationException) {
                        return@launch
                    }

                    if (!canReconnect) break

                    reconnectAttempts++
                    try {
                        connect()
                        delay(2000.milliseconds)
                        if (isConnected) {
                            reconnectAttempts = 0
                        }
                    } catch (_: Throwable) {
                    }
                }
                reconnectJob = null
            }
    }

    override fun disconnect() {
        manuallyDisconnected = true
        reconnectJob?.cancel()
        reconnectJob = null
        activityJob?.cancel()
        activityJob = null
        try {
            ipc?.disconnect()
        } catch (_: Exception) {
        }
        ipc = null
        isConnected = false
    }

    override fun release() {
        observerJob?.cancel()
        observerJob = null
        disconnect()
    }
}
