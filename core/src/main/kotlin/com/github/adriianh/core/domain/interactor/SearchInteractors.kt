package com.github.adriianh.core.domain.interactor

import com.github.adriianh.core.domain.usecase.*

data class SearchInteractors(
    val searchTracks: SearchTracksUseCase,
    val loadMoreTracks: LoadMoreTracksUseCase,
    val getTrack: GetTrackUseCase,
    val getLyrics: GetLyricsUseCase,
    val getSyncedLyrics: GetSyncedLyricsUseCase,
    val getSimilarTracks: GetSimilarTracksUseCase,
)
