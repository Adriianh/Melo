package com.github.adriianh.melo.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.search.GetEntityDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntityDetailUiState(
    val isLoading: Boolean = true,
    val entity: SearchResult? = null,
    val error: String? = null,
)

class EntityDetailViewModel(
    private val getEntityDetailsUseCase: GetEntityDetailsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntityDetailUiState())
    val uiState: StateFlow<EntityDetailUiState> = _uiState.asStateFlow()

    fun loadAlbum(
        id: String,
        initialTitle: String = "",
        initialArtwork: String? = null,
        initialAuthor: String = ""
    ) {
        loadEntity(
            SearchResult.Album(
                id = id,
                title = initialTitle,
                author = initialAuthor,
                year = null,
                artworkUrl = initialArtwork
            )
        )
    }

    fun loadPlaylist(
        id: String,
        initialTitle: String = "",
        initialArtwork: String? = null,
        initialAuthor: String = ""
    ) {
        loadEntity(
            SearchResult.Playlist(
                id = id,
                title = initialTitle,
                author = initialAuthor,
                trackCount = null,
                artworkUrl = initialArtwork
            )
        )
    }

    fun loadArtist(id: String, initialName: String = "", initialArtwork: String? = null) {
        loadEntity(
            SearchResult.Artist(
                id = id,
                name = initialName,
                artworkUrl = initialArtwork
            )
        )
    }

    private fun loadEntity(initial: SearchResult) {
        _uiState.update { it.copy(isLoading = true, entity = initial, error = null) }
        viewModelScope.launch {
            try {
                val fullDetails = getEntityDetailsUseCase(initial)
                _uiState.update { it.copy(isLoading = false, entity = fullDetails) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar detalles"
                    )
                }
            }
        }
    }
}