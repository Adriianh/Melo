package com.github.adriianh.melo.ui.library.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
fun AlbumsTabContent(
    albums: List<SearchResult.Album>,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewMode: LibraryViewMode = LibraryViewMode.GRID,
    isOffline: Boolean = false,
    offlineAlbumIds: Set<String> = emptySet(),
) {
    if (albums.isEmpty()) {
        MeloEmptyState(message = "No tienes álbumes guardados.", modifier = modifier)
        return
    }

    if (viewMode == LibraryViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(albums, key = { it.id }) { album ->
                val isAvailable = !isOffline || album.id in offlineAlbumIds
                val hasDownloaded = isOffline && album.id in offlineAlbumIds

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .alpha(if (isAvailable) 1f else 0.45f)
                            .clickable(enabled = isAvailable) { onAlbumClick(album.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MeloColors.surface1),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                        ) {
                            PlatformAsyncImage(
                                url = album.artworkUrl,
                                contentDescription = album.title,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                size = 130.dp,
                                shape = RoundedCornerShape(8.dp),
                            )
                            if (hasDownloaded) {
                                Box(
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .background(
                                                MeloColors.surface0.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(6.dp),
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
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = album.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MeloColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(albums, key = { "compact_${it.id}" }) { album ->
                val isAvailable = !isOffline || album.id in offlineAlbumIds
                val hasDownloaded = isOffline && album.id in offlineAlbumIds

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .alpha(if (isAvailable) 1f else 0.45f)
                            .background(MeloColors.surface1)
                            .clickable(enabled = isAvailable) { onAlbumClick(album.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(modifier = Modifier.size(56.dp)) {
                        PlatformAsyncImage(
                            url = album.artworkUrl,
                            contentDescription = album.title,
                            modifier =
                                Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            size = 56.dp,
                            shape = RoundedCornerShape(10.dp),
                        )
                        if (hasDownloaded) {
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .background(
                                            MeloColors.surface0.copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(4.dp),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = album.title,
                            style = MeloType.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MeloColors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val albumSubtitle =
                            buildString {
                                append(album.author)
                                if (!album.year.isNullOrBlank()) {
                                    append(" • ")
                                    append(album.year)
                                }
                            }
                        Text(
                            text = albumSubtitle,
                            style = MeloType.labelSmall,
                            color = MeloColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val desc = album.description
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
        }
    }
}
