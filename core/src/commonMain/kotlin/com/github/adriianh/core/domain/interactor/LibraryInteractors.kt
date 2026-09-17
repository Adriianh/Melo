package com.github.adriianh.core.domain.interactor

import com.github.adriianh.core.domain.usecase.library.AddFavoriteUseCase
import com.github.adriianh.core.domain.usecase.library.AddTrackToPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.CreatePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.DeletePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.GetFavoritesUseCase
import com.github.adriianh.core.domain.usecase.library.GetLikedSongsUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistTracksUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.IsFavoriteUseCase
import com.github.adriianh.core.domain.usecase.library.RemoveFavoriteUseCase
import com.github.adriianh.core.domain.usecase.library.RemoveTrackFromPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.RenamePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeTrackUseCase

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
    val getLikedSongs: GetLikedSongsUseCase? = null,
    val getUserPlaylists: GetUserPlaylistsUseCase? = null,
    val toggleLikeTrack: ToggleLikeTrackUseCase? = null,
)
