package com.github.adriianh.melo.ui.library.components

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.MeloEmptyState
import com.github.adriianh.melo.ui.library.LibraryViewMode
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformAsyncImage

@Composable
fun ArtistsTabContent(
    artists: List<SearchResult.Artist>,
    onArtistClick: (id: String, name: String, artwork: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewMode: LibraryViewMode = LibraryViewMode.GRID,
    isOffline: Boolean = false,
    offlineArtistIds: Set<String> = emptySet(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (artists.isEmpty()) {
        MeloEmptyState(message = "No sigues a ningún artista actualmente.", modifier = modifier)
        return
    }

    if (viewMode == LibraryViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(130.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = contentPadding,
            modifier = modifier.fillMaxSize(),
        ) {
            items(artists, key = { it.id }) { artist ->
                val isAvailable = !isOffline || artist.id in offlineArtistIds
                val hasDownloaded = isOffline && artist.id in offlineArtistIds
                artistGridItem(
                    artist = artist,
                    isAvailable = isAvailable,
                    hasDownloaded = hasDownloaded,
                    onClick = { onArtistClick(artist.id, artist.name, artist.artworkUrl) },
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = contentPadding,
            modifier = modifier.fillMaxSize(),
        ) {
            items(artists, key = { "compact_${it.id}" }) { artist ->
                val isAvailable = !isOffline || artist.id in offlineArtistIds
                val hasDownloaded = isOffline && artist.id in offlineArtistIds
                artistCompactItem(
                    artist = artist,
                    isAvailable = isAvailable,
                    hasDownloaded = hasDownloaded,
                    onClick = { onArtistClick(artist.id, artist.name, artist.artworkUrl) },
                )
            }
        }
    }
}

@Composable
private fun artistGridItem(
    artist: SearchResult.Artist,
    isAvailable: Boolean,
    hasDownloaded: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .alpha(if (isAvailable) 1f else 0.45f)
                .clickable(enabled = isAvailable, onClick = onClick)
                .padding(4.dp),
    ) {
        artistGridCover(
            url = artist.artworkUrl,
            name = artist.name,
            hasDownloaded = hasDownloaded,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MeloType.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MeloColors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun artistGridCover(
    url: String?,
    name: String,
    hasDownloaded: Boolean,
) {
    Box(modifier = Modifier.size(100.dp)) {
        PlatformAsyncImage(
            url = url,
            contentDescription = name,
            modifier = Modifier.size(100.dp).clip(CircleShape),
            size = 100.dp,
            shape = CircleShape,
        )
        if (hasDownloaded) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .background(
                            MeloColors.surface0.copy(alpha = 0.85f),
                            shape = CircleShape,
                        ).padding(3.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = "Disponible sin conexión",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun artistCompactItem(
    artist: SearchResult.Artist,
    isAvailable: Boolean,
    hasDownloaded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .alpha(if (isAvailable) 1f else 0.45f)
                .background(MeloColors.surface1)
                .clickable(enabled = isAvailable, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        artistCompactCover(
            url = artist.artworkUrl,
            name = artist.name,
            hasDownloaded = hasDownloaded,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MeloType.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MeloColors.textPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            val artistSubtitle =
                artist.subscriberCountText?.takeIf { it.isNotBlank() }
                    ?: "En tu biblioteca"
            Text(
                text = artistSubtitle,
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val desc = artist.description
            if (!desc.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MeloType.labelSmall,
                    color = MeloColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun artistCompactCover(
    url: String?,
    name: String,
    hasDownloaded: Boolean,
) {
    Box(modifier = Modifier.size(56.dp)) {
        PlatformAsyncImage(
            url = url,
            contentDescription = name,
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(CircleShape),
            size = 56.dp,
            shape = CircleShape,
        )
        if (hasDownloaded) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .background(
                            MeloColors.surface0.copy(alpha = 0.85f),
                            shape = CircleShape,
                        ).padding(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = "Disponible sin conexión",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
