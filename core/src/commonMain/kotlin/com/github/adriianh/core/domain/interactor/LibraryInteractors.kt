package com.github.adriianh.core.domain.interactor

import com.github.adriianh.core.domain.usecase.library.AddFavoriteEntityUseCase
import com.github.adriianh.core.domain.usecase.library.AddFavoriteUseCase
import com.github.adriianh.core.domain.usecase.library.AddTrackToPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.CreatePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.DeletePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.GetFavoriteEntitiesUseCase
import com.github.adriianh.core.domain.usecase.library.GetFavoritesUseCase
import com.github.adriianh.core.domain.usecase.library.GetLikedSongsUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistTracksUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetRemoteHistoryUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserAlbumsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserArtistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.IsFavoriteEntityUseCase
import com.github.adriianh.core.domain.usecase.library.IsFavoriteUseCase
import com.github.adriianh.core.domain.usecase.library.RemoveFavoriteEntityUseCase
import com.github.adriianh.core.domain.usecase.library.RemoveFavoriteUseCase
import com.github.adriianh.core.domain.usecase.library.RemoveTrackFromPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.RenamePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.SubscribeChannelUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleFavoriteEntityUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeAlbumUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikePlaylistUseCase
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
    val getUserAlbums: GetUserAlbumsUseCase? = null,
    val getUserArtists: GetUserArtistsUseCase? = null,
    val toggleLikeAlbum: ToggleLikeAlbumUseCase? = null,
    val toggleLikePlaylist: ToggleLikePlaylistUseCase? = null,
    val subscribeChannel: SubscribeChannelUseCase? = null,
    val getFavoriteEntities: GetFavoriteEntitiesUseCase? = null,
    val addFavoriteEntity: AddFavoriteEntityUseCase? = null,
    val removeFavoriteEntity: RemoveFavoriteEntityUseCase? = null,
    val isFavoriteEntity: IsFavoriteEntityUseCase? = null,
    val toggleFavoriteEntity: ToggleFavoriteEntityUseCase? = null,
    val getRemoteHistory: GetRemoteHistoryUseCase? = null,
)