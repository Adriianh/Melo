package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.AdaptiveLazyRow
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.detail.components.ExpandableDescriptionCard
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NowPlayingArtistSection(
    state: PlayerUiState,
    artistDetails: SearchResult.Artist?,
    activeAccent: Color,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    queueViewModel: QueueViewModel = koinViewModel()
) {
    if (artistDetails == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = activeAccent)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artistDetails.id) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MeloAsyncImage(
                    url = artistDetails.artworkUrl,
                    contentDescription = artistDetails.name,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MeloColors.borderStrong, CircleShape),
                    size = 110.dp,
                    shape = CircleShape
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = artistDetails.name,
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    textAlign = TextAlign.Center
                )

                val subtitle = listOfNotNull(
                    artistDetails.subscriberCountText,
                    artistDetails.monthlyListenerCount
                ).joinToString(" · ")

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = activeAccent.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, activeAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { onArtistClick(artistDetails.id) }
                ) {
                    Text(
                        "Ver perfil completo",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        style = MeloType.labelMedium,
                        color = activeAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        val bio = artistDetails.description
        if (!bio.isNullOrBlank()) {
            item {
                ExpandableDescriptionCard(
                    title = "Biografía",
                    description = bio,
                    accentColor = activeAccent
                )
            }
        }

        artistDetails.topSongs?.takeIf { it.isNotEmpty() }?.let { topSongs ->
            item {
                Text(
                    "Canciones populares",
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            itemsIndexed(topSongs) { index, track ->
                val isPlayingThis = state.title == track.title && state.artist == track.artist
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isPlayingThis) activeAccent.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                ) {
                    TrackRow(
                        track = track,
                        trackNumber = index + 1,
                        isCurrent = isPlayingThis,
                        isPlaying = state.isPlaying,
                        onClick = { queueViewModel.playTrack(track) }
                    )
                }
            }
        }

        for (section in artistDetails.sections) {
            if (section.items.isEmpty()) continue

            item(key = "section_${section.title}") {
                SectionHeader(title = section.title)
                AdaptiveLazyRow(
                    items = section.items,
                    minCardWidth = 120.dp,
                    spacing = 10.dp
                ) { item, cardWidth ->
                    when (item) {
                        is SearchResult.Album -> AlbumCard(
                            title = item.title,
                            subtitle = item.author,
                            artworkUrl = item.artworkUrl,
                            cardWidth = cardWidth,
                            onClick = { onAlbumClick(item.id) }
                        )

                        is SearchResult.Artist -> ArtistCircle(
                            name = item.name,
                            artworkUrl = item.artworkUrl,
                            onClick = { onArtistClick(item.id) },
                            size = cardWidth
                        )

                        is SearchResult.Song -> TrackRow(
                            track = item.track,
                            onClick = { queueViewModel.playTrack(item.track) },
                            modifier = Modifier.width(280.dp)
                        )

                        is SearchResult.Playlist -> AlbumCard(
                            title = item.title,
                            subtitle = item.author,
                            artworkUrl = item.artworkUrl,
                            cardWidth = cardWidth,
                            onClick = { onPlaylistClick(item.id) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}