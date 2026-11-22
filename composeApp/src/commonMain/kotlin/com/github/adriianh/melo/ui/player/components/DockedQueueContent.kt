package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_refresh_suggestions
import com.github.adriianh.melo.resources.a11y_remove_from_queue
import com.github.adriianh.melo.resources.player_queue_collapse
import com.github.adriianh.melo.resources.player_queue_more_count
import com.github.adriianh.melo.resources.player_section_now_playing
import com.github.adriianh.melo.resources.player_section_suggestions
import com.github.adriianh.melo.resources.player_section_up_next
import com.github.adriianh.melo.resources.player_suggestions_empty
import com.github.adriianh.melo.ui.components.cards.SuggestionSkeletonCard
import com.github.adriianh.melo.ui.components.cards.SuggestionTrackCard
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DockedQueueContent(
    state: PlayerUiState,
    activeAccent: Color,
    queueViewModel: QueueViewModel = koinViewModel(),
) {
    val queueState by queueViewModel.queueState.collectAsState()
    val suggestions by queueViewModel.suggestions.collectAsState()
    val isLoadingSuggestions by queueViewModel.isLoadingSuggestions.collectAsState()
    var isQueueExpanded by remember { mutableStateOf(false) }

    val upNextTracks =
        remember(queueState.tracks, queueState.currentIndex) {
            if (queueState.currentIndex >= 0 && queueState.currentIndex < queueState.tracks.size) {
                queueState.tracks.drop(queueState.currentIndex + 1)
            } else {
                emptyList()
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Text(
                stringResource(Res.string.player_section_now_playing),
                style = MeloType.labelSmall,
                fontWeight = FontWeight.Bold,
                color = activeAccent,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(activeAccent.copy(alpha = 0.15f))
                        .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MeloAsyncImage(
                        url = state.albumArt,
                        contentDescription = state.title,
                        size = 42.dp,
                        shape = RoundedCornerShape(6.dp),
                    )
                    if (state.isBuffering) {
                        Box(
                            modifier =
                                Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.title,
                        style = MeloType.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        state.artist,
                        style = MeloType.labelSmall,
                        color = MeloColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (upNextTracks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Res.string.player_section_up_next),
                        style = MeloType.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MeloColors.textSecondary,
                    )
                    if (upNextTracks.size > 1) {
                        Text(
                            text =
                                if (isQueueExpanded) {
                                    stringResource(Res.string.player_queue_collapse)
                                } else {
                                    stringResource(Res.string.player_queue_more_count, upNextTracks.size - 1)
                                },
                            style = MeloType.labelSmall.copy(fontSize = 11.sp),
                            color = activeAccent,
                            fontWeight = FontWeight.SemiBold,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { isQueueExpanded = !isQueueExpanded }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            val visibleQueueTracks = if (isQueueExpanded) upNextTracks else upNextTracks.take(1)
            itemsIndexed(
                visibleQueueTracks,
                key = { index, track -> "docked_q_${index}_${track.id}" },
            ) { _, track ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MeloColors.surface1.copy(alpha = 0.5f))
                            .clickable { queueViewModel.playTrackInQueue(track) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MeloAsyncImage(
                        url = track.artworkUrl,
                        contentDescription = track.title,
                        size = 36.dp,
                        shape = RoundedCornerShape(6.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.title,
                            style = MeloType.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MeloColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            track.artist,
                            style = MeloType.labelSmall.copy(fontSize = 10.sp),
                            color = MeloColors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = { queueViewModel.removeTrackFromQueue(track) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(Res.string.a11y_remove_from_queue),
                            tint = MeloColors.textMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        stringResource(Res.string.player_section_suggestions),
                        style = MeloType.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textSecondary,
                    )
                    if (isLoadingSuggestions) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = activeAccent,
                        )
                    }
                }
                IconButton(
                    onClick = { queueViewModel.refreshSuggestions() },
                    modifier = Modifier.size(22.dp),
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(Res.string.a11y_refresh_suggestions),
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        if (isLoadingSuggestions && suggestions.isEmpty()) {
            items(4) {
                SuggestionSkeletonCard()
            }
        } else if (suggestions.isEmpty()) {
            item {
                Text(
                    stringResource(Res.string.player_suggestions_empty),
                    style = MeloType.labelSmall,
                    color = MeloColors.textMuted,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            itemsIndexed(
                suggestions,
                key = { index, track -> "docked_sugg_${index}_${track.id}" },
            ) { _, track ->
                SuggestionTrackCard(
                    track = track,
                    activeAccent = activeAccent,
                    isCurrent = state.currentTrack?.id == track.id,
                    isPlaying = state.isPlaying && state.currentTrack?.id == track.id,
                    onClick = { queueViewModel.playTrack(track) },
                    onAddClick = { queueViewModel.insertTrackNext(track) },
                )
            }
        }
    }
}
