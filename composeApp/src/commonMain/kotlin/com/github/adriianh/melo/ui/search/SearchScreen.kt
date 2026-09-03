package com.github.adriianh.melo.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.BatchSelectionBottomBar
import com.github.adriianh.melo.ui.components.MeloSearchBar
import com.github.adriianh.melo.ui.components.TrackInteractionContextMenu
import com.github.adriianh.melo.ui.components.rememberTrackInteraction
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.components.CategoryBrowsingContent
import com.github.adriianh.melo.ui.search.components.ExploreContent
import com.github.adriianh.melo.ui.search.components.SearchFilterChipsRow
import com.github.adriianh.melo.ui.search.components.SearchOverlayWrapper
import com.github.adriianh.melo.ui.search.components.SearchResultsContent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: SearchViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val playerViewModel: PlayerViewModel = koinViewModel()
    val playerState by playerViewModel.uiState.collectAsState()

    val interaction = rememberTrackInteraction(libraryViewModel, queueViewModel)
    val activeAccent = interaction.resolveActiveAccent(playerState)

    val focusManager = LocalFocusManager.current
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedTrackIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedTrackIds.isNotEmpty()

    val visibleSongs = remember(uiState) {
        when {
            uiState.isBrowsingCategory || uiState.browseCategoryResult != null -> {
                uiState.browseCategoryResult?.sections.orEmpty()
                    .flatMap { it.items }
                    .filterIsInstance<SearchResult.Song>()
                    .map { it.track }
            }

            uiState.selectedFilter == SearchFilterType.SONGS -> uiState.songResults
            uiState.query.isNotBlank() -> {
                uiState.summarySections
                    .flatMap { it.items }
                    .filterIsInstance<SearchResult.Song>()
                    .map { it.track }
                    .distinctBy { it.id }
            }

            else -> emptyList()
        }
    }

    LaunchedEffect(uiState.query, uiState.selectedFilter, uiState.isBrowsingCategory) {
        selectedTrackIds = emptySet()
    }

    TrackInteractionContextMenu(
        interaction = interaction,
        libraryState = libraryState,
        activeAccent = activeAccent,
        onArtistClick = onArtistClick,
        onAlbumClick = onAlbumClick
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        MeloSearchBar(
            query = uiState.query,
            onQueryChange = { q ->
                isSearchActive = true
                viewModel.onQueryChange(q)
            },
            onSearch = { q ->
                isSearchActive = false
                focusManager.clearFocus()
                viewModel.executeSearch(q)
            },
            onClear = {
                isSearchActive = false
                focusManager.clearFocus()
                viewModel.clearQuery()
            },
            onOpenSettings = onOpenSettings,
            onFocusChanged = { focused ->
                if (focused) {
                    isSearchActive = true
                    if (uiState.query.isNotBlank()) {
                        viewModel.onQueryChange(uiState.query)
                    }
                }
            },
            placeholderText = "Buscar canciones, artistas, álbumes...",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        AnimatedVisibility(
            visible = uiState.query.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            SearchFilterChipsRow(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { filter ->
                    isSearchActive = false
                    focusManager.clearFocus()
                    viewModel.onFilterSelected(filter)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            Crossfade(
                targetState = when {
                    uiState.isBrowsingCategory || uiState.browseCategoryResult != null -> "category"
                    uiState.query.isNotBlank() -> "results"
                    else -> "explore"
                }
            ) { state ->
                when (state) {
                    "category" -> CategoryBrowsingContent(
                        uiState = uiState,
                        paddingValues = paddingValues,
                        onBack = viewModel::exitBrowsing,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        onPlaylistClick = onPlaylistClick,
                        queueViewModel = queueViewModel,
                        onMoreClick = { interaction.openContextMenu(it) },
                        onSwipeLeft = { interaction.showAddedToQueueSnackbar(it, activeAccent) },
                        isLiked = { track -> libraryState.likedSongs.any { it.id == track.id } },
                        onSwipeRight = { track ->
                            val trackIsLiked = libraryState.likedSongs.any { it.id == track.id }
                            interaction.showToggledLikeSnackbar(track, trackIsLiked, activeAccent)
                        },
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

                    "results" -> SearchResultsContent(
                        uiState = uiState,
                        paddingValues = paddingValues,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        onPlaylistClick = onPlaylistClick,
                        queueViewModel = queueViewModel,
                        onMoreClick = { interaction.openContextMenu(it) },
                        onSwipeLeft = { interaction.showAddedToQueueSnackbar(it, activeAccent) },
                        isLiked = { track -> libraryState.likedSongs.any { it.id == track.id } },
                        onSwipeRight = { track ->
                            val trackIsLiked = libraryState.likedSongs.any { it.id == track.id }
                            interaction.showToggledLikeSnackbar(track, trackIsLiked, activeAccent)
                        },
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

                    "explore" -> ExploreContent(
                        uiState = uiState,
                        paddingValues = paddingValues,
                        onAlbumClick = onAlbumClick,
                        onPlaylistClick = onPlaylistClick,
                        onBrowseCategory = viewModel::browseCategory,
                        queueViewModel = queueViewModel,
                        onMoreClick = { interaction.openContextMenu(it) }
                    )
                }
            }

            val shouldShowOverlay = isSearchActive && (
                    (uiState.query.isBlank() && uiState.recentSearches.isNotEmpty()) ||
                            (uiState.query.isNotBlank() && uiState.suggestions.isNotEmpty())
                    )

            if (shouldShowOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            isSearchActive = false
                            focusManager.clearFocus()
                        }
                )
            }

            SearchOverlayWrapper(
                visible = shouldShowOverlay,
                query = uiState.query,
                suggestions = uiState.suggestions,
                recentSearches = uiState.recentSearches,
                onSelect = { selectedQuery ->
                    isSearchActive = false
                    focusManager.clearFocus()
                    viewModel.onSuggestionSelected(selectedQuery)
                },
                onDeleteRecentSearch = viewModel::deleteRecentSearch
            )

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
