@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownloadOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_delete_download
import com.github.adriianh.melo.resources.a11y_downloaded
import com.github.adriianh.melo.resources.a11y_more_options
import com.github.adriianh.melo.resources.a11y_play_all
import com.github.adriianh.melo.resources.library_downloads_albums_empty
import com.github.adriianh.melo.resources.library_downloads_artists_empty
import com.github.adriianh.melo.resources.library_downloads_background_count
import com.github.adriianh.melo.resources.library_downloads_count
import com.github.adriianh.melo.resources.library_downloads_downloading_count
import com.github.adriianh.melo.resources.library_downloads_empty_subtitle
import com.github.adriianh.melo.resources.library_downloads_empty_title
import com.github.adriianh.melo.resources.library_downloads_pending_hint
import com.github.adriianh.melo.resources.library_downloads_space_used
import com.github.adriianh.melo.resources.library_downloads_various_artists
import com.github.adriianh.melo.resources.playlist_track_count
import com.github.adriianh.melo.resources.section_albums
import com.github.adriianh.melo.resources.section_artists
import com.github.adriianh.melo.resources.section_songs
import com.github.adriianh.melo.ui.components.cards.AlbumCard
import com.github.adriianh.melo.ui.components.cards.ArtistCircle
import com.github.adriianh.melo.ui.components.controls.LocalSwipeRevealGroup
import com.github.adriianh.melo.ui.components.controls.MeloSwipeActions
import com.github.adriianh.melo.ui.components.controls.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.controls.rememberSwipeRevealGroup
import com.github.adriianh.melo.ui.components.feedback.MeloEmptyState
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.ui.components.track.SongMenuAnchorRegistry
import com.github.adriianh.melo.ui.components.track.songMenuRightClick
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.autoScrollOnNewItem
import com.github.adriianh.melo.util.desktopScroll
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private enum class DownloadsFilter(
    val labelRes: StringResource,
) {
    SONGS(Res.string.section_songs),
    ALBUMS(Res.string.section_albums),
    ARTISTS(Res.string.section_artists),
}

