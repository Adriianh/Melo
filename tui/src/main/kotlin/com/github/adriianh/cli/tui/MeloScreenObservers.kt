package com.github.adriianh.cli.tui

import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.tui.handler.loadStats
import com.github.adriianh.cli.tui.handler.search.handleSearchQueryChange
import com.github.adriianh.cli.tui.handler.syncYouTubeLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

/**
 * Background observation and disk-cache helpers owned by [MeloScreen].
 */

internal fun MeloScreen.observeSearchInput() {
    scope.launch {
        var lastObservedFocus = false
        var lastObservedFocusId: String? = null
        while (isActive) {
            val currentQuery = searchInputState.text()
            val focusedId = appRunner()?.focusManager()?.focusedId()
            val isFocused = focusedId == "search-bar"

            if (state.screen is ScreenState.Search) {
                if (currentQuery != lastObservedSearchQuery) {
                    lastObservedSearchQuery = currentQuery
                    handleSearchQueryChange(currentQuery)
                } else if (isFocused && !lastObservedFocus) {
                    handleSearchQueryChange(currentQuery)
                } else if (!isFocused && lastObservedFocus) {
                    updateScreen<ScreenState.Search> { it.copy(isShowingSuggestions = false) }
                }
            }

            if (focusedId != lastObservedFocusId) {
                when (focusedId) {
                    "home-panel", "home-feed-items" -> {
                        val home = (state.screen as? ScreenState.Home) ?: cachedHomeScreen
                        if (home.feedSections.isEmpty() && !home.isLoadingFeed && home.feedError == null) {
                            loadHomeFeed()
                        }
                    }

                    "stats-panel" -> {
                        val stats = (state.screen as? ScreenState.Stats) ?: cachedStatsScreen
                        if (stats.statsListening == null && !stats.statsLoading) {
                            loadStats()
                        }
                    }

                    "library-panel" -> {
                        val isLoggedIn =
                            !settingsViewState.currentSettings.sessionCookies.isNullOrBlank()
                        if (isLoggedIn && state.collections.remotePlaylists.isEmpty()) {
                            syncYouTubeLibrary()
                        }
                    }
                }
                lastObservedFocusId = focusedId
            }

            lastObservedFocus = isFocused
            delay(100.milliseconds)
        }
    }
}

internal fun MeloScreen.loadDiskMetadataCache() {
    try {
        val file = java.io.File(configDir, "metadata_cache.json")
        if (!file.exists()) return
        val text = file.readText()
        val parsed = Json.decodeFromString<Map<String, List<String>>>(text)
        for ((key, pair) in parsed) {
            val album = pair.getOrNull(0).orEmpty()
            val dur = pair.getOrNull(1)?.toLongOrNull() ?: 0L
            trackMetadataCache[key] = album to dur
        }
    } catch (_: Exception) {
    }
}

internal fun MeloScreen.saveDiskMetadataCache() {
    scope.launch(Dispatchers.IO) {
        try {
            val dir = java.io.File(configDir)
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, "metadata_cache.json")
            val map = trackMetadataCache.mapValues {
                listOf(
                    it.value.first,
                    it.value.second.toString()
                )
            }
            file.writeText(Json.encodeToString(map))
        } catch (_: Exception) {
        }
    }
}