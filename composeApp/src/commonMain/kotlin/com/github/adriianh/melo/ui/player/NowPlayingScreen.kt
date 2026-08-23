package com.github.adriianh.melo.ui.player

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.melo.ui.components.AdaptiveLazyRow
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.GlassPanel
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.components.SegmentedControl
import com.github.adriianh.melo.ui.components.SuggestionSkeletonCard
import com.github.adriianh.melo.ui.components.SuggestionTrackCard
import com.github.adriianh.melo.ui.components.TrackRow
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
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val artistDetails by viewModel.artistDetails.collectAsState()
    val platform = remember { getPlatform() }
    var selectedSection by remember { mutableStateOf(PanelSection.QUEUE) }
    var showLyrics by remember { mutableStateOf(false) }
    var expandedBottomSection by remember { mutableStateOf<PanelSection?>(null) }

    val activeAccent =
        if (state.accentColor != Color.Transparent && state.accentColor != MeloColors.textMuted) {
            state.accentColor
        } else {
            MaterialTheme.colorScheme.primary
        }

    Box(modifier = modifier.fillMaxSize()) {
        NowPlayingBackground(artworkUrl = state.albumArt)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp),
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

            Spacer(modifier = Modifier.height(6.dp))

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
                        Box(
                            modifier = Modifier
                                .size(250.dp)
                                .shadow(20.dp, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Crossfade(
                                targetState = showLyrics,
                                label = "DesktopLyricsCrossfade"
                            ) { isLyrics ->
                                if (isLyrics) {
                                    NowPlayingLyricsCard(
                                        lyrics = state.lyrics,
                                        activeAccent = activeAccent,
                                        onToggleArtwork = { showLyrics = false },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    MeloAsyncImage(
                                        url = state.albumArt,
                                        contentDescription = state.title,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { showLyrics = true },
                                        size = 250.dp,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                }
                            }
                        }

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
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable {
                                        artistDetails?.id?.let { id ->
                                            onCollapse()
                                            onArtistClick(id)
                                        }
                                    }
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { showLyrics = !showLyrics }) {
                                    Icon(
                                        Icons.Outlined.Mic,
                                        contentDescription = "Letra",
                                        tint = if (showLyrics) activeAccent else MeloColors.textMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleFavorite() }) {
                                    Icon(
                                        if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (state.isFavorite) activeAccent else MeloColors.textMuted,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        PlayerSlider(
                            state = state,
                            activeAccent = activeAccent,
                            onSeekTo = viewModel::seekTo,
                            modifier = Modifier.widthIn(max = 380.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        PlayerTransportControls(
                            state = state,
                            activeAccent = activeAccent,
                            viewModel = viewModel
                        )
                    }

                    GlassPanel(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
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
                                    PanelSection.QUEUE -> NowPlayingQueueSection(
                                        state,
                                        activeAccent
                                    )

                                    PanelSection.LYRICS -> NowPlayingLyricsSection(state.lyrics)
                                    PanelSection.ARTIST -> NowPlayingArtistSection(
                                        state = state,
                                        artistDetails = artistDetails,
                                        activeAccent = activeAccent,
                                        onArtistClick = { id ->
                                            onCollapse()
                                            onArtistClick(id)
                                        },
                                        onAlbumClick = { id ->
                                            onCollapse()
                                            onAlbumClick(id)
                                        },
                                        onPlaylistClick = { id ->
                                            onCollapse()
                                            onPlaylistClick(id)
                                        }
                                    )
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
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .shadow(16.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(
                            targetState = showLyrics,
                            label = "MobileLyricsCrossfade"
                        ) { isLyrics ->
                            if (isLyrics) {
                                NowPlayingLyricsCard(
                                    lyrics = state.lyrics,
                                    activeAccent = activeAccent,
                                    onToggleArtwork = { showLyrics = false },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                MeloAsyncImage(
                                    url = state.albumArt,
                                    contentDescription = state.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { showLyrics = true },
                                    size = 230.dp,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
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
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable {
                                    artistDetails?.id?.let { id ->
                                        onCollapse()
                                        onArtistClick(id)
                                    }
                                }
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { showLyrics = !showLyrics }) {
                                Icon(
                                    Icons.Outlined.Mic,
                                    contentDescription = "Letra",
                                    tint = if (showLyrics) activeAccent else MeloColors.textMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.toggleFavorite() }) {
                                Icon(
                                    if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (state.isFavorite) activeAccent else MeloColors.textMuted,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    PlayerSlider(
                        state = state,
                        activeAccent = activeAccent,
                        onSeekTo = viewModel::seekTo
                    )

                    PlayerTransportControls(
                        state = state,
                        activeAccent = activeAccent,
                        viewModel = viewModel
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MeloColors.surface1.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MeloColors.border),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { expandedBottomSection = PanelSection.QUEUE }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = activeAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Cola",
                                    style = MeloType.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MeloColors.textPrimary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MeloColors.surface1.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MeloColors.border),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val track = state.currentTrack
                                    if (track != null) {
                                        queueViewModel.startRadio(track)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = activeAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Radio",
                                    style = MeloType.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MeloColors.textPrimary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MeloColors.surface1.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MeloColors.border),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { expandedBottomSection = PanelSection.ARTIST }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = activeAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Artista",
                                    style = MeloType.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MeloColors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (expandedBottomSection != null) {
            ModalBottomSheet(
                onDismissRequest = { expandedBottomSection = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MeloColors.surface1.copy(alpha = 0.95f),
                contentColor = MeloColors.textPrimary,
                scrimColor = Color.Black.copy(alpha = 0.55f),
                tonalElevation = 12.dp,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MeloColors.textMuted.copy(alpha = 0.4f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                        .fillMaxHeight(0.70f)
                ) {
                    Text(
                        text = when (expandedBottomSection) {
                            PanelSection.QUEUE -> "Cola de reproducción"
                            PanelSection.ARTIST -> "Acerca del Artista"
                            else -> ""
                        },
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    when (expandedBottomSection) {
                        PanelSection.QUEUE -> NowPlayingQueueSection(state, activeAccent)
                        PanelSection.ARTIST -> NowPlayingArtistSection(
                            state = state,
                            artistDetails = artistDetails,
                            activeAccent = activeAccent,
                            onArtistClick = { id ->
                                expandedBottomSection = null
                                onCollapse()
                                onArtistClick(id)
                            },
                            onAlbumClick = { id ->
                                expandedBottomSection = null
                                onCollapse()
                                onAlbumClick(id)
                            },
                            onPlaylistClick = { id ->
                                expandedBottomSection = null
                                onCollapse()
                                onPlaylistClick(id)
                            }
                        )

                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingLyricsCard(
    lyrics: String?,
    activeAccent: Color,
    onToggleArtwork: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MeloColors.surface1.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MeloColors.borderStrong),
        shadowElevation = 16.dp,
        modifier = modifier.clickable(onClick = onToggleArtwork)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                tint = activeAccent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (!lyrics.isNullOrBlank()) lyrics else "Letras sincronizadas disponibles próximamente",
                    style = MeloType.body,
                    fontWeight = FontWeight.Medium,
                    color = MeloColors.textPrimary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Toca para ver la portada",
                style = MeloType.labelSmall.copy(fontSize = 11.sp),
                color = MeloColors.textMuted
            )
        }
    }
}

@Composable
private fun PlayerSlider(
    state: PlayerUiState,
    activeAccent: Color,
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
                thumbColor = activeAccent,
                activeTrackColor = activeAccent,
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
    activeAccent: Color,
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
                tint = if (state.shuffleEnabled) activeAccent else MeloColors.textMuted,
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
            color = activeAccent,
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
                else -> activeAccent
            }
            Icon(icon, contentDescription = "Repeat", tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun NowPlayingQueueSection(
    state: PlayerUiState,
    activeAccent: Color,
    queueViewModel: QueueViewModel = koinViewModel()
) {
    val queueState by queueViewModel.queueState.collectAsState()
    val suggestions by queueViewModel.suggestions.collectAsState()
    val isLoadingSuggestions by queueViewModel.isLoadingSuggestions.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "REPRODUCIENDO AHORA",
                style = MeloType.labelSmall,
                fontWeight = FontWeight.Bold,
                color = activeAccent,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeAccent.copy(alpha = 0.15f))
                    .border(0.5.dp, activeAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MeloAsyncImage(
                    url = state.albumArt,
                    contentDescription = state.title,
                    size = 42.dp,
                    shape = RoundedCornerShape(6.dp)
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

            itemsIndexed(
                upNextTracks,
                key = { index, track -> "screen_q_${index}_${track.id}" }) { _, track ->
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
                        size = 36.dp,
                        shape = RoundedCornerShape(6.dp)
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

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SUGERENCIAS PARA TI",
                    style = MeloType.labelSmall,
                    letterSpacing = 1.2.sp,
                    color = MeloColors.textMuted
                )
                IconButton(
                    onClick = { queueViewModel.refreshSuggestions() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refrescar sugerencias",
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(16.dp)
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
                key = { index, track -> "screen_sugg_${index}_${track.id}" }) { _, track ->
                SuggestionTrackCard(
                    track = track,
                    activeAccent = activeAccent,
                    onClick = { queueViewModel.playTrack(track) },
                    onAddClick = { queueViewModel.insertTrackNext(track) }
                )
            }
        }
    }
}

@Composable
private fun NowPlayingLyricsSection(lyrics: String? = null) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(
            text = if (!lyrics.isNullOrBlank()) lyrics else "Letras sincronizadas disponibles próximamente",
            style = MeloType.body,
            color = MeloColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.verticalScroll(rememberScrollState())
        )
    }
}

@Composable
private fun NowPlayingArtistSection(
    state: PlayerUiState,
    artistDetails: SearchResult.Artist?,
    activeAccent: Color,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    queueViewModel: QueueViewModel = koinViewModel()
) {
    if (artistDetails == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = activeAccent)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                        .shadow(12.dp, CircleShape)
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
                    border = BorderStroke(
                        0.5.dp,
                        activeAccent.copy(alpha = 0.35f)
                    ),
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
                var expanded by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MeloColors.glassFill.copy(alpha = 0.3f),
                    border = BorderStroke(0.5.dp, MeloColors.glassBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Biografía",
                            style = MeloType.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MeloColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = bio,
                            style = MeloType.body.copy(lineHeight = 18.sp),
                            color = MeloColors.textSecondary,
                            maxLines = if (expanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (expanded) "Leer menos" else "Leer más",
                            style = MeloType.labelSmall,
                            color = activeAccent,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clickable { expanded = !expanded }
                        )
                    }
                }
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