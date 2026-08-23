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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    initialTitle: String = "",
    initialArtwork: String? = null,
    initialAuthor: String = "",
    viewModel: EntityDetailViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerUiState by playerViewModel.uiState.collectAsState()

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId, initialTitle, initialArtwork, initialAuthor)
    }

    val playlist = uiState.entity as? SearchResult.Playlist
    val effectiveTitle = remember(playlist, initialTitle) {
        playlist?.title?.takeIf { it.isNotBlank() }
            ?: initialTitle.takeIf { it.isNotBlank() }
            ?: "Playlist"
    }

    val queueState by queueViewModel.queueState.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

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
                        onClick = {
                            viewModel.loadPlaylist(
                                playlistId,
                                initialTitle,
                                initialArtwork,
                                initialAuthor
                            )
                        },
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
                val songs = remember(playlist) { playlist?.songs.orEmpty() }
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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val effectiveArtwork = playlist?.artworkUrl?.takeIf { it.isNotBlank() }
                                ?: initialArtwork?.takeIf { it.isNotBlank() }
                                ?: songs.firstOrNull()?.artworkUrl?.takeIf { it.isNotBlank() }

                            MeloAsyncImage(
                                url = effectiveArtwork,
                                contentDescription = playlist?.title ?: initialTitle,
                                modifier = Modifier
                                    .size(200.dp)
                                    .shadow(16.dp, RoundedCornerShape(14.dp))
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        1.dp,
                                        MeloColors.borderStrong,
                                        RoundedCornerShape(14.dp)
                                    ),
                                size = 200.dp,
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = effectiveTitle,
                                style = MeloType.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MeloColors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val authorText =
                                playlist?.author?.ifEmpty { null } ?: initialAuthor.ifEmpty { null }
                            if (!authorText.isNullOrBlank()) {
                                Text(
                                    text = authorText,
                                    style = MeloType.labelMedium,
                                    color = MeloColors.textSecondary,
                                    modifier = Modifier.clickable {
                                        onArtistClick(authorText)
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            val metaParts = listOfNotNull(
                                if (songs.isNotEmpty()) "${songs.size} canciones" else null,
                                totalDurationFormatted.takeIf { it.isNotBlank() }
                            )

                            if (metaParts.isNotEmpty()) {
                                Text(
                                    text = metaParts.joinToString(" • "),
                                    style = MeloType.labelSmall,
                                    color = MeloColors.textMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { queueViewModel.playTracks(songs, 0) },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = RoundedCornerShape(24.dp),
                                    enabled = songs.isNotEmpty()
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
                                    onClick = { queueViewModel.playShuffled(songs) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MeloColors.surface2,
                                        contentColor = MeloColors.textPrimary
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    enabled = songs.isNotEmpty()
                                ) {
                                    Icon(
                                        Icons.Default.Shuffle,
                                        contentDescription = "Aleatorio",
                                        tint = MeloColors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Aleatorio", color = MeloColors.textPrimary)
                                }

                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = if (uiState.isSaved) accentColor.copy(alpha = 0.15f) else MeloColors.surface2,
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (uiState.isSaved) accentColor.copy(alpha = 0.4f) else MeloColors.border
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable { viewModel.toggleSave() }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (uiState.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = if (uiState.isSaved) "Guardado" else "Guardar",
                                            tint = if (uiState.isSaved) accentColor else MeloColors.textPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MeloColors.surface2,
                                    border = BorderStroke(0.5.dp, MeloColors.border),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable { queueViewModel.addAllToQueue(songs) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = "Añadir a la cola",
                                            tint = MeloColors.textPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val description = playlist?.description
                    if (!description.isNullOrBlank()) {
                        item {
                            var expanded by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MeloColors.glassFill.copy(alpha = 0.3f),
                                border = BorderStroke(0.5.dp, MeloColors.glassBorder)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Descripción",
                                        style = MeloType.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MeloColors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = description,
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

                    if (songs.isNotEmpty()) {
                        itemsIndexed(songs) { index, song ->
                            val isPlayingThis = queueState.currentTrack?.id == song.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isPlayingThis) accentColor.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                            ) {
                                TrackRow(
                                    track = song,
                                    trackNumber = index + 1,
                                    isCurrent = isPlayingThis,
                                    isPlaying = isPlayingThis && playerUiState.isPlaying,
                                    onClick = { queueViewModel.playTracks(songs, index) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}