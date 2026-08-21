package com.github.adriianh.melo.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.MoodAndGenreGroup
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.search.GetChartsUseCase
import com.github.adriianh.core.domain.usecase.search.GetExploreUseCase
import com.github.adriianh.core.domain.usecase.search.GetMoodAndGenresUseCase
import com.github.adriianh.core.domain.usecase.search.GetSearchHistoryUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrendingUseCase
import com.github.adriianh.core.domain.usecase.search.SaveSearchQueryUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class SearchUiState(
    val query: String = "",
    val results: List<Track> = emptyList(),
    val isSearching: Boolean = false,
    val exploreSections: List<HomeSection> = emptyList(),
    val isLoadingExplore: Boolean = true,
    val moodAndGenres: List<MoodAndGenreGroup> = emptyList(),
    val isLoadingMoodAndGenres: Boolean = true,
    val recentSearches: List<String> = emptyList(),
    val error: String? = null,
)

class SearchViewModel(
    private val searchTracksUseCase: SearchTracksUseCase,
    private val getExploreUseCase: GetExploreUseCase,
    private val getChartsUseCase: GetChartsUseCase,
    private val getTrendingUseCase: GetTrendingUseCase,
    private val getMoodAndGenresUseCase: GetMoodAndGenresUseCase,
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val saveSearchQueryUseCase: SaveSearchQueryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job = Job()

    init {
        loadExplore()
        loadMoodAndGenres()
        loadRecentSearches()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false, results = emptyList()) }
            loadRecentSearches()
            return
        }

        searchJob = viewModelScope.launch {
            delay(300.milliseconds)
            _uiState.update { it.copy(isSearching = true) }
            try {
                val results = searchTracksUseCase(query)
                _uiState.update { it.copy(results = results, isSearching = false) }
                saveSearchQueryUseCase(query)
            } catch (_: Exception) {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    fun clearQuery() {
        searchJob.cancel()
        _uiState.update { it.copy(query = "", results = emptyList(), isSearching = false) }
        loadRecentSearches()
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            getSearchHistoryUseCase("", 10).collect { queries ->
                _uiState.update { it.copy(recentSearches = queries) }
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
}
