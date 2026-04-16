package com.github.adriianh.core.domain.interactor

import com.github.adriianh.core.domain.usecase.search.GetArtistTagsUseCase
import com.github.adriianh.core.domain.usecase.search.GetEntityDetailsUseCase
import com.github.adriianh.core.domain.usecase.search.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.GetSearchHistoryUseCase
import com.github.adriianh.core.domain.usecase.search.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.search.GetSyncedLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrackUseCase
import com.github.adriianh.core.domain.usecase.search.LoadMoreAlbumsUseCase
import com.github.adriianh.core.domain.usecase.search.LoadMoreArtistsUseCase
import com.github.adriianh.core.domain.usecase.search.LoadMorePlaylistsUseCase
import com.github.adriianh.core.domain.usecase.search.LoadMoreTracksUseCase
import com.github.adriianh.core.domain.usecase.search.SaveSearchQueryUseCase
import com.github.adriianh.core.domain.usecase.search.SearchAlbumsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchArtistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase

data class SearchInteractors(
    val searchTracks: SearchTracksUseCase,
    val searchAlbums: SearchAlbumsUseCase,
    val searchArtists: SearchArtistsUseCase,
    val searchPlaylists: SearchPlaylistsUseCase,
    val loadMoreTracks: LoadMoreTracksUseCase,
    val loadMoreAlbums: LoadMoreAlbumsUseCase,
    val loadMoreArtists: LoadMoreArtistsUseCase,
    val loadMorePlaylists: LoadMorePlaylistsUseCase,
    val getSearchHistory: GetSearchHistoryUseCase,
    val saveSearchQuery: SaveSearchQueryUseCase,
    val getTrack: GetTrackUseCase,
    val getLyrics: GetLyricsUseCase,
    val getSyncedLyrics: GetSyncedLyricsUseCase,
    val getSimilarTracks: GetSimilarTracksUseCase,
    val getEntityDetails: GetEntityDetailsUseCase,
    val getArtistTags: GetArtistTagsUseCase,
)