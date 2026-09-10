package com.github.adriianh.melo.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.TrackInteractionState
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun HomeSearchResultsView(
    activeAccent: Color,
    isSearching: Boolean,
    likedSongs: List<Track>,
    results: List<Track>,
    queueViewModel: QueueViewModel,
    onMoreClick: (Track) -> Unit,
    interaction: TrackInteractionState,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
    if (isSearching) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (results.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No se encontraron resultados",
                style = MeloType.body,
                color = MeloColors.textMuted
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    "Canciones",
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(results, key = { it.id }) { track ->
                val isLiked = likedSongs.any { it.id == track.id }
                val isSelected = track.id in selectedTrackIds
                MeloSwipeableItem(
                    onSwipeLeft = {
                        if (!isSelectionMode) interaction.showAddedToQueueSnackbar(
                            track,
                            activeAccent
                        )
                    },
                    onSwipeRight = {
                        if (!isSelectionMode) {
                            interaction.showToggledLikeSnackbar(track, isLiked, activeAccent)
                        }
                    },
                    swipeRightIcon = if (isLiked) Icons.Default.HeartBroken else Icons.Default.Favorite,
                    swipeLeftIcon = Icons.AutoMirrored.Filled.QueueMusic,
                    swipeLeftColor = MeloColors.brandAccent
                ) {
                    TrackRow(
                        track = track,
                        onClick = { queueViewModel.playTrack(track) },
                        onMoreClick = if (isSelectionMode) null else {
                            { onMoreClick(track) }
                        },
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onSelectionToggle = { onToggleSelectTrack?.invoke(track) },
                        onLongClick = onTrackLongClick?.let { onLong -> { onLong(track) } }
                    )
                }
            }
        }
    }
}