package com.github.adriianh.melo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.HomeFeedChip
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.search.GetChartsUseCase
import com.github.adriianh.core.domain.usecase.search.GetExploreUseCase
import com.github.adriianh.core.domain.usecase.search.GetHomeUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrendingUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val chips: List<HomeFeedChip> = emptyList(),
    val selectedChip: HomeFeedChip? = null,
    val sections: List<SearchResult.ArtistSection> = emptyList(),
    val continuation: String? = null,
    val error: String? = null,
)

class HomeViewModel(
    private val getHomeUseCase: GetHomeUseCase,
    private val getExploreUseCase: GetExploreUseCase,
    private val getChartsUseCase: GetChartsUseCase,
    private val getTrendingUseCase: GetTrendingUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var previousBaseFeed: HomeUiState? = null

    init {
        viewModelScope.launch {
            getSettingsUseCase().collectLatest {
                loadFeed()
            }
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, selectedChip = null) }
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
                            SearchResult.ArtistSection(
                                "Trending",
                                trending.map { SearchResult.Song(it) })
                        )
                    }
                }.distinctBy { it.title }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        chips = homeFeed.chips,
                        sections = combinedSections,
                        continuation = homeFeed.continuation
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error al cargar el inicio")
                }
            }
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
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
}