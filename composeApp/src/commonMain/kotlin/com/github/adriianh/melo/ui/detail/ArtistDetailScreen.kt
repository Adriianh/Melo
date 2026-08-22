package com.github.adriianh.melo.ui.detail

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.AdaptiveLazyRow
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
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
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(artistId) {
        viewModel.loadArtist(artistId, initialName, initialArtwork)
    }

    val artist = uiState.entity as? SearchResult.Artist
    val effectiveName = remember(artist, initialName) {
        artist?.name?.takeIf { it.isNotBlank() }
            ?: initialName.takeIf { it.isNotBlank() }
            ?: "Artista"
    }

    val queueState by queueViewModel.queueState.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

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
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error.orEmpty(),
                        style = MeloType.body,
                        color = MeloColors.textMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.loadArtist(artistId, initialName, initialArtwork) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Reintentar")
                    }
                }
            }
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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val effectiveArtwork = artist?.artworkUrl?.takeIf { it.isNotBlank() }
                                ?: initialArtwork?.takeIf { it.isNotBlank() }
                                ?: topSongs.firstOrNull()?.artworkUrl?.takeIf { it.isNotBlank() }

                            MeloAsyncImage(
                                url = effectiveArtwork,
                                contentDescription = artist?.name ?: initialName,
                                modifier = Modifier
                                    .size(160.dp)
                                    .shadow(16.dp, CircleShape)
                                    .clip(CircleShape)
                                    .border(1.5.dp, MeloColors.borderStrong, CircleShape),
                                size = 160.dp,
                                shape = CircleShape
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = effectiveName,
                                style = MeloType.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MeloColors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            val subtitleParts = listOfNotNull(
                                artist?.monthlyListenerCount?.takeIf { it.isNotBlank() },
                                artist?.subscriberCountText?.takeIf { it.isNotBlank() }
                            )

                            if (subtitleParts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = subtitleParts.joinToString(" • "),
                                    style = MeloType.labelSmall,
                                    color = MeloColors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { queueViewModel.playTracks(topSongs, 0) },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = RoundedCornerShape(24.dp),
                                    enabled = topSongs.isNotEmpty()
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Reproducir", color = MaterialTheme.colorScheme.onPrimary)
                                }

                                Button(
                                    onClick = { queueViewModel.playShuffled(topSongs) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MeloColors.surface2,
                                        contentColor = MeloColors.textPrimary
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    enabled = topSongs.isNotEmpty()
                                ) {
                                    Icon(
                                        Icons.Default.Shuffle,
                                        contentDescription = "Aleatorio",
                                        tint = MeloColors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Aleatorio", color = MeloColors.textPrimary)
                                }

                                Button(
                                    onClick = { viewModel.toggleSave() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.isSaved) accentColor.copy(alpha = 0.15f) else MeloColors.surface2,
                                        contentColor = if (uiState.isSaved) accentColor else MeloColors.textPrimary
                                    ),
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (uiState.isSaved) accentColor.copy(alpha = 0.4f) else MeloColors.border
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(
                                        if (uiState.isSaved) Icons.Default.Check else Icons.Default.PersonAdd,
                                        contentDescription = if (uiState.isSaved) "Siguiendo" else "Seguir",
                                        tint = if (uiState.isSaved) accentColor else MeloColors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        if (uiState.isSaved) "Siguiendo" else "Seguir",
                                        color = if (uiState.isSaved) accentColor else MeloColors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    val bio = artist?.description
                    if (!bio.isNullOrBlank()) {
                        item {
                            var expanded by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MeloColors.glassFill.copy(alpha = 0.3f),
                                border = BorderStroke(0.5.dp, MeloColors.glassBorder),
                                modifier = Modifier.padding(horizontal = 16.dp)
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
                                        color = accentColor,
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .clickable { expanded = !expanded }
                                    )
                                }
                            }
                        }
                    }

                    if (topSongs.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Canciones populares")
                        }
                        itemsIndexed(topSongs) { index, track ->
                            val isPlayingThis = queueState.currentTrack?.id == track.id
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isPlayingThis) accentColor.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                            ) {
                                TrackRow(
                                    track = track,
                                    trackNumber = index + 1,
                                    isCurrent = isPlayingThis,
                                    isPlaying = isPlayingThis,
                                    onClick = { queueViewModel.playTracks(topSongs, index) }
                                )
                            }
                        }
                    }

                    val sections = artist?.sections.orEmpty()
                    for (section in sections) {
                        if (section.items.isEmpty()) continue

                        item(key = "section_${section.title}") {
                            SectionHeader(title = section.title)
                            AdaptiveLazyRow(
                                items = section.items,
                                minCardWidth = 130.dp,
                                spacing = 12.dp
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
                }
            }
        }
    }
}