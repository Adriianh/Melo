package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.handleGlobalShortcuts
import com.github.adriianh.cli.tui.handler.matchesAction
import com.github.adriianh.core.domain.model.MeloAction
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal fun MeloScreen.handleSearchQueryChange(query: String) {
    try {
        suggestionsJob?.cancel()
    } catch (_: Exception) {
    }
    suggestionsJob = null

    if (query.isBlank()) {
        updateScreen<ScreenState.Search> {
            it.copy(query = query, isShowingSuggestions = true, selectedSuggestionIndex = null)
        }
        suggestionsJob = scope.launch {
            try {
                val rawSuggestions =
                    searchInteractors.getSearchHistory(query, limit = 10).firstOrNull()
                        ?: emptyList()
                if (isActive) {
                    appRunner()?.runOnRenderThread {
                        val s = state.screen as? ScreenState.Search ?: return@runOnRenderThread
                        if (s.query.isBlank()) {
                            val visualLocal = rawSuggestions.map { "${MeloTheme.ICON_HISTORY} $it" }
                            updateScreen<ScreenState.Search> {
                                it.copy(searchSuggestions = visualLocal)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        return
    }

    updateScreen<ScreenState.Search> {
        it.copy(query = query, isShowingSuggestions = true, selectedSuggestionIndex = null)
    }

    suggestionsJob = scope.launch {
        try {
            val localHistory = searchInteractors.getSearchHistory(query, limit = 5)
                .firstOrNull() ?: emptyList()

            if (isActive) {
                appRunner()?.runOnRenderThread {
                    val s = state.screen as? ScreenState.Search ?: return@runOnRenderThread
                    if (s.query == query) {
                        val visualLocal = localHistory.map { "${MeloTheme.ICON_HISTORY} $it" }
                        updateScreen<ScreenState.Search> {
                            it.copy(searchSuggestions = visualLocal.ifEmpty {
                                listOf("Loading network suggestions...")
                            })
                        }
                    }
                }
            }

            val networkSuggestions = try {
                searchInteractors.getSearchSuggestions(query)
            } catch (_: Exception) {
                emptyList()
            }

            if (isActive) {
                appRunner()?.runOnRenderThread {
                    val s = state.screen as? ScreenState.Search ?: return@runOnRenderThread
                    if (s.query == query) {
                        val visualLocal = localHistory.map { "${MeloTheme.ICON_HISTORY} $it" }
                        val filteredNetwork = networkSuggestions
                            .filter { net ->
                                localHistory.none { loc ->
                                    net.equals(loc, ignoreCase = true)
                                }
                            }
                            .take(10 - visualLocal.size)
                            .map { "${MeloTheme.ICON_SEARCH} $it" }

                        val finalSuggestions = (visualLocal + filteredNetwork)
                            .ifEmpty { listOf("No recent queries for '$query'") }

                        updateScreen<ScreenState.Search> {
                            it.copy(searchSuggestions = finalSuggestions)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}

internal fun MeloScreen.handleSearchBarKey(event: KeyEvent): EventResult {
    val searchState = state.screen as? ScreenState.Search
    if (searchState != null && searchState.isShowingSuggestions && searchState.searchSuggestions.isNotEmpty()) {
        if (event.code() == KeyCode.ESCAPE) {
            updateScreen<ScreenState.Search> { it.copy(isShowingSuggestions = false) }
            return EventResult.HANDLED
        }
        if (event.code() == KeyCode.DOWN) {
            val nextIndex = if (searchState.selectedSuggestionIndex == null) 0 else minOf(
                searchState.searchSuggestions.size - 1,
                searchState.selectedSuggestionIndex + 1
            )
            updateScreen<ScreenState.Search> { it.copy(selectedSuggestionIndex = nextIndex) }
            return EventResult.HANDLED
        }
        if (event.code() == KeyCode.UP) {
            val prevIndex = if (searchState.selectedSuggestionIndex == null) -1 else maxOf(
                -1,
                searchState.selectedSuggestionIndex - 1
            )
            updateScreen<ScreenState.Search> { it.copy(selectedSuggestionIndex = if (prevIndex == -1) null else prevIndex) }
            return EventResult.HANDLED
        }
        if (event.matchesAction(MeloAction.DELETE, settingsViewState.currentSettings)) {
            if (searchState.selectedSuggestionIndex != null) {
                val suggestionToDeleteRaw =
                    searchState.searchSuggestions[searchState.selectedSuggestionIndex]
                val isLocalHistory = suggestionToDeleteRaw.startsWith("${MeloTheme.ICON_HISTORY} ")
                if (isLocalHistory) {
                    val suggestionToDelete =
                        suggestionToDeleteRaw.removePrefix("${MeloTheme.ICON_HISTORY} ")
                    scope.launch {
                        searchInteractors.deleteSearchQuery(suggestionToDelete)
                        val newSuggestions =
                            searchState.searchSuggestions.filterIndexed { index, _ ->
                                index != searchState.selectedSuggestionIndex
                            }
                        val newIndex = if (newSuggestions.isEmpty()) null else minOf(
                            searchState.selectedSuggestionIndex,
                            newSuggestions.size - 1
                        )
                        appRunner()?.runOnRenderThread {
                            updateScreen<ScreenState.Search> {
                                it.copy(
                                    searchSuggestions = newSuggestions,
                                    selectedSuggestionIndex = newIndex,
                                    isShowingSuggestions = newSuggestions.isNotEmpty()
                                )
                            }
                        }
                    }
                    return EventResult.HANDLED
                }
            }
        }
    }

    if (event.modifiers().alt()) {
        if (event.code() == KeyCode.RIGHT) {
            switchSearchTab(true)
            return EventResult.HANDLED
        }
        if (event.code() == KeyCode.LEFT) {
            switchSearchTab(false)
            return EventResult.HANDLED
        }
    }

    if (event.code() == KeyCode.TAB) {
        focusResults()
        return EventResult.HANDLED
    }

    if (event.code() == KeyCode.CHAR && !event.modifiers().ctrl() && !event.modifiers().alt()) {
        val str = event.string()
        if (str.isNotEmpty() && str[0] >= '\u007F') {
            searchInputState.insert(str)
            return EventResult.HANDLED
        }
    }

    val s = state.screen as? ScreenState.Search ?: return handleGlobalShortcuts(event)
    if (s.results.isNotEmpty() &&
        (event.matches(Actions.MOVE_DOWN) || event.matches(Actions.MOVE_UP))
    ) {
        return handleResultsKey(event)
    }
    return handleGlobalShortcuts(event)
}