package com.github.adriianh.melo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.github.adriianh.melo.ui.components.AdaptiveLazyRow
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.MeloErrorState
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.components.TrackContextMenu
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.components.rememberTrackInteraction
import com.github.adriianh.melo.ui.detail.components.ArtistHeaderCard
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
fun ArtistDetailScreen(
    artistId: String,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    initialName: String = "",
    initialArtwork: String? = null,
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

    LaunchedEffect(artistId) {
        viewModel.loadArtist(artistId, initialName, initialArtwork)
    }

    val artist = uiState.entity as? SearchResult.Artist
    val effectiveName = remember(artist, initialName) {
        artist?.name?.takeIf { it.isNotBlank() }
            ?: initialName.takeIf { it.isNotBlank() }
            ?: "Artista"
    }

    if (interaction.contextMenuTrack != null) {
        val track = interaction.contextMenuTrack!!
        val isLiked = libraryState.likedSongs.any { it.id == track.id }
        TrackContextMenu(
            track = track,
            onDismissRequest = interaction::dismissContextMenu,
            onPlayNext = {
                interaction.showPlayNextSnackbar(track, activeAccent)
                interaction.contextMenuTrack = null
            },
            onAddToQueue = {
                interaction.showAddedToQueueSnackbar(track, activeAccent)
                interaction.contextMenuTrack = null
            },
            onToggleLike = {
                interaction.showToggledLikeSnackbar(track, isLiked, activeAccent)
                interaction.contextMenuTrack = null
            },
            isLiked = isLiked,
            onAddToPlaylist = { /* TODO */ },
            onGoToArtist = {
                interaction.contextMenuTrack = null
            },
            onGoToAlbum = {
                interaction.contextMenuTrack = null
            },
            onShare = { /* TODO */ }
        )
    }

    Scaffold(
        containerColor = MeloColors.surface0,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        effectiveName,
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
        if (uiState.isLoading && artist?.topSongs == null && artist?.sections.isNullOrEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (uiState.error != null && artist?.topSongs == null && artist?.sections.isNullOrEmpty()) {
            MeloErrorState(
                error = uiState.error,
                onRetry = { viewModel.loadArtist(artistId, initialName, initialArtwork) },
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
                val topSongs = remember(artist) { artist?.topSongs.orEmpty() }
                val effectiveArtwork = artist?.artworkUrl?.takeIf { it.isNotBlank() }
                    ?: initialArtwork?.takeIf { it.isNotBlank() }
                    ?: topSongs.firstOrNull()?.artworkUrl?.takeIf { it.isNotBlank() }

                val subtitleParts = listOfNotNull(
                    artist?.monthlyListenerCount?.takeIf { it.isNotBlank() },
                    artist?.subscriberCountText?.takeIf { it.isNotBlank() }
                ).joinToString(" • ")

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        ArtistHeaderCard(
                            artworkUrl = effectiveArtwork,
                            name = effectiveName,
                            subtitleText = subtitleParts,
                            isSaved = uiState.isSaved,
                            onPlayClick = { queueViewModel.playTracks(topSongs, 0) },
                            onShuffleClick = { queueViewModel.playShuffled(topSongs) },
                            onRadioClick = {
                                queueViewModel.startArtistRadio(
                                    artistId = artistId,
                                    fallbackTracks = topSongs
                                )
                            },
                            onToggleFollow = viewModel::toggleSave,
                            accentColor = accentColor,
                            hasTracks = topSongs.isNotEmpty()
                        )
                    }

                    val bio = artist?.description
                    if (!bio.isNullOrBlank()) {
                        item {
                            ExpandableDescriptionCard(
                                title = "Biografía",
                                description = bio,
                                accentColor = accentColor,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    if (topSongs.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Canciones populares",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        detailTrackItems(
                            tracks = topSongs,
                            currentTrackId = queueState.currentTrack?.id,
                            isPlaying = playerUiState.isPlaying,
                            isLiked = { song -> libraryState.likedSongs.any { it.id == song.id } },
                            onTrackClick = { index, _ ->
                                queueViewModel.playTracks(
                                    topSongs,
                                    index
                                )
                            },
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
                            keyPrefix = "artist_top",
                            itemModifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    val sections = artist?.sections.orEmpty()
                    for (section in sections) {
                        if (section.items.isEmpty()) continue

                        item(key = "section_${section.title}") {
                            SectionHeader(
                                title = section.title,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            AdaptiveLazyRow(
                                items = section.items,
                                minCardWidth = 130.dp,
                                spacing = 12.dp,
                                horizontalPadding = 16.dp
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
                                        onMoreClick = { interaction.openContextMenu(item.track) },
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
                }
            }
        }
    }
}