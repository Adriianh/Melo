package com.github.adriianh.melo.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    modifier: Modifier = Modifier
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
            items(results) { track ->
                MeloSwipeableItem(
                    onSwipeLeft = { interaction.showAddedToQueueSnackbar(track) },
                    onSwipeRight = {
                        val trackIsLiked = likedSongs.any { it.id == track.id }
                        interaction.showToggledLikeSnackbar(track, trackIsLiked, activeAccent)
                    }
                ) {
                    TrackRow(
                        track = track,
                        onClick = { queueViewModel.playTrack(track) },
                        onMoreClick = { onMoreClick(track) }
                    )
                }
            }
        }
    }
}
