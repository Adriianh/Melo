package com.github.adriianh.melo.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.ui.LocalSelectionMode
import com.github.adriianh.melo.ui.components.TrackInteractionContextMenu
import com.github.adriianh.melo.ui.components.rememberTrackInteraction
import com.github.adriianh.melo.ui.library.components.LibraryHeader
import com.github.adriianh.melo.ui.library.components.LibraryNotLoggedInCard
import com.github.adriianh.melo.ui.library.components.LibraryTabContent
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.getPlatform
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenSettings: () -> Unit = {},
    onAlbumClick: (id: String, title: String, artwork: String?, author: String) -> Unit = { _, _, _, _ -> },
    onPlaylistClick: (id: String, title: String, artwork: String?, author: String) -> Unit = { _, _, _, _ -> },
    onArtistClick: (String) -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: LibraryViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val playerViewModel: PlayerViewModel = koinViewModel()
    val playerState by playerViewModel.uiState.collectAsState()

    val interaction = rememberTrackInteraction(viewModel, queueViewModel)
    val activeAccent = interaction.resolveActiveAccent(playerState)

    var selectedTrackIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedTrackIds.isNotEmpty()

    val selectionModeState = LocalSelectionMode.current
    LaunchedEffect(isSelectionMode) {
        selectionModeState.value = isSelectionMode
    }
    DisposableEffect(Unit) {
        onDispose {
            selectionModeState.value = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onScreenVisible()
    }

    LaunchedEffect(state.selectedTab) {
        selectedTrackIds = emptySet()
    }

    val query = state.searchQuery.trim().lowercase()
    val sortOrder = state.sortOrder
    val filters = rememberLibraryFilters(state, query, sortOrder)

    val activeCategory = LibraryCategory.fromTab(state.selectedTab)
    val summaryText = remember(
        activeCategory,
        state.customPlaylists.size,
        state.playlists.size,
        state.likedSongs.size,
        state.albums.size,
        state.artists.size,
        state.downloadedTracks.size,
        state.localTracks.size,
        state.history.size
    ) {
        when (activeCategory) {
            LibraryCategory.COLLECTION -> {
                val totalPlaylists = state.customPlaylists.size + state.playlists.size
                "$totalPlaylists playlists • ${state.likedSongs.size} me gusta • ${state.albums.size} álbumes"
            }

            LibraryCategory.DEVICE -> {
                "${state.downloadedTracks.size} descargadas • ${state.localTracks.size} locales"
            }

            LibraryCategory.HISTORY -> {
                "${state.history.size} canciones reproducidas"
            }
        }
    }

    TrackInteractionContextMenu(
        interaction = interaction,
        libraryState = state,
        activeAccent = activeAccent,
        onArtistClick = onArtistClick
    )

    val platform = remember { getPlatform() }
    val horizontalPadding = if (platform.type == PlatformType.DESKTOP) 24.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                top = 16.dp,
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val showViewModeToggle = state.selectedTab in listOf(
            LibraryTab.PLAYLISTS,
            LibraryTab.ALBUMS,
            LibraryTab.ARTISTS
        )

        LibraryHeader(
            profile = state.profile,
            selectedTab = state.selectedTab,
            summaryText = summaryText,
            searchQuery = state.searchQuery,
            isSearchActive = state.isSearchActive,
            sortOrder = state.sortOrder,
            viewMode = state.viewMode,
            showViewModeToggle = showViewModeToggle,
            onTabSelected = viewModel::selectTab,
            onSearchQueryChange = viewModel::setSearchQuery,
            onToggleSearch = viewModel::toggleSearchActive,
            onSortOrderChange = viewModel::setSortOrder,
            onViewModeChange = viewModel::setViewMode,
            onOpenSettings = onOpenSettings,
            onRefresh = viewModel::refreshAll
        )

        val isOfflineTab =
            state.selectedTab == LibraryTab.DOWNLOADS ||
                    state.selectedTab == LibraryTab.LOCAL ||
                    state.selectedTab == LibraryTab.PLAYLISTS ||
                    state.selectedTab == LibraryTab.HISTORY
        if (!state.isLoggedIn && !isOfflineTab) {
            LibraryNotLoggedInCard(onLoginClick = onOpenSettings)
        } else if (state.isLoading && !isOfflineTab) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LibraryTabContent(
                tab = state.selectedTab,
                state = state,
                filters = filters,
                activeAccent = activeAccent,
                isSelectionMode = isSelectionMode,
                selectedTrackIds = selectedTrackIds,
                interaction = interaction,
                onPlaylistClick = onPlaylistClick,
                onAlbumClick = onAlbumClick,
                onArtistClick = onArtistClick,
                onToggleSelectTrack = { track ->
                    selectedTrackIds =
                        if (track.id in selectedTrackIds) selectedTrackIds - track.id else selectedTrackIds + track.id
                },
                onTrackLongClick = { track ->
                    if (!isSelectionMode) selectedTrackIds = setOf(track.id)
                },
                onClearSelection = { selectedTrackIds = emptySet() },
                onSelectAllToggle = { visibleSongs ->
                    selectedTrackIds =
                        if (selectedTrackIds.size == visibleSongs.size) emptySet() else visibleSongs.map { it.id }
                            .toSet()
                },
                viewModel = viewModel,
                queueViewModel = queueViewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}