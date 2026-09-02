package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DockedArtistContent(
    state: PlayerUiState,
    artistDetails: SearchResult.Artist?,
    activeAccent: Color,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    queueViewModel: QueueViewModel = koinViewModel()
) {
    if (artistDetails == null) {
        DockedArtistLoading(state, activeAccent)
        return
    }

    val topSongs = remember(artistDetails) {
        artistDetails.topSongs?.take(4) ?: emptyList()
    }

    val albums = remember(artistDetails) {
        val albumsFromSections = artistDetails.sections
            .filter { section ->
                section.title.contains("álbum", ignoreCase = true) ||
                        section.title.contains("album", ignoreCase = true) ||
                        section.title.contains("disc", ignoreCase = true) ||
                        section.title.contains("lanzamiento", ignoreCase = true) ||
                        section.title.contains("release", ignoreCase = true)
            }
            .flatMap { it.items }
            .filterIsInstance<SearchResult.Album>()
            .ifEmpty {
                artistDetails.sections.flatMap { it.items }.filterIsInstance<SearchResult.Album>()
            }
            .distinctBy { it.id }
            .take(4)
        albumsFromSections
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DockedArtistHeader(
                artistDetails = artistDetails,
                activeAccent = activeAccent,
                onArtistClick = onArtistClick
            )
        }

        if (topSongs.isNotEmpty()) {
            item {
                Text(
                    "Populares",
                    style = MeloType.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = activeAccent,
                    letterSpacing = 1.sp
                )
            }
            items(topSongs) { track ->
                val isPlayingThis = state.title == track.title && state.artist == track.artist
                DockedArtistTopSongRow(
                    track = track,
                    isPlayingThis = isPlayingThis,
                    activeAccent = activeAccent,
                    onClick = { queueViewModel.playTrack(track) }
                )
            }
        }

        if (albums.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Álbumes y lanzamientos",
                    style = MeloType.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textMuted,
                    letterSpacing = 1.sp
                )
            }
            items(albums) { album ->
                DockedArtistAlbumRow(
                    album = album,
                    onClick = { onAlbumClick(album.id) }
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MeloColors.chromePillFill,
                border = BorderStroke(0.5.dp, MeloColors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artistDetails.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ver perfil completo",
                        style = MeloType.labelSmall,
                        color = activeAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}

@Composable
private fun DockedArtistLoading(
    state: PlayerUiState,
    activeAccent: Color,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ArtistCircle(
                name = state.artist,
                artworkUrl = state.albumArt,
                onClick = { },
                size = 68.dp
            )
            CircularProgressIndicator(
                color = activeAccent,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun DockedArtistHeader(
    artistDetails: SearchResult.Artist,
    activeAccent: Color,
    onArtistClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onArtistClick(artistDetails.id) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MeloAsyncImage(
            url = artistDetails.artworkUrl,
            contentDescription = artistDetails.name,
            modifier = Modifier
                .size(68.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .border(1.5.dp, MeloColors.borderStrong, CircleShape),
            size = 68.dp,
            shape = CircleShape
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artistDetails.name,
            style = MeloType.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MeloColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        val subtitle = listOfNotNull(
            artistDetails.subscriberCountText,
            artistDetails.monthlyListenerCount
        ).firstOrNull { it.isNotBlank() }

        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = activeAccent.copy(alpha = 0.12f),
            border = BorderStroke(0.5.dp, activeAccent.copy(alpha = 0.35f)),
            modifier = Modifier.clickable { onArtistClick(artistDetails.id) }
        ) {
            Text(
                "Ver perfil",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                style = MeloType.labelSmall.copy(fontSize = 11.sp),
                color = activeAccent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DockedArtistTopSongRow(
    track: Track,
    isPlayingThis: Boolean,
    activeAccent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isPlayingThis) activeAccent.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
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
                fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                color = if (isPlayingThis) activeAccent else MeloColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                style = MeloType.labelSmall.copy(fontSize = 10.sp),
                color = MeloColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DockedArtistAlbumRow(
    album: SearchResult.Album,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MeloAsyncImage(
            url = album.artworkUrl,
            contentDescription = album.title,
            size = 44.dp,
            shape = RoundedCornerShape(6.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                album.title,
                style = MeloType.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MeloColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val yearOrAuthor =
                listOfNotNull(album.year, album.author).firstOrNull { it.isNotBlank() }
            if (!yearOrAuthor.isNullOrBlank()) {
                Text(
                    yearOrAuthor,
                    style = MeloType.labelSmall.copy(fontSize = 11.sp),
                    color = MeloColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}