package com.github.adriianh.melo.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.BrowseCategoryResult
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.MoodAndGenreGroup
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.library.GetRemoteHistoryUseCase
import com.github.adriianh.core.domain.usecase.offline.GetOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.ScanLocalTracksUseCase
import com.github.adriianh.core.domain.usecase.playback.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.search.BrowseCategoryUseCase
import com.github.adriianh.core.domain.usecase.search.GetChartsUseCase
import com.github.adriianh.core.domain.usecase.search.GetExploreUseCase
import com.github.adriianh.core.domain.usecase.search.GetMoodAndGenresUseCase
import com.github.adriianh.core.domain.usecase.search.GetSearchHistoryUseCase
import com.github.adriianh.core.domain.usecase.search.GetSearchSuggestionsUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrendingUseCase
import com.github.adriianh.core.domain.usecase.search.SaveSearchQueryUseCase
import com.github.adriianh.core.domain.usecase.search.SearchAlbumsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchArtistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchSummaryUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.adriianh.core.domain.usecase.search.SearchVideosUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

enum class SearchFilterType(val label: String) {
    ALL("Todos"),
    SONGS("Canciones"),
    ALBUMS("Álbumes"),
    ARTISTS("Artistas"),
    PLAYLISTS("Playlists"),
    VIDEOS("Videos")
}

data class SearchUiState(
    val query: String = "",
    val selectedFilter: SearchFilterType = SearchFilterType.ALL,
    val summarySections: List<HomeSection> = emptyList(),
    val songResults: List<Track> = emptyList(),
    val albumResults: List<SearchResult.Album> = emptyList(),
    val artistResults: List<SearchResult.Artist> = emptyList(),
    val playlistResults: List<SearchResult.Playlist> = emptyList(),
    val videoResults: List<Track> = emptyList(),
    val isSearching: Boolean = false,
    val exploreSections: List<HomeSection> = emptyList(),
    val isLoadingExplore: Boolean = true,
    val moodAndGenres: List<MoodAndGenreGroup> = emptyList(),
    val isLoadingMoodAndGenres: Boolean = true,
    val recentSearches: List<String> = emptyList(),
    val recentHistory: List<Track> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val browseCategoryResult: BrowseCategoryResult? = null,
    val isBrowsingCategory: Boolean = false,
    val error: String? = null,
    val isOfflineSearch: Boolean = false,
) {
    val hasResults: Boolean
        get() = when (selectedFilter) {
            SearchFilterType.ALL -> summarySections.isNotEmpty()
            SearchFilterType.SONGS -> songResults.isNotEmpty()
            SearchFilterType.ALBUMS -> albumResults.isNotEmpty()
            SearchFilterType.ARTISTS -> artistResults.isNotEmpty()
            SearchFilterType.PLAYLISTS -> playlistResults.isNotEmpty()
            SearchFilterType.VIDEOS -> videoResults.isNotEmpty()
        }
}

