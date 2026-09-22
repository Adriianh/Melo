package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.component.screen.handleMediaSessionNext
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionPlayPause
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionPrevious
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionStop
import com.github.adriianh.cli.tui.player.AudioPlayer
import com.github.adriianh.core.domain.player.JvmMediaSessionManager
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.data.player.PlaybackManagerImpl
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Builders for the shared media/playback components owned by [MeloScreen].
 */

internal fun MeloScreen.buildMediaSession(): JvmMediaSessionManager = JvmMediaSessionManager(
    httpClient = httpClient,
    onPlayPause = ::handleMediaSessionPlayPause,
    onNext = ::handleMediaSessionNext,
    onPrevious = ::handleMediaSessionPrevious,
    onStop = ::handleMediaSessionStop,
)

internal fun MeloScreen.buildPlaybackManager(dispatcher: CoroutineDispatcher): PlaybackManager =
    PlaybackManagerImpl(
        meloPlayer = audioPlayer,
        getStreamUseCase = getStream,
        scope = scope,
        getRadioUseCase = discoveryInteractors.getRadio,
        getSettingsUseCase = getSettings,
        offlineRepository = offlineRepository,
        updateSettingsUseCase = updateSettings,
        saveSessionUseCase = saveSession,
        restoreSessionUseCase = restoreSession,
        clearSessionUseCase = clearSession,
        ioDispatcher = dispatcher,
    )

internal fun MeloScreen.buildAudioPlayer(): AudioPlayer = AudioPlayer(
    scope = scope,
    onError = { err ->
        appRunner()?.runOnRenderThread {
            state = state.copy(
                player = state.player.copy(
                    audioError = err.message ?: "Playback error",
                    isLoadingAudio = false,
                )
            )
        }
    },
)