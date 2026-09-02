package com.github.adriianh.melo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.MeloErrorState
import com.github.adriianh.melo.ui.components.TrackContextMenu
import com.github.adriianh.melo.ui.components.TrackInteractionContextMenu
import com.github.adriianh.melo.ui.components.rememberTrackInteraction
import com.github.adriianh.melo.ui.detail.components.EntityHeaderCard
import com.github.adriianh.melo.ui.detail.components.ExpandableDescriptionCard
import com.github.adriianh.melo.ui.detail.components.detailTrackItems
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    initialTitle: String = "",
    initialArtwork: String? = null,
    initialAuthor: String = "",
    viewModel: EntityDetailViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerUiState by playerViewModel.uiState.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val queueState by queueViewModel.queueState.collectAsState()

    val interaction = rememberTrackInteraction(libraryViewModel, queueViewModel)
    val activeAccent = interaction.resolveActiveAccent(playerUiState)
    val accentColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId, initialTitle, initialArtwork, initialAuthor)
    }

    val playlist = uiState.entity as? SearchResult.Playlist
    val songs = remember(playlist) { playlist?.songs.orEmpty() }
    val effectiveTitle = remember(playlist, initialTitle) {
        playlist?.title?.takeIf { it.isNotBlank() }
            ?: initialTitle.takeIf { it.isNotBlank() }
            ?: "Playlist"
    }

    TrackInteractionContextMenu(
        interaction = interaction,
        libraryState = libraryState,
        activeAccent = activeAccent,
        onArtistClick = onArtistClick,
        onAlbumClick = onAlbumClick
    )

    Scaffold(
        containerColor = MeloColors.surface0,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        effectiveTitle,
                        style = MeloType.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = MeloColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MeloColors.textPrimary
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && playlist?.songs == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (uiState.error != null && playlist?.songs == null) {
            MeloErrorState(
                error = uiState.error,
                onRetry = {
                    viewModel.loadPlaylist(
                        playlistId,
                        initialTitle,
                        initialArtwork,
                        initialAuthor
                    )
                },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.20f), MeloColors.surface0),
                            endY = 600f
                        )
                    )
            ) {
                val totalDurationMs = remember(songs) { songs.sumOf { it.durationMs } }
                val totalDurationFormatted = remember(totalDurationMs) {
                    if (totalDurationMs <= 0) ""
                    else {
                        val minutes = (totalDurationMs / 1000) / 60
                        val hours = minutes / 60
                        val remainingMinutes = minutes % 60
                        if (hours > 0) "$hours h $remainingMinutes min" else "$minutes min"
                    }
                }

                val authorText =
                    playlist?.author?.ifEmpty { null } ?: initialAuthor.ifEmpty { null }
                val metaParts = listOfNotNull(
                    if (songs.isNotEmpty()) "${songs.size} canciones" else null,
                    totalDurationFormatted.takeIf { it.isNotBlank() }
                ).joinToString(" • ")

                val effectiveArtwork = playlist?.artworkUrl?.takeIf { it.isNotBlank() }
                    ?: initialArtwork?.takeIf { it.isNotBlank() }
                    ?: songs.firstOrNull()?.artworkUrl?.takeIf { it.isNotBlank() }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        EntityHeaderCard(
                            artworkUrl = effectiveArtwork,
                            title = effectiveTitle,
                            subtitle = authorText,
                            onSubtitleClick = authorText?.let { { onArtistClick(it) } },
                            metadataText = metaParts,
                            isSaved = uiState.isSaved,
                            onPlayClick = { queueViewModel.playTracks(songs, 0) },
                            onShuffleClick = { queueViewModel.playShuffled(songs) },
                            onToggleSave = viewModel::toggleSave,
                            onAddToQueue = { queueViewModel.addAllToQueue(songs) },
                            accentColor = accentColor,
                            hasTracks = songs.isNotEmpty()
                        )
                    }

                    val description = playlist?.description
                    if (!description.isNullOrBlank()) {
                        item {
                            ExpandableDescriptionCard(
                                title = "Descripción",
                                description = description,
                                accentColor = accentColor
                            )
                        }
                    }

                    if (songs.isNotEmpty()) {
                        detailTrackItems(
                            tracks = songs,
                            currentTrackId = queueState.currentTrack?.id,
                            isPlaying = playerUiState.isPlaying,
                            isLiked = { song -> libraryState.likedSongs.any { it.id == song.id } },
                            onTrackClick = { index, _ -> queueViewModel.playTracks(songs, index) },
                            onMoreClick = { interaction.openContextMenu(it) },
                            onSwipeLeft = {
                                interaction.showAddedToQueueSnackbar(
                                    it,
                                    activeAccent
                                )
                            },
                            onSwipeRight = { song ->
                                val isLiked = libraryState.likedSongs.any { it.id == song.id }
                                interaction.showToggledLikeSnackbar(
                                    song,
                                    isLiked,
                                    activeAccent
                                )
                            },
                            accentColor = accentColor,
                            keyPrefix = "playlist"
                        )
                    }
                }
            }
        }
    }
}