class SearchViewModel(
    private val searchTracksUseCase: SearchTracksUseCase,
    private val searchAlbumsUseCase: SearchAlbumsUseCase,
    private val searchArtistsUseCase: SearchArtistsUseCase,
    private val searchPlaylistsUseCase: SearchPlaylistsUseCase,
    private val searchVideosUseCase: SearchVideosUseCase,
    private val searchSummaryUseCase: SearchSummaryUseCase,
    private val getExploreUseCase: GetExploreUseCase,
    private val getChartsUseCase: GetChartsUseCase,
    private val getTrendingUseCase: GetTrendingUseCase,
    private val getMoodAndGenresUseCase: GetMoodAndGenresUseCase,
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val saveSearchQueryUseCase: SaveSearchQueryUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    private val getRecentTracksUseCase: GetRecentTracksUseCase,
    private val getRemoteHistoryUseCase: GetRemoteHistoryUseCase,
    private val browseCategoryUseCase: BrowseCategoryUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val getOfflineTracksUseCase: GetOfflineTracksUseCase,
    private val scanLocalTracksUseCase: ScanLocalTracksUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job = Job()
    private var suggestionsJob: Job = Job()

    private fun resetResults() {
        _uiState.update {
            it.copy(
                summarySections = emptyList(),
                songResults = emptyList(),
                albumResults = emptyList(),
                artistResults = emptyList(),
                playlistResults = emptyList(),
                videoResults = emptyList(),
                suggestions = emptyList(),
                isSearching = false,
                isOfflineSearch = false,
            )
        }
    }

    init {
        loadExplore()
        loadMoodAndGenres()
        loadRecentSearches()
        loadRecentHistory()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob.cancel()
        suggestionsJob.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(query = "") }
            resetResults()
            loadRecentSearches()
            return
        }

        suggestionsJob = viewModelScope.launch {
            val isOffline = getSettingsUseCase.getSnapshot().offlineMode
            if (isOffline) return@launch
            delay(200.milliseconds)
            try {
                val suggestions = getSearchSuggestionsUseCase(query)
                _uiState.update { it.copy(suggestions = suggestions) }
            } catch (_: Exception) {
                _uiState.update { it.copy(suggestions = emptyList()) }
            }
        }

        searchJob = viewModelScope.launch {
            delay(400.milliseconds)
            performSearch(query, _uiState.value.selectedFilter)
        }
    }

    fun onFilterSelected(filter: SearchFilterType) {
        if (_uiState.value.selectedFilter == filter) return
        _uiState.update { it.copy(selectedFilter = filter) }
        val query = _uiState.value.query
        if (query.isNotBlank()) {
            executeSearch(query)
        }
    }

    fun executeSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        searchJob.cancel()
        suggestionsJob.cancel()
        _uiState.update { it.copy(query = trimmed, suggestions = emptyList()) }

        searchJob = viewModelScope.launch {
            performSearch(trimmed, _uiState.value.selectedFilter)
        }
    }

    private suspend fun performSearch(query: String, filter: SearchFilterType) {
        _uiState.update { it.copy(isSearching = true) }
        val isOffline = getSettingsUseCase.getSnapshot().offlineMode
        if (isOffline) {
            performOfflineSearch(query)
            return
        }

        try {
            when (filter) {
                SearchFilterType.ALL -> {
                    val summary = searchSummaryUseCase(query)
                    if (summary.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                summarySections = summary,
                                isSearching = false,
                                isOfflineSearch = false
                            )
                        }
                    } else {
                        val tracks = searchTracksUseCase(query)
                        val fallbackSections = if (tracks.isNotEmpty()) {
                            listOf(
                                HomeSection(
                                    "Canciones",
                                    com.github.adriianh.core.domain.model.HomeSectionType.SONGS,
                                    tracks.map { SearchResult.Song(it) })
                            )
                        } else emptyList()
                        _uiState.update {
                            it.copy(
                                summarySections = fallbackSections,
                                isSearching = false,
                                isOfflineSearch = false
                            )
                        }
                    }
                }

                SearchFilterType.SONGS -> {
                    val results = searchTracksUseCase(query)
                    _uiState.update {
                        it.copy(
                            songResults = results,
                            isSearching = false,
                            isOfflineSearch = false
                        )
                    }
                }

                SearchFilterType.ALBUMS -> {
                    val results = searchAlbumsUseCase(query)
                    _uiState.update {
                        it.copy(
                            albumResults = results,
                            isSearching = false,
                            isOfflineSearch = false
                        )
                    }
                }

                SearchFilterType.ARTISTS -> {
                    val results = searchArtistsUseCase(query)
                    _uiState.update {
                        it.copy(
                            artistResults = results,
                            isSearching = false,
                            isOfflineSearch = false
                        )
                    }
                }

                SearchFilterType.PLAYLISTS -> {
                    val results = searchPlaylistsUseCase(query)
                    _uiState.update {
                        it.copy(
                            playlistResults = results,
                            isSearching = false,
                            isOfflineSearch = false
                        )
                    }
                }

                SearchFilterType.VIDEOS -> {
                    val results = searchVideosUseCase(query)
                    _uiState.update {
                        it.copy(
                            videoResults = results,
                            isSearching = false,
                            isOfflineSearch = false
                        )
                    }
                }
            }
            saveSearchQueryUseCase(query)
            loadRecentSearches()
        } catch (_: Exception) {
            performOfflineSearch(query)
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
        _uiState.update {
            it.copy(
                songResults = allLocal,
                summarySections = if (allLocal.isNotEmpty()) listOf(
                    HomeSection(
                        "Resultados locales y descargados",
                        com.github.adriianh.core.domain.model.HomeSectionType.SONGS,
                        allLocal.map { SearchResult.Song(it) }
                    )
                ) else emptyList(),
                isSearching = false,
                isOfflineSearch = true
            )
        }
    }

    fun onSuggestionSelected(query: String) {
        _uiState.update { it.copy(query = query, suggestions = emptyList()) }
        executeSearch(query)
    }

    fun clearQuery() {
        searchJob.cancel()
        suggestionsJob.cancel()
        _uiState.update { it.copy(query = "") }
        resetResults()
        _uiState.update {
            it.copy(
                browseCategoryResult = null,
                isBrowsingCategory = false
            )
        }
        loadRecentSearches()
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            getSearchHistoryUseCase("", 10).collect { queries ->
                _uiState.update { it.copy(recentSearches = queries) }
            }
        }
    }

    private fun loadRecentHistory() {
        viewModelScope.launch {
            try {
                val localTracks = getRecentTracksUseCase(10).first()
                    .map { it.track }

                if (localTracks.isNotEmpty()) {
                    _uiState.update { it.copy(recentHistory = localTracks) }
                } else {
                    val remoteTracks = getRemoteHistoryUseCase().getOrNull().orEmpty()
                        .map { it.track }
                    _uiState.update { it.copy(recentHistory = remoteTracks) }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun loadMoodAndGenres() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoodAndGenres = true) }
            try {
                val groups = getMoodAndGenresUseCase()
                _uiState.update { it.copy(moodAndGenres = groups, isLoadingMoodAndGenres = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMoodAndGenres = false) }
            }
        }
    }

    private fun loadExplore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingExplore = true) }
            try {
                val explore = getExploreUseCase()
                val charts = getChartsUseCase()
                val trending = getTrendingUseCase()
                val combined = buildList {
                    addAll(explore)
                    addAll(charts)
                    if (trending.isNotEmpty()) {
                        add(
                            HomeSection(
                                title = "Trending",
                                type = com.github.adriianh.core.domain.model.HomeSectionType.SONGS,
                                items = trending.map { SearchResult.Song(it) }
                            )
                        )
                    }
                }.distinctBy { it.title }
                _uiState.update { it.copy(exploreSections = combined, isLoadingExplore = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingExplore = false,
                        error = e.message ?: "Error al cargar"
                    )
                }
            }
        }
    }

    fun browseCategory(browseId: String, params: String?) {
        searchJob.cancel()
        suggestionsJob.cancel()
        resetResults()
        _uiState.update {
            it.copy(
                query = "",
                isBrowsingCategory = true,
                browseCategoryResult = null
            )
        }
        searchJob = viewModelScope.launch {
            try {
                val result = browseCategoryUseCase(browseId, params)
                _uiState.update {
                    it.copy(
                        browseCategoryResult = result,
                        isBrowsingCategory = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isBrowsingCategory = false) }
            }
        }
    }

    fun exitBrowsing() {
        searchJob.cancel()
        _uiState.update {
            it.copy(
                browseCategoryResult = null,
                isBrowsingCategory = false
            )
        }
        loadRecentSearches()
        loadRecentHistory()
    }
}