@Composable
fun DownloadsTabContent(
    downloadedTracks: List<OfflineTrack>,
    onPlayTrack: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onDeleteDownload: (String) -> Unit,
    onMoreClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
    isLiked: (Track) -> Boolean = { false },
    onSwipeLeft: (Track) -> Unit = {},
    onSwipeRight: (Track) -> Unit = {},
    activeDownloads: Map<String, Float> = emptyMap(),
    onAlbumClick: (id: String, title: String, artwork: String?, author: String) -> Unit = { _, _, _, _ -> },
    onArtistClick: (id: String, name: String, artwork: String?) -> Unit = { _, _, _ -> },
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (downloadedTracks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                if (activeDownloads.isNotEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text =
                            pluralStringResource(
                                Res.plurals.library_downloads_downloading_count,
                                activeDownloads.size,
                                activeDownloads.size,
                            ),
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.library_downloads_pending_hint),
                        style = MeloType.body,
                        color = MeloColors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownloadOff,
                        contentDescription = null,
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(56.dp),
                    )
                    Text(
                        text = stringResource(Res.string.library_downloads_empty_title),
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.library_downloads_empty_subtitle),
                        style = MeloType.body,
                        color = MeloColors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        return
    }

    var selectedFilter by remember { mutableStateOf(DownloadsFilter.SONGS) }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val totalSizeMb = downloadedTracks.sumOf { it.fileSize } / (1024.0 * 1024.0)

    listState.autoScrollOnNewItem(downloadedTracks) { "${it.track.id}_${it.track.artist}" }

    val variousArtistsLabel = stringResource(Res.string.library_downloads_various_artists)
    val downloadedAlbums =
        remember(downloadedTracks) {
            downloadedTracks
                .filter {
                    val album = it.track.album.trim()
                    album.isNotBlank() &&
                        !album.equals("Varios", ignoreCase = true) &&
                        !album.equals("Various Artists", ignoreCase = true) &&
                        !album.equals("Desconocido", ignoreCase = true) &&
                        !album.equals("Unknown", ignoreCase = true)
                }.groupBy { it.track.album.trim() }
                .map { (albumName, tracks) ->
                    DownloadedAlbumGroup(
                        name = albumName,
                        artist =
                            tracks
                                .firstOrNull()
                                ?.track
                                ?.artist
                                ?.takeIf { it.isNotBlank() }
                                ?: variousArtistsLabel,
                        artworkUrl = tracks.firstOrNull()?.track?.artworkUrl,
                        tracks = tracks.map { it.track },
                    )
                }
        }

    val downloadedArtists =
        remember(downloadedTracks) {
            downloadedTracks
                .filter {
                    val artist = it.track.artist.trim()
                    artist.isNotBlank() &&
                        !artist.equals("Desconocido", ignoreCase = true) &&
                        !artist.equals("Unknown", ignoreCase = true)
                }.groupBy { it.track.artist.trim() }
                .map { (artistName, tracks) ->
                    DownloadedArtistGroup(
                        name = artistName,
                        artworkUrl = tracks.firstOrNull()?.track?.artworkUrl,
                        tracks = tracks.map { it.track },
                    )
                }
        }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DownloadsFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        val count =
                            when (filter) {
                                DownloadsFilter.SONGS -> downloadedTracks.size
                                DownloadsFilter.ALBUMS -> downloadedAlbums.size
                                DownloadsFilter.ARTISTS -> downloadedArtists.size
                            }
                        Text("${stringResource(filter.labelRes)} ($count)")
                    },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            containerColor = MeloColors.surface1,
                            labelColor = MeloColors.textSecondary,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                    border =
                        FilterChipDefaults.filterChipBorder(
                            borderColor = MeloColors.border,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            enabled = true,
                            selected = isSelected,
                        ),
                    shape = RoundedCornerShape(20.dp),
                )
            }
        }

        when (selectedFilter) {
            DownloadsFilter.SONGS -> {
                val revealGroup = rememberSwipeRevealGroup()
                CompositionLocalProvider(LocalSwipeRevealGroup provides revealGroup) {
                    LazyColumn(
                        state = listState,
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize().desktopScroll(listState),
                    ) {
                        item {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text =
                                                pluralStringResource(
                                                    Res.plurals.library_downloads_count,
                                                    downloadedTracks.size,
                                                    downloadedTracks.size,
                                                ),
                                            style = MeloType.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MeloColors.textPrimary,
                                        )
                                        Text(
                                            text =
                                                stringResource(
                                                    Res.string.library_downloads_space_used,
                                                    formatOneDecimal(totalSizeMb),
                                                ),
                                            style = MeloType.labelSmall,
                                            color = MeloColors.textSecondary,
                                        )
                                    }
                                }

                                if (activeDownloads.isNotEmpty()) {
                                    Surface(
                                        color = MeloColors.surface1,
                                        shape = RoundedCornerShape(10.dp),
                                        border =
                                            BorderStroke(
                                                0.5.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            ),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    ) {
                                        Row(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                            Text(
                                                text =
                                                    pluralStringResource(
                                                        Res.plurals.library_downloads_background_count,
                                                        activeDownloads.size,
                                                        activeDownloads.size,
                                                    ),
                                                style = MeloType.labelMedium,
                                                color = MeloColors.textPrimary,
                                            )
                                        }
                                    }
                                }

                                if (downloadedTracks.isNotEmpty()) {
                                    Button(
                                        onClick = onPlayAll,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(stringResource(Res.string.a11y_play_all))
                                    }
                                }
                            }
                        }

                        items(downloadedTracks, key = { it.track.id }) { offlineTrack ->
                            val track = offlineTrack.track
                            MeloSwipeableItem(
                                onSwipeRight = { onSwipeRight(track) },
                                trailingActions =
                                    listOf(
                                        MeloSwipeActions.queue() to { onSwipeLeft(track) },
                                        MeloSwipeActions.remove() to { onDeleteDownload(track.id) },
                                    ),
                                leadingAction = MeloSwipeActions.like(isLiked(track)),
                                cornerRadius = 12,
                            ) {
                                DownloadedTrackRow(
                                    offlineTrack = offlineTrack,
                                    onClick = { onPlayTrack(track) },
                                    onDelete = { onDeleteDownload(track.id) },
                                    onMoreClick = { onMoreClick(track) },
                                )
                            }
                        }
                    }
                }
            }

            DownloadsFilter.ALBUMS -> {
                if (downloadedAlbums.isEmpty()) {
                    MeloEmptyState(
                        message = stringResource(Res.string.library_downloads_albums_empty),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(140.dp),
                        state = gridState,
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(downloadedAlbums, key = { it.name }) { album ->
                            val trackCountLabel =
                                pluralStringResource(
                                    Res.plurals.playlist_track_count,
                                    album.tracks.size,
                                    album.tracks.size,
                                )
                            AlbumCard(
                                title = album.name,
                                subtitle = "${album.artist} • $trackCountLabel",
                                artworkUrl = album.artworkUrl,
                                cardWidth = 140.dp,
                                onClick = {
                                    onAlbumClick(
                                        album.name,
                                        album.name,
                                        album.artworkUrl,
                                        album.artist,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            DownloadsFilter.ARTISTS -> {
                if (downloadedArtists.isEmpty()) {
                    MeloEmptyState(
                        message = stringResource(Res.string.library_downloads_artists_empty),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(140.dp),
                        state = gridState,
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(downloadedArtists, key = { it.name }) { artist ->
                            ArtistCircle(
                                name = artist.name,
                                artworkUrl = artist.artworkUrl,
                                onClick = { onArtistClick(artist.name, artist.name, artist.artworkUrl) },
                                size = 130.dp,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class DownloadedAlbumGroup(
    val name: String,
    val artist: String,
    val artworkUrl: String?,
    val tracks: List<Track>,
)

private data class DownloadedArtistGroup(
    val name: String,
    val artworkUrl: String?,
    val tracks: List<Track>,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadedTrackRow(
    offlineTrack: OfflineTrack,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val track = offlineTrack.track
    val sizeMb = offlineTrack.fileSize / (1024.0 * 1024.0)
    var rowBounds by remember { mutableStateOf<Rect?>(null) }
    var moreButtonBounds by remember { mutableStateOf<Rect?>(null) }

    Surface(
        color = MeloColors.surface1,
        shape = RoundedCornerShape(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .onGloballyPositioned { rowBounds = it.boundsInWindow() }
                .songMenuRightClick(
                    onOpenMenu = { anchor ->
                        SongMenuAnchorRegistry.capture(anchor)
                        onMoreClick()
                    },
                ).combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        SongMenuAnchorRegistry.capture(rowBounds?.let { SongMenuAnchor.Bounds(it) })
                        onMoreClick()
                    },
                ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MeloAsyncImage(
                url = track.artworkUrl,
                contentDescription = track.title,
                size = 48.dp,
                shape = RoundedCornerShape(8.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MeloType.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MeloColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = stringResource(Res.string.a11y_downloaded),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "${track.artist} • ${formatOneDecimal(sizeMb)} MB",
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(Res.string.a11y_delete_download),
                    tint = MeloColors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }

            IconButton(
                onClick = {
                    SongMenuAnchorRegistry.capture(moreButtonBounds?.let { SongMenuAnchor.Bounds(it) })
                    onMoreClick()
                },
                modifier = Modifier.onGloballyPositioned { moreButtonBounds = it.boundsInWindow() },
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(Res.string.a11y_more_options),
                    tint = MeloColors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun formatOneDecimal(value: Double): String {
    val tenths =
        kotlin.math
            .round(value * 10.0)
            .toLong()
            .coerceAtLeast(0L)
    return "${tenths / 10L}.${tenths % 10L}"
}
