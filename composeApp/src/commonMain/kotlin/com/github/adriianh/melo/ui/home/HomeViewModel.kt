package com.github.adriianh.melo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.DownloadStatus
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
import kotlinx.coroutines.Job
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var previousBaseFeed: HomeUiState? = null
    private var searchJob: Job? = null

    init {
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
        loadFeed()
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
        val local = try { scanLocalTracksUseCase() } catch (_: Exception) { emptyList() }
        val allLocal = (downloaded + local).distinctBy { it.id }.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
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

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, selectedChip = null) }
            val settings = getSettingsUseCase.getSnapshot()
            if (settings.offlineMode) {
                loadOfflineFeed()
                return@launch
            }

            try {
                val homeFeed = getHomeUseCase()
                val exploreSections =
                    if (homeFeed.sections.size < 4) getExploreUseCase() else emptyList()
                val chartsSections =
                    if (homeFeed.sections.size < 6) getChartsUseCase() else emptyList()
                val trending = if (homeFeed.sections.size < 4) getTrendingUseCase() else emptyList()

                val combinedSections = buildList {
                    addAll(homeFeed.sections)
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

                if (combinedSections.isEmpty()) {
                    loadOfflineFeed()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            chips = homeFeed.chips,
                            sections = combinedSections,
                            continuation = homeFeed.continuation,
                            isOfflineFeed = false
                        )
                    }
                }
            } catch (_: Exception) {
                loadOfflineFeed()
            }
        }
    }

    private suspend fun loadOfflineFeed() {
        val downloadedTracks = getOfflineTracksUseCase().firstOrNull()
            ?.filter { it.downloadStatus == DownloadStatus.COMPLETED }
            ?.map { it.track } ?: emptyList()

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

        viewModelScope.launch {
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

        viewModelScope.launch {
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