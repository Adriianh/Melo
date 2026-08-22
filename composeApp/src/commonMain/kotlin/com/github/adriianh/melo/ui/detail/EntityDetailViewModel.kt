package com.github.adriianh.melo.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.library.SubscribeChannelUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeAlbumUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikePlaylistUseCase
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
    val isSaved: Boolean = false,
)

class EntityDetailViewModel(
    private val getEntityDetailsUseCase: GetEntityDetailsUseCase,
    private val toggleLikeAlbumUseCase: ToggleLikeAlbumUseCase,
    private val toggleLikePlaylistUseCase: ToggleLikePlaylistUseCase,
    private val subscribeChannelUseCase: SubscribeChannelUseCase? = null,
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

    fun toggleSave() {
        val entity = _uiState.value.entity ?: return
        val newSaved = !_uiState.value.isSaved
        _uiState.update { it.copy(isSaved = newSaved) }
        viewModelScope.launch {
            try {
                when (entity) {
                    is SearchResult.Album -> toggleLikeAlbumUseCase(entity.id, newSaved)
                    is SearchResult.Playlist -> toggleLikePlaylistUseCase(entity.id, newSaved)
                    is SearchResult.Artist -> subscribeChannelUseCase?.invoke(entity.id, newSaved)
                    else -> {}
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaved = !newSaved) }
            }
        }
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