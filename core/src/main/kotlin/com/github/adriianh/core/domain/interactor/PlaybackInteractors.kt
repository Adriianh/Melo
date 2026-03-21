package com.github.adriianh.core.domain.interactor

import com.github.adriianh.core.domain.usecase.playback.*

data class PlaybackInteractors(
    val getRecentTracks: GetRecentTracksUseCase,
    val getStream: GetStreamUseCase,
    val recordPlay: RecordPlayUseCase,
    val updateNowPlaying: UpdateNowPlayingUseCase,
    val scrobble: ScrobbleUseCase,
)
