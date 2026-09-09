package com.github.adriianh.melo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.cache.HomeFeedCache
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.HomeFeed
import com.github.adriianh.core.domain.model.HomeFeedChip
import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.offline.GetOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.ScanLocalTracksUseCase
import com.github.adriianh.core.domain.usecase.playback.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.search.GetChartsUseCase
import com.github.adriianh.core.domain.usecase.search.GetExploreUseCase
import com.github.adriianh.core.domain.usecase.search.GetHomeUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrendingUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.util.MeloDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class HomeUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val chips: List<HomeFeedChip> = emptyList(),
    val selectedChip: HomeFeedChip? = null,
    val sections: List<HomeSection> = emptyList(),
    val continuation: String? = null,
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Track> = emptyList(),
    val isSearching: Boolean = false,
    val isOfflineFeed: Boolean = false,
)

class HomeViewModel(
    private val getHomeUseCase: GetHomeUseCase,
    private val getExploreUseCase: GetExploreUseCase,
    private val getChartsUseCase: GetChartsUseCase,
    private val getTrendingUseCase: GetTrendingUseCase,
    private val searchTracksUseCase: SearchTracksUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val getOfflineTracksUseCase: GetOfflineTracksUseCase,
    private val scanLocalTracksUseCase: ScanLocalTracksUseCase,
    private val getRecentTracksUseCase: GetRecentTracksUseCase,
    private val homeFeedCache: HomeFeedCache,
    private val ioDispatcher: CoroutineDispatcher = MeloDispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var previousBaseFeed: HomeUiState? = null
    private var searchJob: Job? = null
    private var loadFeedJob: Job? = null

    init {
        val cached = homeFeedCache.getSync()
        if (cached != null && cached.sections.isNotEmpty()) {
            _uiState.value = HomeUiState(
                isLoading = false,
                chips = cached.chips,
                sections = cached.sections,
                continuation = cached.continuation,
                isOfflineFeed = false
            )
            loadFeed(silent = true, delayMs = 1800L)
        } else {
            viewModelScope.launch(ioDispatcher) {
                val cachedAsync = homeFeedCache.get()
                if (cachedAsync != null && cachedAsync.sections.isNotEmpty() && _uiState.value.sections.isEmpty()) {
                    _uiState.update { current ->
                        if (current.sections.isEmpty()) {
                            current.copy(
                                isLoading = false,
                                chips = cachedAsync.chips,
                                sections = cachedAsync.sections,
                                continuation = cachedAsync.continuation,
                                isOfflineFeed = false
                            )
                        } else current
                    }
                }
                loadFeed(
                    silent = cachedAsync != null && cachedAsync.sections.isNotEmpty(),
                    delayMs = if (cachedAsync != null && cachedAsync.sections.isNotEmpty()) 1800L else 0L
                )
            }
        }

        viewModelScope.launch {
            getSettingsUseCase()
                .map { it.offlineMode }
                .distinctUntilChanged()
                .collectLatest { offlineMode ->
                    if (offlineMode) {
                        loadOfflineFeed()
                    } else if (_uiState.value.isOfflineFeed) {
                        loadFeed()
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300.milliseconds)
            _uiState.update { it.copy(isSearching = true) }

            if (_uiState.value.isOfflineFeed || getSettingsUseCase.getSnapshot().offlineMode) {
                performOfflineSearch(query)
            } else {
                try {
                    val results = searchTracksUseCase(query)
                    _uiState.update { it.copy(searchResults = results, isSearching = false) }
                } catch (_: Exception) {
                    performOfflineSearch(query)
                }
            }
        }
    }

    private suspend fun performOfflineSearch(query: String) {
        val downloaded = getOfflineTracksUseCase().firstOrNull()
            ?.filter { it.downloadStatus == DownloadStatus.COMPLETED }
            ?.map { it.track } ?: emptyList()
        val local = try {
            scanLocalTracksUseCase()
        } catch (_: Exception) {
            emptyList()
        }
        val allLocal = (downloaded + local).distinctBy { it.id }.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(
                query,
                ignoreCase = true
            )
        }
        _uiState.update { it.copy(searchResults = allLocal, isSearching = false) }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false
            )
        }
    }

    /**
     * Emits sections to the UI state progressively in small batches, with a short
     * delay between each batch. This prevents Compose from receiving all sections in
     * a single recomposition (which causes a multi-frame lag on the compositor thread),
     * producing a smooth incremental render instead.
     *
     * A 16ms delay between batches corresponds to one compositor frame at 60fps,
     * giving Compose enough time to measure, layout, and draw each batch before the
     * next one arrives.
     */
    private suspend fun emitSectionsProgressively(
        sections: List<HomeSection>,
        chips: List<HomeFeedChip>,
        continuation: String?,
        batchSize: Int = 2,
        frameDelayMs: Long = 16L,
    ) {
        if (sections.isEmpty()) return
        val batches = sections.chunked(batchSize)
        batches.forEachIndexed { index, batch ->
            _uiState.update { current ->
                if (index == 0) {
                    current.copy(
                        isLoading = false,
                        chips = chips,
                        sections = batch,
                        continuation = continuation,
                        isOfflineFeed = false
                    )
                } else {
                    current.copy(sections = current.sections + batch)
                }
            }
            if (index < batches.lastIndex) {
                delay(frameDelayMs.milliseconds)
            }
        }
    }

    /**
     * Merges refreshed sections into the currently displayed ones so that
     * unchanged sections keep their existing composition (and scroll state),
     * avoiding a full re-render of the feed when only a few sections changed.
     */
    private fun mergeSections(
        current: List<HomeSection>,
        incoming: List<HomeSection>,
    ): List<HomeSection> {
        if (incoming.isEmpty()) return current
        if (current.isEmpty()) return incoming
        val incomingByKey = incoming.associateBy { sectionKey(it) }
        val merged = current.mapNotNull { old ->
            incomingByKey[sectionKey(old)]?.let { next -> if (next == old) old else next }
        }
        val seen = merged.map { sectionKey(it) }.toMutableSet()
        val result = merged.toMutableList()
        for (next in incoming) {
            if (seen.add(sectionKey(next))) {
                result.add(next)
            }
        }
        return result
    }

    private fun sectionKey(section: HomeSection): String =
        if (section.title.isNotBlank()) "${section.type}_${section.title}"
        else "blank_${System.identityHashCode(section)}"

    fun loadFeed(silent: Boolean = false, delayMs: Long = 0L) {
        if (silent && loadFeedJob?.isActive == true) return
        loadFeedJob?.cancel()
        loadFeedJob = viewModelScope.launch(ioDispatcher) {
            if (delayMs > 0L) {
                delay(delayMs.milliseconds)
            }
            if (!silent) {
                _uiState.update { it.copy(isLoading = true, error = null, selectedChip = null) }
            } else {
                _uiState.update { it.copy(error = null, selectedChip = null) }
            }
            val settings = getSettingsUseCase.getSnapshot()
            if (settings.offlineMode) {
                loadOfflineFeed()
                return@launch
            }

            try {
                val homeFeed = getHomeUseCase()
                val needsMore = homeFeed.sections.isEmpty()
                val (exploreSections, chartsSections, trending) = if (needsMore) {
                    coroutineScope {
                        val exp =
                            async { runCatching { getExploreUseCase() }.getOrDefault(emptyList()) }
                        val chr =
                            async { runCatching { getChartsUseCase() }.getOrDefault(emptyList()) }
                        val trn =
                            async { runCatching { getTrendingUseCase() }.getOrDefault(emptyList()) }
                        Triple(exp.await(), chr.await(), trn.await())
                    }
                } else {
                    Triple(emptyList(), emptyList(), emptyList())
                }

                val combinedSections = homeFeed.sections.ifEmpty {
                    buildList {
                        addAll(exploreSections)
                        addAll(chartsSections)
                        if (trending.isNotEmpty()) {
                            add(
                                HomeSection(
                                    title = "Trending",
                                    type = HomeSectionType.SONGS,
                                    items = trending.map { SearchResult.Song(it) }
                                )
                            )
                        }
                    }.distinctBy { it.title }
                }

                if (combinedSections.isEmpty()) {
                    if (_uiState.value.sections.isEmpty()) {
                        loadOfflineFeed()
                    }
                } else {
                    if (_uiState.value.sections != combinedSections || _uiState.value.chips != homeFeed.chips) {
                        val fullFeed = HomeFeed(
                            chips = homeFeed.chips,
                            sections = combinedSections,
                            continuation = homeFeed.continuation
                        )
                        homeFeedCache.save(fullFeed)

                        if (_uiState.value.isLoading) {
                            emitSectionsProgressively(
                                sections = combinedSections,
                                chips = homeFeed.chips,
                                continuation = homeFeed.continuation
                            )
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    chips = homeFeed.chips,
                                    sections = mergeSections(it.sections, combinedSections),
                                    continuation = homeFeed.continuation,
                                    isOfflineFeed = false
                                )
                            }
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (_: Exception) {
                if (_uiState.value.sections.isEmpty()) {
                    loadOfflineFeed()
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private suspend fun loadOfflineFeed() {
        val allCompleted = getOfflineTracksUseCase().firstOrNull()
            ?.filter { it.downloadStatus == DownloadStatus.COMPLETED } ?: emptyList()
        val downloadedTracks = allCompleted
            .filter { it.downloadType == DownloadType.MANUAL }
            .map { it.track }
            .ifEmpty { allCompleted.map { it.track } }

        val localTracks = try {
            scanLocalTracksUseCase()
        } catch (_: Exception) {
            emptyList()
        }

        val recentTracks = try {
            getRecentTracksUseCase(limit = 20).firstOrNull()?.map { it.track } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val offlineSections = buildList {
            if (recentTracks.isNotEmpty()) {
                add(
                    HomeSection(
                        title = "Escuchado recientemente",
                        type = HomeSectionType.SONGS,
                        items = recentTracks.map { SearchResult.Song(it) }
                    )
                )
            }
            if (downloadedTracks.isNotEmpty()) {
                add(
                    HomeSection(
                        title = "Tus descargas",
                        type = HomeSectionType.SONGS,
                        items = downloadedTracks.map { SearchResult.Song(it) }
                    )
                )
            }
            if (localTracks.isNotEmpty()) {
                add(
                    HomeSection(
                        title = "Archivos locales",
                        type = HomeSectionType.SONGS,
                        items = localTracks.map { SearchResult.Song(it) }
                    )
                )
            }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                error = null,
                chips = emptyList(),
                sections = offlineSections,
                continuation = null,
                isOfflineFeed = true
            )
        }
    }

    fun toggleChip(chip: HomeFeedChip) {
        if (_uiState.value.selectedChip == chip) {
            if (previousBaseFeed != null) {
                _uiState.value = previousBaseFeed!!
                previousBaseFeed = null
            } else {
                loadFeed()
            }
            return
        }

        if (previousBaseFeed == null && _uiState.value.selectedChip == null) {
            previousBaseFeed = _uiState.value
        }

        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true, selectedChip = chip, error = null) }
            try {
                val filteredFeed = getHomeUseCase(params = chip.params)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sections = filteredFeed.sections,
                        continuation = filteredFeed.continuation
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error al filtrar")
                }
            }
        }
    }

    fun loadMore() {
        val currentContinuation = _uiState.value.continuation ?: return
        if (_uiState.value.isLoadingMore || _uiState.value.isLoading) return

        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val nextFeed = getHomeUseCase(continuation = currentContinuation)
                val newSections =
                    (_uiState.value.sections + nextFeed.sections).distinctBy { it.title }
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        sections = newSections,
                        continuation = nextFeed.continuation
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
}