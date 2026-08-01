package com.github.adriianh.melo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.search.GetChartsUseCase
import com.github.adriianh.core.domain.usecase.search.GetExploreUseCase
import com.github.adriianh.core.domain.usecase.search.GetHomeUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrendingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val sections: List<SearchResult.ArtistSection> = emptyList(),
    val error: String? = null
)

class HomeViewModel(
    private val getHomeUseCase: GetHomeUseCase,
    private val getExploreUseCase: GetExploreUseCase,
    private val getChartsUseCase: GetChartsUseCase,
    private val getTrendingUseCase: GetTrendingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val sections = buildList {
                    addAll(getHomeUseCase())
                    addAll(getExploreUseCase())
                    addAll(getChartsUseCase())
                    val trending = getTrendingUseCase()
                    if (trending.isNotEmpty()) {
                        add(
                            SearchResult.ArtistSection(
                                "Trending",
                                trending.map { SearchResult.Song(it) })
                        )
                    }
                }.distinctBy { it.title }
                _uiState.update { it.copy(sections = sections, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }
}