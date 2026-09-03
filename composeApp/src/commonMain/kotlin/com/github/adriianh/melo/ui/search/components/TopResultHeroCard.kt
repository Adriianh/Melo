package com.github.adriianh.melo.ui.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun TopResultHeroCard(
    item: SearchResult,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String, String, String?, String) -> Unit,
    queueViewModel: QueueViewModel,
    modifier: Modifier = Modifier
) {
    val isArtist = item is SearchResult.Artist
    val title = when (item) {
        is SearchResult.Artist -> item.name
        is SearchResult.Song -> item.track.title
        is SearchResult.Album -> item.title
        is SearchResult.Playlist -> item.title
    }
    val subtitle = when (item) {
        is SearchResult.Artist -> "Artista"
        is SearchResult.Song -> item.track.artist
        is SearchResult.Album -> item.author + (item.year?.let { " • $it" } ?: "")
        is SearchResult.Playlist -> item.author
    }
    val artworkUrl = when (item) {
        is SearchResult.Artist -> item.artworkUrl
        is SearchResult.Song -> item.track.artworkUrl
        is SearchResult.Album -> item.artworkUrl
        is SearchResult.Playlist -> item.artworkUrl
    }
    val badgeText = when (item) {
        is SearchResult.Artist -> "ARTISTA"
        is SearchResult.Song -> "CANCIÓN"
        is SearchResult.Album -> "ÁLBUM"
        is SearchResult.Playlist -> "PLAYLIST"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MeloColors.surface2.copy(alpha = 0.85f),
                        MeloColors.surface1.copy(alpha = 0.65f)
                    )
                )
            )
            .border(0.75.dp, MeloColors.glassBorder, RoundedCornerShape(20.dp))
            .clickable {
                when (item) {
                    is SearchResult.Artist -> onArtistClick(item.id)
                    is SearchResult.Song -> queueViewModel.playTrack(item.track)
                    is SearchResult.Album -> onAlbumClick(item.id)
                    is SearchResult.Playlist -> onPlaylistClick(
                        item.id,
                        item.title,
                        item.artworkUrl,
                        item.author
                    )
                }
            }
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MeloType.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isArtist) {
                    MeloAsyncImage(
                        url = artworkUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                CircleShape
                            ),
                        shape = CircleShape
                    )
                } else {
                    MeloAsyncImage(
                        url = artworkUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .shadow(8.dp, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MeloType.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MeloType.body,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        when (item) {
                            is SearchResult.Song -> queueViewModel.playTrack(item.track)
                            is SearchResult.Artist -> onArtistClick(item.id)
                            is SearchResult.Album -> onAlbumClick(item.id)
                            is SearchResult.Playlist -> onPlaylistClick(
                                item.id,
                                item.title,
                                item.artworkUrl,
                                item.author
                            )
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(8.dp, CircleShape)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
