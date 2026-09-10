package com.github.adriianh.melo.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
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
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.LocalSelectionMode
import com.github.adriianh.melo.ui.components.BatchSelectionBottomBar
import com.github.adriianh.melo.ui.components.MeloErrorState
import com.github.adriianh.melo.ui.components.MeloSearchBar
import com.github.adriianh.melo.ui.components.TrackInteractionContextMenu
import com.github.adriianh.melo.ui.components.rememberTrackInteraction
import com.github.adriianh.melo.ui.home.components.HomeContent
import com.github.adriianh.melo.ui.home.components.HomeSearchResultsView
import com.github.adriianh.melo.ui.home.components.HomeSkeletonLoading
import com.github.adriianh.melo.ui.library.LibraryUiState
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.viewmodel.koinViewModel

/** Represents the four mutually exclusive display states of the home feed. */
private enum class HomeFeedState { Loading, Content, Error, Search }

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onAlbumClick: (id: String, title: String, artwork: String?, author: String) -> Unit = { _, _, _, _ -> },
    onPlaylistClick: (id: String, title: String, artwork: String?, author: String) -> Unit = { _, _, _, _ -> },
    onArtistClick: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: HomeViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentAccent = MaterialTheme.colorScheme.primary
    val interaction = rememberTrackInteraction(libraryViewModel, queueViewModel)
    val activeAccent = interaction.resolveActiveAccent(currentAccent)

    val isLibraryNeeded = interaction.contextMenuTrack != null ||
            !interaction.addToPlaylistTracks.isNullOrEmpty() ||
            uiState.searchQuery.isNotBlank()
    val libraryState by remember(isLibraryNeeded) {
        if (isLibraryNeeded) {
            libraryViewModel.uiState
        } else {
            flowOf(LibraryUiState())
        }
    }.collectAsState(initial = LibraryUiState())

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

    val visibleSongs =
        remember(isSelectionMode, uiState.searchQuery, uiState.searchResults, uiState.sections) {
            if (!isSelectionMode) {
                emptyList()
            } else if (uiState.searchQuery.isNotBlank()) {
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
            val feedState = when {
                uiState.searchQuery.isNotBlank() -> HomeFeedState.Search
                uiState.isLoading -> HomeFeedState.Loading
                uiState.error != null -> HomeFeedState.Error
                else -> HomeFeedState.Content
            }

            AnimatedContent(
                targetState = feedState,
                transitionSpec = {
                    val isLoadingToContent = initialState == HomeFeedState.Loading &&
                            targetState == HomeFeedState.Content
                    if (isLoadingToContent) {
                        (fadeIn(animationSpec = tween(220)) togetherWith fadeOut(
                            animationSpec = tween(
                                220
                            )
                        ))
                    } else {
                        (fadeIn(animationSpec = tween(150)) togetherWith fadeOut(
                            animationSpec = tween(
                                150
                            )
                        ))
                    }
                },
                label = "HomeFeedState"
            ) { state ->
                when (state) {
                    HomeFeedState.Search -> HomeSearchResultsView(
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

                    HomeFeedState.Loading -> HomeSkeletonLoading(
                        chips = uiState.chips,
                        onChipClick = viewModel::toggleChip,
                        selectedChip = uiState.selectedChip
                    )

                    HomeFeedState.Error -> MeloErrorState(
                        uiState.error,
                        onRetry = viewModel::loadFeed
                    )

                    HomeFeedState.Content -> HomeContent(
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