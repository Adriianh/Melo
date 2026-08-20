package com.github.adriianh.melo.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.SegmentedControl
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DesktopNowPlayingDockedPane(
    onExpandToFullscreen: () -> Unit,
    onClose: () -> Unit,
    selectedSection: PanelSection = PanelSection.QUEUE,
    onSectionChange: (PanelSection) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    if (!state.hasTrack) return

    Column(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .padding(top = 12.dp, bottom = 12.dp, end = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MeloColors.glassFill)
            .border(0.5.dp, MeloColors.glassBorder, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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

            Spacer(modifier = Modifier.height(10.dp))

            MeloAsyncImage(
                url = state.albumArt,
                contentDescription = state.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .shadow(12.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp)),
                size = 200.dp,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleFavorite() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else MeloColors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                when (selectedSection) {
                    PanelSection.QUEUE -> DockedQueueContent(state)
                    PanelSection.LYRICS -> DockedLyricsContent()
                    PanelSection.ARTIST -> DockedArtistContent(state)
                }
            }
        }
    }
}

@Composable
private fun DockedQueueContent(
    state: PlayerUiState,
    queueViewModel: QueueViewModel = koinViewModel()
) {
    val queueState by queueViewModel.queueState.collectAsState()
    val suggestions by queueViewModel.suggestions.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text(
                "Reproduciendo ahora",
                style = MeloType.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MeloAsyncImage(
                    url = state.albumArt,
                    contentDescription = state.title,
                    size = 32.dp,
                    shape = RoundedCornerShape(4.dp)
                )
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

        val upNextTracks = queueState.tracks.drop(queueState.currentIndex + 1)
        if (upNextTracks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "A continuación",
                    style = MeloType.labelSmall,
                    color = MeloColors.textMuted
                )
            }
            items(upNextTracks) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { queueViewModel.playTrackInQueue(track) }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MeloAsyncImage(
                        url = track.artworkUrl,
                        contentDescription = track.title,
                        size = 26.dp,
                        shape = RoundedCornerShape(4.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.title,
                            style = MeloType.labelMedium,
                            color = MeloColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Sugerencias",
                    style = MeloType.labelSmall,
                    color = MeloColors.textMuted
                )
            }
            items(suggestions) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MeloAsyncImage(
                        url = track.artworkUrl,
                        contentDescription = track.title,
                        size = 26.dp,
                        shape = RoundedCornerShape(4.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.title,
                            style = MeloType.labelMedium,
                            color = MeloColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { queueViewModel.addToQueue(track) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Añadir a la cola",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockedLyricsContent() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Letras sincronizadas próximamente",
            style = MeloType.body,
            color = MeloColors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DockedArtistContent(state: PlayerUiState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ArtistCircle(
            name = state.artist,
            artworkUrl = state.albumArt,
            onClick = {},
            size = 64.dp
        )
    }
}