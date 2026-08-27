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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.ui.components.MeloSearchBar
import com.github.adriianh.melo.ui.components.TrackContextMenu
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

    if (interaction.contextMenuTrack != null) {
        val track = interaction.contextMenuTrack!!
        val isLiked = libraryState.likedSongs.any { it.id == track.id }
        TrackContextMenu(
            track = track,
            onDismissRequest = interaction::dismissContextMenu,
            onPlayNext = {
                interaction.showPlayNextSnackbar(track, activeAccent)
                interaction.contextMenuTrack = null
            },
            onAddToQueue = {
                interaction.showAddedToQueueSnackbar(track, activeAccent)
                interaction.contextMenuTrack = null
            },
            onToggleLike = {
                interaction.showToggledLikeSnackbar(track, isLiked, activeAccent)
                interaction.contextMenuTrack = null
            },
            isLiked = isLiked,
            onAddToPlaylist = { /* TODO */ },
            onGoToArtist = {
                interaction.contextMenuTrack = null
                onArtistClick(track.artist)
            },
            onGoToAlbum = {
                interaction.contextMenuTrack = null
            },
            onShare = { /* TODO */ }
        )
    }

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
                }
            )
        }
    }
}
