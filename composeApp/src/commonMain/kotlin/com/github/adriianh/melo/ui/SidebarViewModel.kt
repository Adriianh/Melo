package com.github.adriianh.melo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.library.GetAccountProfileUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SidebarUiState(
    val isLoggedIn: Boolean = false,
    val profile: AccountProfile? = null,
    val playlists: List<SearchResult.Playlist> = emptyList(),
)

class SidebarViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val getAccountProfileUseCase: GetAccountProfileUseCase,
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SidebarUiState())
    val uiState: StateFlow<SidebarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSettingsUseCase().collectLatest { settings ->
                val loggedIn = !settings.sessionCookies.isNullOrBlank()
                _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
                if (loggedIn) {
                    refreshLibrary()
                }
            }
        }
    }

    fun refreshLibrary() {
        if (!_uiState.value.isLoggedIn) return

        viewModelScope.launch {
            val profileResult = getAccountProfileUseCase()
            val playlistsResult = getUserPlaylistsUseCase()

            _uiState.value = _uiState.value.copy(
                profile = profileResult.getOrNull() ?: _uiState.value.profile,
                playlists = playlistsResult.getOrDefault(emptyList())
            )
        }
    }
}
