package com.github.adriianh.melo.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.ui.components.SegmentedControl
import com.github.adriianh.melo.ui.player.components.DockedArtistContent
import com.github.adriianh.melo.ui.player.components.DockedLyricsContent
import com.github.adriianh.melo.ui.player.components.DockedQueueContent
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
            MeloColors.brandAccent
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
        DockedPaneHeader(
            onExpandToFullscreen = onExpandToFullscreen,
            onClose = onClose
        )

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

        DockedPaneTrackInfo(
            state = state,
            activeAccent = activeAccent,
            onArtistClick = onArtistClick,
            artistId = artistDetails?.id,
            onStartRadio = { state.currentTrack?.let(queueViewModel::startRadio) },
            onToggleFavorite = { viewModel.toggleFavorite() }
        )

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
                PanelSection.QUEUE -> DockedQueueContent(state, activeAccent, queueViewModel)
                PanelSection.LYRICS -> DockedLyricsContent(state, activeAccent, viewModel)
                PanelSection.ARTIST -> DockedArtistContent(
                    state = state,
                    artistDetails = artistDetails,
                    activeAccent = activeAccent,
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    queueViewModel = queueViewModel
                )
            }
        }
    }
}

@Composable
private fun DockedPaneHeader(
    onExpandToFullscreen: () -> Unit,
    onClose: () -> Unit,
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
}

@Composable
private fun DockedPaneTrackInfo(
    state: PlayerUiState,
    activeAccent: Color,
    onArtistClick: (String) -> Unit,
    artistId: String?,
    onStartRadio: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
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
                    artistId?.let(onArtistClick)
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onStartRadio,
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
                onClick = onToggleFavorite,
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
}