package com.github.adriianh.melo.ui.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.MeloEmptyState
import com.github.adriianh.melo.ui.components.MeloSwipeableItem
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.PlatformAsyncImage

@Composable
fun LikedSongsTabContent(
    songs: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        MeloEmptyState(message = "Aún no tienes canciones en tus 'Me Gusta'.", modifier = modifier)
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(songs) { track ->
            MeloSwipeableItem(
                onSwipeLeft = { onSwipeLeft(track) },
                onSwipeRight = { onSwipeRight(track) },
                swipeRightIcon = Icons.Default.HeartBroken
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            track.title,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MeloColors.textSecondary
                        )
                    },
                    leadingContent = {
                        PlatformAsyncImage(
                            url = track.artworkUrl,
                            contentDescription = track.title,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                            size = 48.dp,
                            shape = RoundedCornerShape(6.dp)
                        )
                    },
                    trailingContent = {
                        Row {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp).align(Alignment.CenterVertically)
                            )
                            IconButton(onClick = { onMoreClick(track) }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                            }
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPlayTrack(track) }
                )
            }
        }
    }
}
