package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.SuggestionSkeletonCard
import com.github.adriianh.melo.ui.components.SuggestionTrackCard
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerUiState
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun NowPlayingQueueSection(
    state: PlayerUiState,
    activeAccent: Color,
    modifier: Modifier = Modifier,
    queueViewModel: QueueViewModel = koinViewModel(),
    onMoreClick: (Track, Boolean) -> Unit = { _, _ -> },
    onSwipeQueueItem: (Int) -> Unit = {},
    onSwipeSuggestionItem: (Track) -> Unit = {},
    onPlayNextSuggestionItem: ((Track) -> Unit)? = null,
    onSwipeRight: (Track) -> Unit = {},
    isLiked: (Track) -> Boolean = { false }
) {
    val queueState by queueViewModel.queueState.collectAsState()
    val suggestions by queueViewModel.suggestions.collectAsState()
    val isLoadingSuggestions by queueViewModel.isLoadingSuggestions.collectAsState()

    val upNextTracks = remember(queueState.tracks, queueState.currentIndex) {
        if (queueState.currentIndex >= 0 && queueState.currentIndex < queueState.tracks.size) {
            queueState.tracks.drop(queueState.currentIndex + 1)
        } else {
            emptyList()
        }
    }

    val upNextKeys = remember(upNextTracks) {
        val counts = mutableMapOf<String, Int>()
        upNextTracks.map { track ->
            val count = counts.getOrPut(track.id) { 0 }
            counts[track.id] = count + 1
            "screen_q_${track.id}_$count"
        }
    }

    val suggestionKeys = remember(suggestions) {
        val counts = mutableMapOf<String, Int>()
        suggestions.map { track ->
            val count = counts.getOrPut(track.id) { 0 }
            counts[track.id] = count + 1
            "screen_sugg_${track.id}_$count"
        }
    }

    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromRel = upNextKeys.indexOf(from.key)
        val toRel = upNextKeys.indexOf(to.key)
        if (fromRel != -1 && toRel != -1 && fromRel != toRel) {
            val fromAbs = queueState.currentIndex + 1 + fromRel
            val toAbs = queueState.currentIndex + 1 + toRel
            queueViewModel.moveTrack(fromAbs, toAbs)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "REPRODUCIENDO AHORA",
                style = MeloType.labelSmall,
                fontWeight = FontWeight.Bold,
                color = activeAccent,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeAccent.copy(alpha = 0.15f))
                    .border(0.5.dp, activeAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MeloAsyncImage(
                        url = state.albumArt,
                        contentDescription = state.title,
                        size = 42.dp,
                        shape = RoundedCornerShape(6.dp)
                    )
                    if (state.isBuffering) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
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
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        state.artist,
                        style = MeloType.labelSmall,
                        color = MeloColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (upNextTracks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "A CONTINUACIÓN",
                    style = MeloType.labelSmall,
                    letterSpacing = 1.2.sp,
                    color = MeloColors.textMuted
                )
            }

            itemsIndexed(
                upNextTracks,
                key = { index, _ -> upNextKeys.getOrElse(index) { "screen_q_$index" } }) { index, track ->
                val itemKey = upNextKeys.getOrElse(index) { "screen_q_$index" }
                ReorderableItem(
                    state = reorderState,
                    key = itemKey
                ) { isDragging ->
                    MeloSwipeableItem(
                        onSwipeLeft = { onSwipeQueueItem(queueState.currentIndex + 1 + index) },
                        onSwipeRight = { onSwipeRight(track) },
                        swipeRightIcon = if (isLiked(track)) Icons.Default.HeartBroken else Icons.Default.Favorite,
                        swipeLeftIcon = Icons.Default.Delete,
                        swipeLeftColor = Color(0xFFE91E63)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDragging) activeAccent.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { queueViewModel.playTrackInQueue(track) }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MeloAsyncImage(
                                url = track.artworkUrl,
                                contentDescription = track.title,
                                size = 36.dp,
                                shape = RoundedCornerShape(6.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    style = MeloType.labelMedium,
                                    color = if (isDragging) activeAccent else MeloColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    track.artist,
                                    style = MeloType.labelSmall,
                                    color = MeloColors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onMoreClick(track, true) }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Opciones",
                                    tint = MeloColors.textMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Reordenar",
                                tint = if (isDragging) activeAccent else MeloColors.textMuted,
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(4.dp)
                                    .draggableHandle(
                                        onDragStarted = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SUGERENCIAS PARA TI",
                    style = MeloType.labelSmall,
                    letterSpacing = 1.2.sp,
                    color = MeloColors.textMuted
                )
                IconButton(
                    onClick = { queueViewModel.refreshSuggestions() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refrescar sugerencias",
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(16.dp)
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
                    "No hay más sugerencias por ahora",
                    style = MeloType.labelSmall,
                    color = MeloColors.textMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            itemsIndexed(
                suggestions,
                key = { index, _ -> suggestionKeys.getOrElse(index) { "screen_sugg_$index" } }) { _, track ->
                MeloSwipeableItem(
                    onSwipeLeft = { onSwipeSuggestionItem(track) },
                    onSwipeRight = { onSwipeRight(track) },
                    swipeRightIcon = if (isLiked(track)) Icons.Default.HeartBroken else Icons.Default.Favorite
                ) {
                    Box {
                        SuggestionTrackCard(
                            track = track,
                            activeAccent = activeAccent,
                            isCurrent = state.currentTrack?.id == track.id,
                            isPlaying = state.isPlaying && state.currentTrack?.id == track.id,
                            onClick = { queueViewModel.playTrack(track) },
                            onAddClick = {
                                onPlayNextSuggestionItem?.invoke(track)
                                    ?: queueViewModel.insertTrackNext(track)
                            },
                            onMoreClick = { onMoreClick(track, false) }
                        )
                    }
                }
            }
        }
    }
}
