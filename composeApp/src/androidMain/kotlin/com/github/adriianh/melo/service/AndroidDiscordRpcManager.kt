package com.github.adriianh.melo.service

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.DiscordRpcManager
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.repository.SettingsRepository
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.my.kizzyrpc.model.Timestamps
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
    private var kizzyRpc: KizzyRPC? = null
    private var currentToken: String? = null
    private var isEnabled: Boolean = false

    private var currentTrack: Track? = null
    private var isPlaying: Boolean = false
    private var currentPositionMs: Long? = null

    private var observer: Job? = null
    private var activityJob: Job? = null

    init {
        if (playbackManager != null && settingsRepository != null) {
            startObservers(playbackManager, settingsRepository)
        }
    }

    private fun startObservers(
        playbackManager: PlaybackManager,
        settingsRepository: SettingsRepository,
    ) {
        observer = scope.launch {
            launch {
                settingsRepository.getSettingsFlow()
                    .map { Pair(it.discordRpcEnabled, it.discordRpcToken) }
                    .distinctUntilChanged()
                    .collect { (enabled, token) ->
                        isEnabled = enabled
                        val tokenChanged = token != currentToken
                        currentToken = token

                        if (!enabled || token.isNullOrBlank()) {
                            disconnect()
                        } else if (tokenChanged || kizzyRpc == null) {
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
        if (!isEnabled || currentToken.isNullOrBlank()) return

        scope.launch {
            try {
                kizzyRpc?.closeRPC()
                kizzyRpc = KizzyRPC(currentToken!!)
                sendActivity(currentTrack, isPlaying, currentPositionMs)
            } catch (_: Exception) {
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

        if (!isEnabled || currentToken.isNullOrBlank()) return
        if (!trackChanged && !playStateChanged) return

        sendActivity(track, playing, positionMs)
    }

    private fun sendActivity(
        track: Track?,
        playing: Boolean,
        positionMs: Long?,
    ) {
        activityJob?.cancel()
        activityJob = scope.launch {
            try {
                if (track == null || !playing) {
                    kizzyRpc?.closeRPC()
                    return@launch
                }

                val token = currentToken ?: return@launch
                val rpc = kizzyRpc ?: KizzyRPC(token).also { kizzyRpc = it }

                val now = System.currentTimeMillis()
                val pos = (positionMs ?: 0L).coerceAtLeast(0L)
                val startMillis = now - pos
                val endMillis = if (track.durationMs > 0) startMillis + track.durationMs else null

                val imageUrl = track.artworkUrl?.takeIf {
                    it.startsWith("http://") || it.startsWith("https://")
                }

                val activity = Activity(
                    name = "Melo",
                    state = "by ${track.artist}".take(128),
                    details = track.title.take(128),
                    type = 2,
                    timestamps = Timestamps(start = startMillis, end = endMillis),
                    assets = Assets(
                        largeImage = imageUrl ?: "melo_logo",
                        smallImage = null,
                        largeText = track.album.takeIf { it.isNotBlank() } ?: track.title,
                        smallText = null,
                    ),
                    buttons = null,
                    metadata = null,
                    applicationId = clientId,
                )

                if (rpc.isRpcRunning()) {
                    rpc.closeRPC()
                }
                rpc.setActivity(activity)
            } catch (_: Exception) {
            }
        }
    }

    override fun disconnect() {
        activityJob?.cancel()
        activityJob = null
        try {
            kizzyRpc?.closeRPC()
        } catch (_: Exception) {
        }
        kizzyRpc = null
    }

    override fun release() {
        observer?.cancel()
        observer = null
        disconnect()
    }
}
