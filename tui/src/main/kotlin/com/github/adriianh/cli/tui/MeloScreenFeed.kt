package com.github.adriianh.cli.tui

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Home feed loading and track-metadata enrichment, owned by [MeloScreen].
 */

internal fun MeloScreen.loadHomeFeed(chipParams: String? = null, chipIndex: Int = 0) {
    val currentHome = (state.screen as? ScreenState.Home) ?: cachedHomeScreen
    if (currentHome.isLoadingFeed && chipParams == null && chipIndex == currentHome.selectedChipIndex) return
    updateScreen<ScreenState.Home> { it.copy(isLoadingFeed = true, feedError = null) }
    scope.launch {
        try {
            val feed = getHome(params = chipParams)
            val allSections = feed.sections.toMutableList()
            val chips = feed.chips
            var nextContinuation = feed.continuation

            var continuationCount = 0
            while (!nextContinuation.isNullOrBlank() && allSections.size < 12 && continuationCount < 3) {
                try {
                    val nextFeed = getHome(continuation = nextContinuation)
                    if (nextFeed.sections.isEmpty()) break
                    allSections.addAll(nextFeed.sections)
                    nextContinuation = nextFeed.continuation
                    continuationCount++
                } catch (_: Exception) {
                    break
                }
            }

            if (chipParams == null && allSections.size < 5) {
                try {
                    val explore = discoveryInteractors.getExplore()
                    allSections.addAll(explore)
                } catch (_: Exception) {
                }
                try {
                    val charts = discoveryInteractors.getCharts()
                    allSections.addAll(charts)
                } catch (_: Exception) {
                }
            }

            val distinctSections = allSections.distinctBy { it.title }

            appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Home> { current ->
                    val preservedChips = chips.ifEmpty { current.feedChips }
                    current.copy(
                        feedSections = distinctSections,
                        feedChips = preservedChips,
                        feedContinuation = nextContinuation,
                        isLoadingMoreSections = false,
                        selectedChipIndex = chipIndex,
                        isLoadingFeed = false,
                        feedError = null,
                        selectedSectionIndex = 0,
                        selectedItemIndex = 0
                    )
                }
                enrichActiveSectionTracks()
            }
        } catch (e: Exception) {
            appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Home> {
                    it.copy(
                        isLoadingFeed = false,
                        feedError = e.message ?: "Failed to load feed"
                    )
                }
            }
        }
    }
}

internal fun MeloScreen.loadMoreHomeSections() {
    val s = state.screen as? ScreenState.Home ?: return
    val continuation = s.feedContinuation ?: return
    if (s.isLoadingMoreSections || s.isLoadingFeed) return

    updateScreen<ScreenState.Home> { it.copy(isLoadingMoreSections = true) }
    scope.launch {
        try {
            val nextFeed = getHome(continuation = continuation)
            appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Home> { current ->
                    val newSections =
                        (current.feedSections + nextFeed.sections).distinctBy { it.title }
                    current.copy(
                        feedSections = newSections,
                        feedContinuation = nextFeed.continuation,
                        isLoadingMoreSections = false
                    )
                }
            }
        } catch (_: Exception) {
            appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Home> { it.copy(isLoadingMoreSections = false) }
            }
        }
    }
}

internal fun MeloScreen.enrichActiveSectionTracks() {
    val s = state.screen as? ScreenState.Home ?: return
    val sectionIndex = s.selectedSectionIndex
    val currentSection = s.feedSections.getOrNull(sectionIndex) ?: return

    val songsToEnrich = currentSection.items.mapIndexedNotNull { index, item ->
        if (item is SearchResult.Song) {
            val track = item.track
            if (track.album.isBlank() || track.durationMs <= 0L) {
                index to track
            } else null
        } else null
    }

    if (songsToEnrich.isEmpty()) return

    enrichSectionJob?.cancel()
    enrichSectionJob = scope.launch(Dispatchers.IO) {
        val semaphore = Semaphore(6)
        val immediateUpdates = mutableListOf<Pair<Int, Track>>()
        val pendingNetwork = mutableListOf<Pair<Int, Track>>()

        for ((itemIndex, track) in songsToEnrich) {
            val cached = trackMetadataCache[track.id]
            if (cached != null && (cached.first.isNotBlank() || cached.second > 0L)) {
                val updated = track.copy(
                    album = track.album.ifBlank { cached.first },
                    durationMs = if (track.durationMs > 0L) track.durationMs else cached.second
                )
                immediateUpdates.add(itemIndex to updated)
                continue
            }

            val fav = state.collections.favorites.find { it.id == track.id }
            if (fav != null && (fav.album.isNotBlank() || fav.durationMs > 0L)) {
                val updated = track.copy(
                    album = track.album.ifBlank { fav.album },
                    durationMs = if (track.durationMs > 0L) track.durationMs else fav.durationMs
                )
                trackMetadataCache[track.id] = updated.album to updated.durationMs
                immediateUpdates.add(itemIndex to updated)
                continue
            }

            pendingNetwork.add(itemIndex to track)
        }

        if (immediateUpdates.isNotEmpty()) {
            appRunner()?.runOnRenderThread {
                applyEnrichedTracksBatch(sectionIndex, immediateUpdates)
            }
        }

        if (pendingNetwork.isEmpty()) return@launch

        val networkBatch = java.util.concurrent.ConcurrentLinkedQueue<Pair<Int, Track>>()
        coroutineScope {
            pendingNetwork.forEach { (itemIndex, track) ->
                launch {
                    semaphore.withPermit {
                        if (!isActive) return@withPermit
                        try {
                            val fetched = getTrack(track.id)
                            if (fetched != null && (fetched.album.isNotBlank() || fetched.durationMs > 0L)) {
                                val updated = track.copy(
                                    album = track.album.ifBlank { fetched.album },
                                    durationMs = if (track.durationMs > 0L) track.durationMs else fetched.durationMs
                                )
                                trackMetadataCache[track.id] =
                                    updated.album to updated.durationMs
                                networkBatch.add(itemIndex to updated)
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }

        if (networkBatch.isNotEmpty() && isActive) {
            appRunner()?.runOnRenderThread {
                applyEnrichedTracksBatch(sectionIndex, networkBatch.toList())
            }
            saveDiskMetadataCache()
        }
    }
}

private fun MeloScreen.applyEnrichedTracksBatch(sectionIndex: Int, updates: List<Pair<Int, Track>>) {
    if (updates.isEmpty()) return
    updateScreen<ScreenState.Home> { current ->
        if (current.selectedSectionIndex != sectionIndex) return@updateScreen current
        val sec = current.feedSections.getOrNull(sectionIndex) ?: return@updateScreen current
        val newItems = sec.items.toMutableList()
        for ((idx, track) in updates) {
            if (idx in newItems.indices && newItems[idx] is SearchResult.Song) {
                newItems[idx] = SearchResult.Song(track)
            }
        }
        val newSec = sec.copy(items = newItems)
        val newSections = current.feedSections.toMutableList()
        newSections[sectionIndex] = newSec
        current.copy(feedSections = newSections)
    }
}