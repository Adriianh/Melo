package com.github.adriianh.melo.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.BatchSelectionBottomBar
import com.github.adriianh.melo.ui.components.MeloErrorState
import com.github.adriianh.melo.ui.components.MeloSearchBar
import com.github.adriianh.melo.ui.components.TrackInteractionContextMenu
import com.github.adriianh.melo.ui.components.rememberTrackInteraction
import com.github.adriianh.melo.ui.home.components.HomeContent
import com.github.adriianh.melo.ui.home.components.HomeSearchResultsView
import com.github.adriianh.melo.ui.home.components.HomeSkeletonLoading
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onAlbumClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: HomeViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val playerViewModel: PlayerViewModel = koinViewModel()
    val playerState by playerViewModel.uiState.collectAsState()

    val interaction = rememberTrackInteraction(libraryViewModel, queueViewModel)
    val activeAccent = interaction.resolveActiveAccent(playerState)

    var selectedTrackIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedTrackIds.isNotEmpty()

    val visibleSongs = remember(uiState) {
        if (uiState.searchQuery.isNotBlank()) {
            uiState.searchResults
        } else {
            uiState.sections
                .filter { it.type == HomeSectionType.SONGS }
                .flatMap { it.items }
                .filterIsInstance<SearchResult.Song>()
                .map { it.track }
        }
    }

    LaunchedEffect(uiState.searchQuery, uiState.selectedChip) {
        selectedTrackIds = emptySet()
    }

    TrackInteractionContextMenu(
        interaction = interaction,
        libraryState = libraryState,
        activeAccent = activeAccent,
        onArtistClick = onArtistClick
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        MeloSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onClear = viewModel::clearSearch,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.searchQuery.isNotBlank() -> {
                    HomeSearchResultsView(
                        activeAccent = activeAccent,
                        isSearching = uiState.isSearching,
                        results = uiState.searchResults,
                        likedSongs = libraryState.likedSongs,
                        queueViewModel = queueViewModel,
                        onMoreClick = { interaction.openContextMenu(it) },
                        interaction = interaction,
                        isSelectionMode = isSelectionMode,
                        selectedTrackIds = selectedTrackIds,
                        onToggleSelectTrack = { track ->
                            selectedTrackIds =
                                if (track.id in selectedTrackIds) selectedTrackIds - track.id else selectedTrackIds + track.id
                        },
                        onTrackLongClick = { track ->
                            if (!isSelectionMode) selectedTrackIds = setOf(track.id)
                        }
                    )
                }

                uiState.isLoading -> {
                    HomeSkeletonLoading(
                        chips = uiState.chips,
                        onChipClick = viewModel::toggleChip,
                        selectedChip = uiState.selectedChip
                    )
                }

                uiState.error != null -> MeloErrorState(
                    uiState.error,
                    onRetry = viewModel::loadFeed
                )

                else -> HomeContent(
                    uiState = uiState,
                    onChipClick = viewModel::toggleChip,
                    onLoadMore = viewModel::loadMore,
                    onAlbumClick = onAlbumClick,
                    onPlaylistClick = onPlaylistClick,
                    onArtistClick = onArtistClick,
                    queueViewModel = queueViewModel,
                    paddingValues = paddingValues,
                    onMoreClick = { interaction.openContextMenu(it) },
                    isSelectionMode = isSelectionMode,
                    selectedTrackIds = selectedTrackIds,
                    onToggleSelectTrack = { track ->
                        selectedTrackIds =
                            if (track.id in selectedTrackIds) selectedTrackIds - track.id else selectedTrackIds + track.id
                    },
                    onTrackLongClick = { track ->
                        if (!isSelectionMode) selectedTrackIds = setOf(track.id)
                    }
                )
            }

            BatchSelectionBottomBar(
                isVisible = isSelectionMode,
                selectedCount = selectedTrackIds.size,
                totalCount = visibleSongs.size,
                onClearSelection = { selectedTrackIds = emptySet() },
                onSelectAllToggle = {
                    selectedTrackIds =
                        if (selectedTrackIds.size == visibleSongs.size) emptySet() else visibleSongs.map { it.id }
                            .toSet()
                },
                onAddToPlaylist = {
                    val selTracks = visibleSongs.filter { it.id in selectedTrackIds }
                    interaction.openAddToPlaylist(selTracks)
                    selectedTrackIds = emptySet()
                },
                onAddToQueue = {
                    val selTracks = visibleSongs.filter { it.id in selectedTrackIds }
                    queueViewModel.addAllToQueue(selTracks)
                    interaction.snackbar.show(
                        msg = if (selTracks.size == 1) "1 canción añadida a la cola" else "${selTracks.size} canciones añadidas a la cola",
                        actionColor = activeAccent
                    )
                    selectedTrackIds = emptySet()
                },
                onDownload = {
                    val selTracks = visibleSongs.filter { it.id in selectedTrackIds }
                    selTracks.forEach { libraryViewModel.downloadTrack(it) }
                    interaction.snackbar.show(
                        msg = "Descargando ${selTracks.size} canciones...",
                        actionColor = activeAccent
                    )
                    selectedTrackIds = emptySet()
                },
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}