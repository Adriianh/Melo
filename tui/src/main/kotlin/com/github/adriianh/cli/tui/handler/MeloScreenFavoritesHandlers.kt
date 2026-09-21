package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.FavoritesSubTab
import com.github.adriianh.cli.tui.LibraryFavoriteEntityItem
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.allFavoriteEntities
import com.github.adriianh.cli.tui.filteredAndSortedFavorites
import com.github.adriianh.cli.tui.handler.playback.playList
import com.github.adriianh.cli.tui.handler.search.openEntityDetails
import com.github.adriianh.core.domain.model.FavoriteEntityType
import com.github.adriianh.core.domain.model.toSearchResult
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch

/**
 * Handles key events while the Favorites tab of the Library screen is focused:
 * sub-tab switching, typing, entity collections (albums/artists/playlists) and songs.
 */
internal fun MeloScreen.handleFavoritesKey(event: KeyEvent): EventResult {
    val actualState = state.screen as? ScreenState.Library ?: return handleGlobalShortcuts(event)

    if (actualState.isTyping) return handleFavoritesTypingKey(event)

    if (switchFavoritesSubTab(event)) return EventResult.HANDLED

    if (actualState.favoritesSubTab != FavoritesSubTab.SONGS) {
        val result = handleFavoriteEntitiesKey(actualState, event)
        return if (result == EventResult.UNHANDLED) handleGlobalShortcuts(event) else result
    }

    return handleFavoriteSongsKey(event)
}

/** Switches the favorites sub-tab (left/right, h/l or Tab). Returns true if the event was consumed. */
private fun MeloScreen.switchFavoritesSubTab(event: KeyEvent): Boolean {
    val step = when {
        event.code() == KeyCode.LEFT || event.isChar('h') ||
                (event.modifiers().shift() && event.code() == KeyCode.TAB) -> -1

        event.code() == KeyCode.RIGHT || event.isChar('l') || event.code() == KeyCode.TAB -> 1
        else -> return false
    }
    updateScreen<ScreenState.Library> { state ->
        state.copy(
            favoritesSubTab = if (step < 0) state.favoritesSubTab.previous() else state.favoritesSubTab.next(),
            selectedIndex = 0
        )
    }
    favoritesList.selected(0)
    return true
}

