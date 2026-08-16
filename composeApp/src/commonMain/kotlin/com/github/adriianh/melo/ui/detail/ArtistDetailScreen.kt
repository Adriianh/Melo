package com.github.adriianh.melo.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.AdaptiveLazyRow
import com.github.adriianh.melo.ui.components.AlbumCard
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
    val queueState by queueViewModel.queueState.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = MeloColors.surface0,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        artist?.name ?: initialName,
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
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.22f), MeloColors.surface0),
                            endY = 600f
                        )
                    )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MeloAsyncImage(
                                url = artist?.artworkUrl ?: initialArtwork,
                                contentDescription = artist?.name ?: initialName,
                                modifier = Modifier
                                    .size(160.dp)
                                    .shadow(16.dp, CircleShape)
                                    .clip(CircleShape),
                                size = 160.dp,
                                shape = CircleShape
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = artist?.name ?: initialName,
                                style = MeloType.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MeloColors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!artist?.subscriberCountText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = artist.subscriberCountText.orEmpty(),
                                    style = MeloType.labelMedium,
                                    color = MeloColors.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        artist?.topSongs?.firstOrNull()
                                            ?.let { queueViewModel.playTrack(it) }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir",
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Reproducir", color = Color.White)
                                }

                                Button(
                                    onClick = { /* artist radio */ },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MeloColors.surface2,
                                        contentColor = MeloColors.textPrimary
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Radio,
                                        contentDescription = "Radio",
                                        tint = MeloColors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Radio", color = MeloColors.textPrimary)
                                }
                            }
                        }
                    }

                    artist?.topSongs?.takeIf { it.isNotEmpty() }?.let { topSongs ->
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
                                    onClick = { queueViewModel.playTrack(track) }
                                )
                            }
                        }
                    }

                    artist?.sections?.forEach { section ->
                        item {
                            SectionHeader(title = section.title)
                            AdaptiveLazyRow(
                                items = section.items,
                                minCardWidth = 140.dp,
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

                                    is SearchResult.Song -> TrackRow(
                                        track = item.track,
                                        onClick = { queueViewModel.playTrack(item.track) },
                                        modifier = Modifier.size(300.dp, 60.dp)
                                    )

                                    else -> {}
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}