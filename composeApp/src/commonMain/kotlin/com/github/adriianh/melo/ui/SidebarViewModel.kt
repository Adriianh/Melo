package com.github.adriianh.melo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.cache.LibraryCache
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.LibraryCacheData
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.network.NetworkMonitor
import com.github.adriianh.core.domain.usecase.library.GetAccountProfileUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.util.MeloDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class SidebarUiState(
    val isLoggedIn: Boolean = false,
    val profile: AccountProfile? = null,
    val playlists: List<SearchResult.Playlist> = emptyList(),
)

class SidebarViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val getAccountProfileUseCase: GetAccountProfileUseCase,
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val libraryCache: LibraryCache,
    networkMonitor: NetworkMonitor? = null,
    private val ioDispatcher: CoroutineDispatcher = MeloDispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SidebarUiState())
    val uiState: StateFlow<SidebarUiState> = _uiState.asStateFlow()

    init {
        val cached = libraryCache.getSync()
        if (cached != null) {
            _uiState.update {
                it.copy(
                    profile = cached.profile ?: it.profile,
                    playlists = cached.playlists.ifEmpty { it.playlists }
                )
            }
        } else {
            viewModelScope.launch(ioDispatcher) {
                libraryCache.get()?.let { asyncCached ->
                    _uiState.update {
                        it.copy(
                            profile = asyncCached.profile ?: it.profile,
                            playlists = asyncCached.playlists.ifEmpty { it.playlists }
                        )
                    }
                }
            }
        }

        networkMonitor?.let { monitor ->
            viewModelScope.launch(ioDispatcher) {
                monitor.isOnline
                    .collectLatest { isOnline ->
                        if (isOnline && _uiState.value.isLoggedIn) {
                            refreshLibrary()
                        }
                    }
            }
        }

        viewModelScope.launch(ioDispatcher) {
            val initialCookies = getSettingsUseCase.getSnapshot().sessionCookies
                ?.takeIf { it.isNotBlank() }

            val initiallyLoggedIn = initialCookies != null
            _uiState.update { it.copy(isLoggedIn = initiallyLoggedIn) }
            if (initiallyLoggedIn) {
                delay(1500.milliseconds)
                refreshLibrary()
            }

            getSettingsUseCase()
                .map { it.sessionCookies?.takeIf { cookies -> cookies.isNotBlank() } }
                .dropWhile { it == initialCookies }
                .distinctUntilChanged()
                .collectLatest { cookies ->
                    val loggedIn = cookies != null
                    _uiState.update { it.copy(isLoggedIn = loggedIn) }
                    if (loggedIn) {
                        refreshLibrary()
                    }
                }
        }
    }

    fun refreshLibrary() {
        if (!_uiState.value.isLoggedIn) return

        viewModelScope.launch(ioDispatcher) {
            val profileResult = getAccountProfileUseCase()
            val playlistsResult = getUserPlaylistsUseCase()

            val newProfile = profileResult.getOrNull() ?: _uiState.value.profile
            val newPlaylists = playlistsResult.getOrNull() ?: _uiState.value.playlists

            _uiState.update {
                it.copy(
                    profile = newProfile,
                    playlists = newPlaylists
                )
            }

            if (playlistsResult.isSuccess || profileResult.isSuccess) {
                val currentCache = libraryCache.get() ?: LibraryCacheData()

                libraryCache.save(
                    currentCache.copy(
                        profile = newProfile ?: currentCache.profile,
                        playlists = if (playlistsResult.isSuccess) newPlaylists else currentCache.playlists
                    )
                )
            }
        }
    }
}
