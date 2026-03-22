package com.github.adriianh.cli.tui.service

import com.github.adriianh.core.domain.model.Track
import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.event.impl.DisconnectedEvent
import dev.cbyrne.kdiscordipc.core.event.impl.ReadyEvent
import dev.cbyrne.kdiscordipc.data.activity.ActivityType
import dev.cbyrne.kdiscordipc.data.activity.largeImage
import dev.cbyrne.kdiscordipc.data.activity.timestamps
import kotlinx.coroutines.*
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

/**
 * Manages Discord Rich Presence using KDiscordIPC.
 *
 * Connects to a local Discord client via IPC and updates the user's
 * activity to reflect the currently playing track in Melo.
 */
class DiscordRpcManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private var ipc: KDiscordIPC? = null
    private val clientId = "1485113215905042515"

    private var currentTrack: Track? = null
    private var isPlaying: Boolean = false
    private var startTime: Instant? = null
    private var isConnected: Boolean = false
    private var activityJob: Job? = null

    fun connect() {
        scope.launch {
            try {
                if (isConnected || ipc != null) return@launch
                
                val newIpc = KDiscordIPC(clientId).also { ipc = it }
                newIpc.on<ReadyEvent> {
                    isConnected = true
                    currentTrack?.let { updateActivity(it, isPlaying) }
                }
                
                newIpc.on<DisconnectedEvent> {
                    isConnected = false
                    ipc = null
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
            }
        }
    }
}