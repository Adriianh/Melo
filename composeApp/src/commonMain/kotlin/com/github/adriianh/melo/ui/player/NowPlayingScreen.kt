package com.github.adriianh.melo.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.GlassPanel
import com.github.adriianh.melo.ui.components.SegmentedControl
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.PlayerUiState
import com.github.adriianh.melo.util.getPlatform
import org.koin.compose.viewmodel.koinViewModel

enum class PanelSection {
    QUEUE,
    LYRICS,
    ARTIST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val platform = remember { getPlatform() }
    var selectedSection by remember { mutableStateOf(PanelSection.QUEUE) }
    var isFavorite by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        NowPlayingBackground(artworkUrl = state.albumArt)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = MeloColors.textPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "REPRODUCIENDO",
                        style = MeloType.labelSmall,
                        letterSpacing = 1.5.sp,
                        color = MeloColors.textMuted
                    )
                    Text(
                        text = state.title.ifEmpty { "Melo" },
                        style = MeloType.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { /* options */ }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MeloColors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (platform.type == PlatformType.DESKTOP) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        MeloAsyncImage(
                            url = state.albumArt,
                            contentDescription = state.title,
                            modifier = Modifier
                                .size(280.dp)
                                .shadow(24.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp)),
                            size = 280.dp,
                            shape = RoundedCornerShape(20.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.title,
                                    style = MeloType.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MeloColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = state.artist,
                                    style = MeloType.titleMedium,
                                    color = MeloColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { isFavorite = !isFavorite }) {
                                Icon(
                                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MeloColors.textMuted,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        PlayerSlider(
                            state = state,
                            onSeekTo = viewModel::seekTo,
                            modifier = Modifier.widthIn(max = 380.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        PlayerTransportControls(state = state, viewModel = viewModel)
                    }

                    GlassPanel(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SegmentedControl(
                                options = PanelSection.entries,
                                selected = selectedSection,
                                onSelect = { selectedSection = it },
                                label = {
                                    when (it) {
                                        PanelSection.QUEUE -> "Cola"
                                        PanelSection.LYRICS -> "Letra"
                                        PanelSection.ARTIST -> "Artista"
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                when (selectedSection) {
                                    PanelSection.QUEUE -> NowPlayingQueueSection(state)
                                    PanelSection.LYRICS -> NowPlayingLyricsSection()
                                    PanelSection.ARTIST -> NowPlayingArtistSection(state)
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    MeloAsyncImage(
                        url = state.albumArt,
                        contentDescription = state.title,
                        modifier = Modifier
                            .size(260.dp)
                            .shadow(20.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp)),
                        size = 260.dp,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.title,
                                style = MeloType.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MeloColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = state.artist,
                                style = MeloType.titleMedium,
                                color = MeloColors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { isFavorite = !isFavorite }) {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MeloColors.textMuted,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    PlayerSlider(state = state, onSeekTo = viewModel::seekTo)

                    PlayerTransportControls(state = state, viewModel = viewModel)

                    Spacer(modifier = Modifier.height(4.dp))

                    GlassPanel(
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SegmentedControl(
                                options = PanelSection.entries,
                                selected = selectedSection,
                                onSelect = { selectedSection = it },
                                label = {
                                    when (it) {
                                        PanelSection.QUEUE -> "Cola"
                                        PanelSection.LYRICS -> "Letra"
                                        PanelSection.ARTIST -> "Artista"
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                when (selectedSection) {
                                    PanelSection.QUEUE -> NowPlayingQueueSection(state)
                                    PanelSection.LYRICS -> NowPlayingLyricsSection()
                                    PanelSection.ARTIST -> NowPlayingArtistSection(state)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSlider(
    state: PlayerUiState,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = state.progressFraction,
            onValueChange = { fraction ->
                if (state.durationMs > 0) {
                    onSeekTo((fraction * state.durationMs).toLong())
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = state.accentColor,
                activeTrackColor = state.accentColor,
                inactiveTrackColor = MeloColors.borderStrong
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = state.elapsedLabel,
                style = MeloType.labelSmall,
                color = MeloColors.textMuted
            )
            Text(
                text = state.remainingLabel,
                style = MeloType.labelSmall,
                color = MeloColors.textMuted
            )
        }
    }
}

@Composable
private fun PlayerTransportControls(
    state: PlayerUiState,
    viewModel: PlayerViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = viewModel::toggleShuffle) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MeloColors.textMuted,
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(onClick = viewModel::playPrevious, enabled = state.hasPrevious) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = if (state.hasPrevious) MeloColors.textPrimary else MeloColors.textMuted.copy(
                    alpha = 0.4f
                ),
                modifier = Modifier.size(36.dp)
            )
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp).clickable(onClick = viewModel::togglePlayPause)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        IconButton(onClick = viewModel::playNext, enabled = state.hasNext) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = if (state.hasNext) MeloColors.textPrimary else MeloColors.textMuted.copy(
                    alpha = 0.4f
                ),
                modifier = Modifier.size(36.dp)
            )
        }

        IconButton(onClick = viewModel::toggleRepeat) {
            val icon = when (state.repeatMode) {
                RepeatMode.ONE -> Icons.Default.RepeatOne
                else -> Icons.Default.Repeat
            }
            val tint = when (state.repeatMode) {
                RepeatMode.NONE -> MeloColors.textMuted
                else -> MaterialTheme.colorScheme.primary
            }
            Icon(icon, contentDescription = "Repeat", tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun NowPlayingQueueSection(
    state: PlayerUiState,
    queueViewModel: QueueViewModel = koinViewModel()
) {
    val queueState by queueViewModel.queueState.collectAsState()
    val suggestions by queueViewModel.suggestions.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "REPRODUCIENDO AHORA",
                style = MeloType.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MeloAsyncImage(
                    url = state.albumArt,
                    contentDescription = state.title,
                    size = 38.dp,
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
                    "A CONTINUACIÓN",
                    style = MeloType.labelSmall,
                    letterSpacing = 1.2.sp,
                    color = MeloColors.textMuted
                )
            }

            items(upNextTracks) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { queueViewModel.playTrackInQueue(track) }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MeloAsyncImage(
                        url = track.artworkUrl,
                        contentDescription = track.title,
                        size = 32.dp,
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
                        Text(
                            track.artist,
                            style = MeloType.labelSmall,
                            color = MeloColors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "SUGERENCIAS",
                    style = MeloType.labelSmall,
                    letterSpacing = 1.2.sp,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MeloAsyncImage(
                        url = track.artworkUrl,
                        contentDescription = track.title,
                        size = 32.dp,
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
                        Text(
                            track.artist,
                            style = MeloType.labelSmall,
                            color = MeloColors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { queueViewModel.addToQueue(track) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.Add,
                            contentDescription = "Añadir a la cola",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingLyricsSection() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Letras sincronizadas disponibles próximamente",
            style = MeloType.body,
            color = MeloColors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NowPlayingArtistSection(state: PlayerUiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ArtistCircle(
            name = state.artist,
            artworkUrl = state.albumArt,
            onClick = {},
            size = 80.dp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Artista verificado",
            style = MeloType.labelSmall,
            color = MeloColors.textMuted
        )
    }
}