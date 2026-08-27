package com.github.adriianh.melo.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.util.MeloColors

fun LazyListScope.detailTrackItems(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isLiked: (Track) -> Boolean,
    onTrackClick: (Int, Track) -> Unit,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    accentColor: Color,
    keyPrefix: String = "detail",
    itemModifier: Modifier = Modifier
) {
    itemsIndexed(
        tracks,
        key = { index, song -> "${keyPrefix}_${song.id}_$index" }
    ) { index, song ->
        val isPlayingThis = currentTrackId == song.id
        val liked = isLiked(song)
        Box(modifier = itemModifier) {
            MeloSwipeableItem(
                onSwipeLeft = { onSwipeLeft(song) },
                onSwipeRight = { onSwipeRight(song) },
                swipeRightIcon = if (liked) Icons.Default.HeartBroken else Icons.Default.Favorite,
                swipeLeftIcon = Icons.AutoMirrored.Filled.QueueMusic,
                swipeLeftColor = MeloColors.brandAccent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isPlayingThis) accentColor.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                ) {
                    TrackRow(
                        track = song,
                        trackNumber = index + 1,
                        isCurrent = isPlayingThis,
                        isPlaying = isPlayingThis && isPlaying,
                        onClick = { onTrackClick(index, song) },
                        onMoreClick = { onMoreClick(song) }
                    )
                }
            }
        }
    }
}