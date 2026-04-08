package com.github.adriianh.core.domain.interactor

import com.github.adriianh.core.domain.usecase.search.*

data class SearchInteractors(
    val searchTracks: SearchTracksUseCase,
    val searchAlbums: SearchAlbumsUseCase,
    val searchArtists: SearchArtistsUseCase,
    val searchPlaylists: SearchPlaylistsUseCase,
    val loadMoreTracks: LoadMoreTracksUseCase,
    val getTrack: GetTrackUseCase,
    val getLyrics: GetLyricsUseCase,
    val getSyncedLyrics: GetSyncedLyricsUseCase,
    val getSimilarTracks: GetSimilarTracksUseCase,
)