private fun MeloScreen.handleFavoritesTypingKey(event: KeyEvent): EventResult {
    when {
        event.code() == KeyCode.ENTER -> {
            updateScreen<ScreenState.Library> { it.copy(isTyping = false) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    isTyping = false,
                    favoritesSearchQuery = ""
                )
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.BACKSPACE -> {
            updateScreen<ScreenState.Library> {
                it.copy(favoritesSearchQuery = it.favoritesSearchQuery.dropLast(1))
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR -> {
            val text = event.string()
            updateScreen<ScreenState.Library> { it.copy(favoritesSearchQuery = it.favoritesSearchQuery + text) }
            return EventResult.HANDLED
        }
    }
    return EventResult.HANDLED
}

/** Handles navigation and actions for the entity sub-tabs (albums, artists, playlists). */
private fun MeloScreen.handleFavoriteEntitiesKey(
    actualState: ScreenState.Library,
    event: KeyEvent
): EventResult {
    val items = state.allFavoriteEntities(actualState.favoritesSubTab)
    val listSize = items.size
    when {
        event.matches(Actions.MOVE_DOWN) && listSize > 0 -> {
            favoritesList.selected(minOf(listSize - 1, favoritesList.selected() + 1))
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) && listSize > 0 -> {
            favoritesList.selected(maxOf(0, favoritesList.selected() - 1))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER && listSize > 0 -> {
            items.getOrNull(favoritesList.selected())?.let {
                openEntityDetails(it.entity.toSearchResult())
            }
            return EventResult.HANDLED
        }

        (event.isCharIgnoreCase('f') || event.code() == KeyCode.DELETE) && listSize > 0 -> {
            items.getOrNull(favoritesList.selected())?.let { removeFavoriteEntityWithSync(it) }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

/** Removes a favorite entity, syncing remote collections and YouTube likes when needed. */
private fun MeloScreen.removeFavoriteEntityWithSync(item: LibraryFavoriteEntityItem) {
    val entity = item.entity
    scope.launch {
        removeFavoriteEntity?.invoke(entity.id)
        if (item.isRemote) {
            appRunner()?.runOnRenderThread {
                val rawId = entity.id.removePrefix("piped:")
                val updatedRemoteAlbums = state.collections.remoteAlbums.filterNot {
                    it.id == entity.id || it.id.removePrefix("piped:") == rawId
                }
                val updatedRemoteArtists = state.collections.remoteArtists.filterNot {
                    it.id == entity.id || it.id.removePrefix("piped:") == rawId
                }
                val updatedRemotePlaylists = state.collections.remotePlaylists.filterNot {
                    it.id == entity.id || it.id.removePrefix("piped:") == rawId
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
            val rawId = entity.id.removePrefix("piped:")
            try {
                when (entity.type) {
                    FavoriteEntityType.ALBUM -> toggleLikeAlbum?.invoke(rawId, false)

                    FavoriteEntityType.ARTIST -> subscribeChannel?.invoke(rawId, false)

                    FavoriteEntityType.PLAYLIST -> toggleLikePlaylist?.invoke(rawId, false)
                }
            } catch (_: Exception) {
            }
        }
    }
}

/** Handles the songs sub-tab: filters, sorting, playback and selection actions. */
private fun MeloScreen.handleFavoriteSongsKey(event: KeyEvent): EventResult {
    val actualState = state.screen as? ScreenState.Library ?: return handleGlobalShortcuts(event)
    val filtered = state.filteredAndSortedFavorites(
        sourceFilter = actualState.favoritesSourceFilter,
        sortOrder = actualState.favoritesSortOrder,
        sortDirection = actualState.favoritesSortDirection,
        query = actualState.favoritesSearchQuery
    )

    when {
        event.isCtrlF() -> {
            updateScreen<ScreenState.Library> { it.copy(isTyping = true) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('s') -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    favoritesSourceFilter = it.favoritesSourceFilter.next(),
                    selectedIndex = 0
                )
            }
            favoritesList.selected(0)
            return EventResult.HANDLED
        }

        event.isChar('O') || (event.modifiers().shift() && event.isCharIgnoreCase('o')) -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    favoritesSortDirection = it.favoritesSortDirection.toggle(),
                    selectedIndex = 0
                )
            }
            favoritesList.selected(0)
            return EventResult.HANDLED
        }

        event.isChar('o') && !event.modifiers().shift() -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    favoritesSortOrder = it.favoritesSortOrder.next(),
                    selectedIndex = 0
                )
            }
            favoritesList.selected(0)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            if (actualState.favoritesSearchQuery.isNotEmpty()) {
                updateScreen<ScreenState.Library> { it.copy(favoritesSearchQuery = "") }
                return EventResult.HANDLED
            }
        }

        event.matches(Actions.MOVE_DOWN) -> {
            favoritesList.selected(
                minOf(
                    filtered.lastIndex.coerceAtLeast(0),
                    favoritesList.selected() + 1
                )
            )
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            favoritesList.selected(maxOf(0, favoritesList.selected() - 1))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            val tracks = filtered.map { it.track }
            val idx = favoritesList.selected()
            if (idx in tracks.indices) playList(tracks, idx)
            return EventResult.HANDLED
        }
    }

    val actionResult = handleTrackListSelectionActions(
        event = event,
        tracks = filtered.map { it.track },
        selectedIndex = favoritesList.selected(),
        onFavorite = { removeFavoriteTrack(it) }
    )
    if (actionResult == EventResult.HANDLED) return actionResult

    if (event.isCharIgnoreCase('y')) {
        syncYouTubeLibrary()
        return EventResult.HANDLED
    }

    return handleGlobalShortcuts(event)
}