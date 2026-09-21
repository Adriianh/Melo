package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.handleGlobalShortcuts
import com.github.adriianh.cli.tui.handler.toggleFavorite
import com.github.adriianh.cli.tui.isFavoriteEntity
import com.github.adriianh.core.domain.model.FavoriteEntityType
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.model.toFavoriteEntity
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch

internal fun MeloScreen.toggleEntityFavorite(entity: SearchResult) {
    val favEntity = when (entity) {
        is SearchResult.Album -> entity.toFavoriteEntity()
        is SearchResult.Artist -> entity.toFavoriteEntity()
        is SearchResult.Playlist -> entity.toFavoriteEntity()
        is SearchResult.Song -> {
            toggleFavorite(entity.track)
            return
        }
    }
    scope.launch {
        val currentlyFav = state.isFavoriteEntity(favEntity.id)
        val newFavState = !currentlyFav

        toggleFavoriteEntity?.invoke(favEntity)

        if (!newFavState) {
            appRunner()?.runOnRenderThread {
                val rawId = favEntity.id.removePrefix("piped:")
                val updatedRemoteAlbums = state.collections.remoteAlbums.filterNot {
                    it.id == favEntity.id || it.id.removePrefix("piped:") == rawId
                }
                val updatedRemoteArtists = state.collections.remoteArtists.filterNot {
                    it.id == favEntity.id || it.id.removePrefix("piped:") == rawId
                }
                val updatedRemotePlaylists = state.collections.remotePlaylists.filterNot {
                    it.id == favEntity.id || it.id.removePrefix("piped:") == rawId
                }
                state = state.copy(
                    collections = state.collections.copy(
                        remoteAlbums = updatedRemoteAlbums,
                        remoteArtists = updatedRemoteArtists,
                        remotePlaylists = updatedRemotePlaylists
                    )
                )
            }
        }

        val settings = settingsViewState.currentSettings
        val isLoggedIn = !settings.sessionCookies.isNullOrBlank()
        if (isLoggedIn && settings.syncLikesToYouTube) {
            val rawId = favEntity.id.removePrefix("piped:")
            try {
                when (favEntity.type) {
                    FavoriteEntityType.ALBUM -> toggleLikeAlbum?.invoke(rawId, newFavState)
                    FavoriteEntityType.ARTIST -> subscribeChannel?.invoke(rawId, newFavState)
                    FavoriteEntityType.PLAYLIST -> toggleLikePlaylist?.invoke(rawId, newFavState)
                }
            } catch (_: Exception) {
            }
        }
    }
}

/**
 * Sale del detalle de entidad devolviendo el foco a la pantalla de retorno.
 * Usado por el dashboard de artista y por la lista de tracks de la entidad.
 */
internal fun MeloScreen.exitEntityDetailToReturnScreen(
    actualDetail: ScreenState.EntityDetail
): EventResult {
    if (actualDetail.returnScreen is ScreenState.Home) {
        cachedHomeScreen = actualDetail.returnScreen
    } else if (actualDetail.returnScreen is ScreenState.Stats) {
        cachedStatsScreen = actualDetail.returnScreen
    }
    state = state.copy(
        screen = actualDetail.returnScreen,
        navigation = state.navigation.copy(activeSection = actualDetail.returnSection)
    )
    val targetFocus = when (actualDetail.returnScreen) {
        is ScreenState.Home -> "home-panel"
        is ScreenState.Search -> "results-panel"
        is ScreenState.Library -> "library-panel"
        is ScreenState.EntityDetail -> {
            if (actualDetail.returnScreen.entity is SearchResult.Artist) "artist-dashboard-list" else "entity-tracks-list"
        }

        else -> "home-panel"
    }
    appRunner()?.focusManager()?.setFocus(targetFocus)
    return EventResult.HANDLED
}

internal fun MeloScreen.handleEntityDetailKey(event: KeyEvent): EventResult {
    val isDescFocused = appRunner()?.focusManager()?.focusedId() == "desc-area"
    if (isDescFocused) {
        if (event.code() == KeyCode.ESCAPE || event.code() == KeyCode.TAB || (event.modifiers()
                .alt() && event.code() == KeyCode.LEFT)
        ) {
            val targetFocus = when (val curScreen = state.screen) {
                is ScreenState.Search -> "results-panel"
                is ScreenState.EntityDetail -> {
                    if (curScreen.entity is SearchResult.Artist) "artist-dashboard-list" else "entity-tracks-list"
                }

                else -> "results-panel"
            }
            appRunner()?.focusManager()?.setFocus(targetFocus)
            return EventResult.HANDLED
        }
        return handleGlobalShortcuts(event)
    }

    val actualDetail =
        state.screen as? ScreenState.EntityDetail ?: return handleGlobalShortcuts(event)

    if (event.isChar('F') || (event.modifiers().shift() && event.isCharIgnoreCase('f'))) {
        toggleEntityFavorite(actualDetail.entity)
        return EventResult.HANDLED
    }

    val handled = when (val entity = actualDetail.entity) {
        is SearchResult.Artist -> handleArtistDashboardKey(actualDetail, entity, event)
        else -> handleEntityTracksKey(actualDetail, event)
    }
    return if (handled == EventResult.HANDLED) handled else handleGlobalShortcuts(event)
}