package com.github.adriianh.cli.tui.service

import com.github.adriianh.core.domain.model.Track
import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.event.impl.DisconnectedEvent
import dev.cbyrne.kdiscordipc.core.event.impl.ErrorEvent
import dev.cbyrne.kdiscordipc.core.event.impl.ReadyEvent
import dev.cbyrne.kdiscordipc.data.activity.ActivityType
import dev.cbyrne.kdiscordipc.data.activity.largeImage
import dev.cbyrne.kdiscordipc.data.activity.timestamps
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
import kotlin.math.pow

/**
 * Manages Discord Rich Presence using KDiscordIPC.
 *
 * Connects to a local Discord client via IPC and updates the user's
 * activity to reflect the currently playing track in Melo.
 */
class DiscordRpcManager(
    providedScope: CoroutineScope? = null
) {
    private var ipc: KDiscordIPC? = null
    private var isConnected: Boolean = false

    private val scope: CoroutineScope = providedScope ?: CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
            if (throwable is CancellationException) return@CoroutineExceptionHandler
            throwable.printStackTrace()
        }
    )
    private val clientId = "1485113215905042515"

    private var currentTrack: Track? = null
    private var isPlaying: Boolean = false
    private var startTime: Instant? = null
    private var activityJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 6
    private val initialReconnectDelayMs = 1_000L
    private val maxReconnectDelayMs = 30_000L
    private var manuallyDisconnected = false

    fun connect() {
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
                    currentTrack?.let { updateActivity(it, isPlaying) }
                }

                newIpc.on<DisconnectedEvent> {
                    isConnected = false
                    ipc = null
                    scheduleReconnect()
                }

                // Subscribe to library ErrorEvent (emitted for decode/socket errors)
                newIpc.on<ErrorEvent> {
                    isConnected = false
                    ipc = null
                    scheduleReconnect()
                }

                newIpc.connect()
            } catch (e: Throwable) {
                if (e !is CancellationException) {
                    e.printStackTrace()
                }
                ipc = null
                isConnected = false
            }
        }
    }

    fun updateActivity(track: Track?, playing: Boolean, positionMs: Long? = null) {
        val trackChanged = track?.id != currentTrack?.id
        if (trackChanged) {
            startTime = null
        }

        this.currentTrack = track
        this.isPlaying = playing

        if (track == null || !playing) {
            startTime = null
            activityJob?.cancel()
            activityJob = scope.launch {
                try {
                    if (isConnected) {
                        ipc?.activityManager?.clearActivity()
                    }
                } catch (_: Exception) { }
            }
            return
        }

        if (startTime == null || positionMs != null) {
            startTime = Instant.now().minusMillis(positionMs ?: 0L)
        }

        activityJob?.cancel()
        activityJob = scope.launch {
            try {
                if (isConnected) {
                    ipc?.activityManager?.setActivity(
                        details = track.title,
                        state = track.artist,
                    ) {
                        type = ActivityType.Listening
                        statusDisplayType = 1

                        val imageUrl = track.artworkUrl
                        if (imageUrl != null && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                            largeImage(imageUrl, track.album.takeIf { it.isNotBlank() })
                        } else {
                            largeImage("melo_logo", "Melo")
                        }

                        val currentStartTime = startTime ?: return@setActivity
                        val startMillis = currentStartTime.toEpochMilli()
                        if (track.durationMs > 0) {
                            timestamps(startMillis, startMillis + track.durationMs)
                        } else {
                            timestamps(startMillis)
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                ipc?.disconnect()
            } catch (_: Throwable) {
            } finally {
                ipc = null
                isConnected = false
                manuallyDisconnected = true
                reconnectJob?.cancel()
                reconnectJob = null
            }
        }
    }

    private fun scheduleReconnect() {
        if (manuallyDisconnected) return
        if (reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {
            while (reconnectAttempts < maxReconnectAttempts && !manuallyDisconnected) {
                val delayMs = min(
                    (initialReconnectDelayMs * 2.0.pow(reconnectAttempts.toDouble())).toLong(),
                    maxReconnectDelayMs
                )

                try {
                    delay(delayMs)
                } catch (_: CancellationException) {
                    return@launch
                }

                if (isConnected || ipc != null || manuallyDisconnected) break

                reconnectAttempts++
                try {
                    connect()
                    delay(2_000L)

                    if (isConnected) {
                        reconnectAttempts = 0
                        break
                    }
                } catch (_: Throwable) {
                }
            }
            reconnectJob = null
        }
    }
}