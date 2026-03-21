package com.github.adriianh.core.domain.interactor

import com.github.adriianh.core.domain.usecase.*

data class LibraryInteractors(
    val getFavorites: GetFavoritesUseCase,
    val addFavorite: AddFavoriteUseCase,
    val removeFavorite: RemoveFavoriteUseCase,
    val isFavorite: IsFavoriteUseCase,
    val getPlaylists: GetPlaylistsUseCase,
    val getPlaylistTracks: GetPlaylistTracksUseCase,
    val createPlaylist: CreatePlaylistUseCase,
    val renamePlaylist: RenamePlaylistUseCase,
    val deletePlaylist: DeletePlaylistUseCase,
    val addTrackToPlaylist: AddTrackToPlaylistUseCase,
    val removeTrackFromPlaylist: RemoveTrackFromPlaylistUseCase,
)
