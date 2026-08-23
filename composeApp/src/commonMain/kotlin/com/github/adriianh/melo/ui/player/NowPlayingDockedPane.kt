package com.github.adriianh.melo.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.SegmentedControl
import com.github.adriianh.melo.ui.components.SuggestionSkeletonCard
import com.github.adriianh.melo.ui.components.SuggestionTrackCard
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DesktopNowPlayingDockedPane(
    onExpandToFullscreen: () -> Unit,
    onClose: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    selectedSection: PanelSection = PanelSection.QUEUE,
    onSectionChange: (PanelSection) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val artistDetails by viewModel.artistDetails.collectAsState()
    val activeAccent =
        if (state.accentColor != Color.Transparent && state.accentColor != MeloColors.textMuted) {
            state.accentColor
        } else {
            MaterialTheme.colorScheme.primary
        }

    if (!state.hasTrack) return

    Column(
        modifier = modifier
            .width(290.dp)
            .fillMaxHeight()
            .padding(top = 12.dp, bottom = 12.dp, end = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MeloColors.glassSurface)
            .border(0.5.dp, MeloColors.glassBorder, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REPRODUCIENDO",
                style = MeloType.labelSmall,
                letterSpacing = 1.2.sp,
                color = MeloColors.textMuted
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExpandToFullscreen, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.OpenInFull,
                        contentDescription = "Expandir pantalla completa",
                        tint = MeloColors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar panel",
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        MeloAsyncImage(
            url = state.albumArt,
            contentDescription = state.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .shadow(10.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            size = 160.dp,
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.artist,
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable {
                        artistDetails?.id?.let(onArtistClick)
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val track = state.currentTrack
                        if (track != null) {
                            queueViewModel.startRadio(track)
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = "Iniciar Radio",
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleFavorite() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (state.isFavorite) activeAccent else MeloColors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        SegmentedControl(
            options = PanelSection.entries,
            selected = selectedSection,
            onSelect = onSectionChange,
            label = {
                when (it) {
                    PanelSection.QUEUE -> "Cola"
                    PanelSection.LYRICS -> "Letra"
                    PanelSection.ARTIST -> "Artista"
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedSection) {
                PanelSection.QUEUE -> DockedQueueContent(state, activeAccent)
                PanelSection.LYRICS -> DockedLyricsContent()
                PanelSection.ARTIST -> DockedArtistContent(
                    state = state,
                    artistDetails = artistDetails,
                    activeAccent = activeAccent,
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick
                )
            }
        }
    }
}

@Composable
private fun DockedQueueContent(
    state: PlayerUiState,
    activeAccent: Color,
    queueViewModel: QueueViewModel = koinViewModel()
) {
    val queueState by queueViewModel.queueState.collectAsState()
    val suggestions by queueViewModel.suggestions.collectAsState()
    val isLoadingSuggestions by queueViewModel.isLoadingSuggestions.collectAsState()
    var isQueueExpanded by remember { mutableStateOf(false) }

    val upNextTracks = remember(queueState.tracks, queueState.currentIndex) {
        if (queueState.currentIndex >= 0 && queueState.currentIndex < queueState.tracks.size) {
            queueState.tracks.drop(queueState.currentIndex + 1)
        } else {
            emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                "Reproduciendo ahora",
                style = MeloType.labelSmall,
                fontWeight = FontWeight.Bold,
                color = activeAccent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(activeAccent.copy(alpha = 0.15f))
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
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "A continuación",
                        style = MeloType.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MeloColors.textSecondary
                    )
                    if (upNextTracks.size > 1) {
                        Text(
                            text = if (isQueueExpanded) "Contraer" else "+${upNextTracks.size - 1} en cola",
                            style = MeloType.labelSmall.copy(fontSize = 11.sp),
                            color = activeAccent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { isQueueExpanded = !isQueueExpanded }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            val visibleQueueTracks = if (isQueueExpanded) upNextTracks else upNextTracks.take(1)
            itemsIndexed(
                visibleQueueTracks,
                key = { index, track -> "docked_q_${index}_${track.id}" }) { _, track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MeloColors.surface1.copy(alpha = 0.5f))
                        .clickable { queueViewModel.playTrackInQueue(track) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
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
                            fontWeight = FontWeight.Medium,
                            color = MeloColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            track.artist,
                            style = MeloType.labelSmall.copy(fontSize = 10.sp),
                            color = MeloColors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { queueViewModel.removeTrackFromQueue(track) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Quitar de la cola",
                            tint = MeloColors.textMuted,
                            modifier = Modifier.size(14.dp)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Sugerencias para ti",
                        style = MeloType.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textSecondary
                    )
                    if (isLoadingSuggestions) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = activeAccent
                        )
                    }
                }
                IconButton(
                    onClick = { queueViewModel.refreshSuggestions() },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refrescar sugerencias",
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(14.dp)
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
                key = { index, track -> "docked_sugg_${index}_${track.id}" }) { _, track ->
                SuggestionTrackCard(
                    track = track,
                    activeAccent = activeAccent,
                    isCurrent = state.currentTrack?.id == track.id,
                    isPlaying = state.isPlaying && state.currentTrack?.id == track.id,
                    onClick = { queueViewModel.playTrack(track) },
                    onAddClick = { queueViewModel.insertTrackNext(track) }
                )
            }
        }
    }
}

@Composable
private fun DockedLyricsContent() {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Letras sincronizadas disponibles próximamente",
            style = MeloType.body,
            color = MeloColors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DockedArtistContent(
    state: PlayerUiState,
    artistDetails: SearchResult.Artist?,
    activeAccent: Color,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    queueViewModel: QueueViewModel = koinViewModel()
) {
    if (artistDetails == null) {
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

        if (topSongs.isNotEmpty()) {
            item {
                Text(
                    "Populares",
                    style = MeloType.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textMuted,
                    letterSpacing = 1.sp
                )
            }
            items(topSongs) { track ->
                val isPlayingThis = state.title == track.title && state.artist == track.artist
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isPlayingThis) activeAccent.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable { queueViewModel.playTrack(track) }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAlbumClick(album.id) }
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