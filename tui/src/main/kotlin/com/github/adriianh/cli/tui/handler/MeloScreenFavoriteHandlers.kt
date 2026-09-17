package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.launch

internal fun MeloScreen.toggleFavorite(track: Track) {
    scope.launch {
        val wasFav = isFavoriteUseCase(track.id)
        if (wasFav) removeFavorite(track.id) else addFavorite(track)
        val isFav = !wasFav
        appRunner()?.runOnRenderThread { state = state.copy(player = state.player.copy(isFavorite = isFav)) }

        val settings = settingsViewState.currentSettings
        val isLoggedIn = !settings.sessionCookies.isNullOrBlank()
        if (isLoggedIn && settings.syncLikesToYouTube) {
            val rawVideoId = track.sourceId?.takeIf { it.isNotBlank() }
                ?: track.id.removePrefix("piped:").takeIf {
                    !it.startsWith("local:") && !it.startsWith("itunes:") && !it.startsWith("spotify:")
                }
            if (rawVideoId != null) {
                try {
                    toggleLikeTrack?.invoke(rawVideoId, isFav)
                } catch (_: Exception) {
                }
            }
        }
    }
}

internal fun MeloScreen.removeFavoriteTrack(track: Track) {
    scope.launch {
        removeFavorite(track.id)
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
        val isFav = isFavoriteUseCase(trackId)
        appRunner()?.runOnRenderThread { state = state.copy(player = state.player.copy(isFavorite = isFav)) }
    }
}

internal fun MeloScreen.syncYouTubeFavorites() {
    val settings = settingsViewState.currentSettings
    val isLoggedIn = !settings.sessionCookies.isNullOrBlank()
    if (!isLoggedIn) return
    scope.launch {
        try {
            val result = getLikedSongs?.invoke() ?: return@launch
            val remoteSongs = result.getOrNull().orEmpty()
            if (remoteSongs.isNotEmpty()) {
                val currentFavIds = state.collections.favorites.flatMap {
                    listOf(
                        it.id,
                        it.id.removePrefix("piped:"),
                        "piped:${it.id.removePrefix("piped:")}",
                        it.sourceId.orEmpty()
                    )
                }.filter { it.isNotBlank() }.toSet()

                for (track in remoteSongs) {
                    val rawId = track.sourceId?.takeIf { it.isNotBlank() } ?: track.id.removePrefix(
                        "piped:"
                    )
                    if (rawId !in currentFavIds && track.id !in currentFavIds) {
                        addFavorite(track)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}