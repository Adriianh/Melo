package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.isFavoriteTrack
import com.github.adriianh.cli.tui.util.ToastKind
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

internal fun MeloScreen.toggleFavorite(track: Track, showConfirmation: Boolean = true) {
    scope.launch {
        val rawId = track.sourceId?.takeIf { it.isNotBlank() } ?: track.id.removePrefix("piped:")
        val isRemoteFav = state.collections.remoteFavorites.any {
            it.id == track.id || it.id == rawId || it.id.removePrefix("piped:") == rawId ||
                    (it.sourceId != null && (it.sourceId == track.id || it.sourceId == rawId))
        }
        val isLocalFav = isFavoriteUseCase(track.id)
        val currentlyFav = isLocalFav || isRemoteFav
        val newFavState = !currentlyFav

        if (isLocalFav) {
            removeFavorite(track.id)
        } else if (newFavState) {
            addFavorite(track)
        }

        if (isRemoteFav) {
            appRunner()?.runOnRenderThread {
                val updatedRemote = state.collections.remoteFavorites.filterNot {
                    it.id == track.id || it.id == rawId || it.id.removePrefix("piped:") == rawId ||
                            it.sourceId == track.id || it.sourceId == rawId
                }
                state =
                    state.copy(collections = state.collections.copy(remoteFavorites = updatedRemote))
            }
        }

        appRunner()?.runOnRenderThread {
            state = state.copy(player = state.player.copy(isFavorite = newFavState))
        }

        if (showConfirmation) {
            appRunner()?.runOnRenderThread {
                showToast(
                    message = if (newFavState) {
                        "${track.title} — ${track.artist}"
                    } else {
                        "Removed from favorites: ${track.title}"
                    },
                    kind = ToastKind.HEART
                )
            }
        }

        val settings = settingsViewState.currentSettings
        val isLoggedIn = !settings.sessionCookies.isNullOrBlank()
        if (isLoggedIn && settings.syncLikesToYouTube) {
            val rawVideoId = track.sourceId?.takeIf { it.isNotBlank() }
                ?: track.id.removePrefix("piped:").takeIf {
                    !it.startsWith("local:") && !it.startsWith("itunes:") && !it.startsWith("spotify:")
                }
            if (rawVideoId != null) {
                try {
                    toggleLikeTrack?.invoke(rawVideoId, newFavState)
                } catch (_: Exception) {
                }
            }
        }
    }
}

internal fun MeloScreen.removeFavoriteTrack(track: Track, showConfirmation: Boolean = true) {
    scope.launch {
        removeFavorite(track.id)
        val rawId = track.sourceId?.takeIf { it.isNotBlank() } ?: track.id.removePrefix("piped:")
        appRunner()?.runOnRenderThread {
            val updatedRemote = state.collections.remoteFavorites.filterNot {
                it.id == track.id || it.id == rawId || it.id.removePrefix("piped:") == rawId ||
                        it.sourceId == track.id || it.sourceId == rawId
            }
            val isNowPlaying = state.player.nowPlaying?.id == track.id
            state = state.copy(
                collections = state.collections.copy(remoteFavorites = updatedRemote),
                player = if (isNowPlaying) state.player.copy(isFavorite = false) else state.player
            )
            if (showConfirmation) {
                showToast("Removed from favorites: ${track.title}", ToastKind.HEART)
            }
        }
        val settings = settingsViewState.currentSettings
        val isLoggedIn = !settings.sessionCookies.isNullOrBlank()
        if (isLoggedIn && settings.syncLikesToYouTube) {
            val rawVideoId = track.sourceId?.takeIf { it.isNotBlank() }
                ?: track.id.removePrefix("piped:").takeIf {
                    !it.startsWith("local:") && !it.startsWith("itunes:") && !it.startsWith("spotify:")
                }
            if (rawVideoId != null) {
                try {
                    toggleLikeTrack?.invoke(rawVideoId, false)
                } catch (_: Exception) {
                }
            }
        }
    }
}

internal fun MeloScreen.checkIsFavorite(trackId: String) {
    scope.launch {
        val isFav = state.isFavoriteTrack(trackId)
        appRunner()?.runOnRenderThread { state = state.copy(player = state.player.copy(isFavorite = isFav)) }
    }
}

internal fun MeloScreen.syncYouTubeFavorites() {
    val settings = settingsViewState.currentSettings
    val isLoggedIn = !settings.sessionCookies.isNullOrBlank()
    if (!isLoggedIn) {
        appRunner()?.runOnRenderThread {
            state = state.copy(
                collections = state.collections.copy(
                    remoteFavorites = emptyList(),
                    remoteAlbums = emptyList(),
                    remoteArtists = emptyList(),
                    remoteRecentTracks = emptyList(),
                )
            )
        }
        return
    }
    scope.launch {
        try {
            val songsDeferred = async { getLikedSongs?.invoke()?.getOrNull().orEmpty() }
            val albumsDeferred = async { getUserAlbums?.invoke()?.getOrNull().orEmpty() }
            val artistsDeferred = async { getUserArtists?.invoke()?.getOrNull().orEmpty() }

            val remoteSongs = songsDeferred.await()
            val remoteAlbums = albumsDeferred.await()
            val remoteArtists = artistsDeferred.await()

            appRunner()?.runOnRenderThread {
                state = state.copy(
                    collections = state.collections.copy(
                        remoteFavorites = remoteSongs,
                        remoteAlbums = remoteAlbums,
                        remoteArtists = remoteArtists
                    )
                )
            }
        } catch (_: Exception) {
        }
    }
}

internal fun MeloScreen.syncYouTubeHistory() {
    val settings = settingsViewState.currentSettings
    val isLoggedIn = !settings.sessionCookies.isNullOrBlank()
    if (!isLoggedIn || !settings.syncHistoryToYouTube) {
        appRunner()?.runOnRenderThread {
            state = state.copy(
                collections = state.collections.copy(remoteRecentTracks = emptyList())
            )
        }
        return
    }
    scope.launch {
        try {
            val remoteHistory = getRemoteHistory?.invoke()?.getOrNull().orEmpty()
            appRunner()?.runOnRenderThread {
                state = state.copy(
                    collections = state.collections.copy(remoteRecentTracks = remoteHistory)
                )
            }
        } catch (_: Exception) {
        }
    }
}