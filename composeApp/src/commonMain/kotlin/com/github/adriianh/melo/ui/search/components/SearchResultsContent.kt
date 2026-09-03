package com.github.adriianh.melo.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.MeloEmptyState
import com.github.adriianh.melo.ui.components.SuggestionSkeletonCard
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.SearchFilterType
import com.github.adriianh.melo.ui.search.SearchUiState

@Composable
fun SearchResultsContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String, String, String?, String) -> Unit,
    queueViewModel: QueueViewModel,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    isLiked: (Track) -> Boolean,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
    when {
        uiState.isSearching -> SearchResultsSkeleton(modifier, paddingValues)

        !uiState.hasResults -> MeloEmptyState(
            title = if (uiState.isOfflineSearch) "No se encontraron descargas ni archivos locales" else "No se encontraron resultados",
            message = if (uiState.isOfflineSearch) "Intenta buscar el título de una canción guardada en tu dispositivo" else "Intenta buscar con otras palabras o cambiar de filtro",
            modifier = modifier
        )

        else -> when (uiState.selectedFilter) {
            SearchFilterType.ALL -> AllResultsContent(
                uiState = uiState,
                paddingValues = paddingValues,
                queueViewModel = queueViewModel,
                onAlbumClick = onAlbumClick,
                onArtistClick = onArtistClick,
                onPlaylistClick = onPlaylistClick,
                onMoreClick = onMoreClick,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight,
                isLiked = isLiked,
                isSelectionMode = isSelectionMode,
                selectedTrackIds = selectedTrackIds,
                onToggleSelectTrack = onToggleSelectTrack,
                onTrackLongClick = onTrackLongClick,
                modifier = modifier
            )

            SearchFilterType.SONGS -> SongsResultsContent(
                uiState = uiState,
                paddingValues = paddingValues,
                queueViewModel = queueViewModel,
                onMoreClick = onMoreClick,
                isSelectionMode = isSelectionMode,
                selectedTrackIds = selectedTrackIds,
                onToggleSelectTrack = onToggleSelectTrack,
                onTrackLongClick = onTrackLongClick,
                modifier = modifier
            )

            SearchFilterType.ALBUMS -> AlbumsResultsContent(
                uiState = uiState,
                paddingValues = paddingValues,
                onAlbumClick = onAlbumClick
            )

            SearchFilterType.ARTISTS -> ArtistsResultsContent(
                uiState = uiState,
                paddingValues = paddingValues,
                onArtistClick = onArtistClick,
                modifier = modifier
            )

            SearchFilterType.PLAYLISTS -> PlaylistsResultsContent(
                uiState = uiState,
                paddingValues = paddingValues,
                onPlaylistClick = onPlaylistClick,
                modifier = modifier
            )

            SearchFilterType.VIDEOS -> VideosResultsContent(
                uiState = uiState,
                paddingValues = paddingValues,
                queueViewModel = queueViewModel,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun SearchResultsSkeleton(
    modifier: Modifier,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = searchResultContentPadding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(6) {
            SuggestionSkeletonCard()
        }
    }
}