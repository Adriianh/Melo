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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    initialTitle: String = "",
    initialArtwork: String? = null,
    initialAuthor: String = "",
    viewModel: EntityDetailViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId, initialTitle, initialArtwork, initialAuthor)
    }

    val album = uiState.entity as? SearchResult.Album

    val queueState by queueViewModel.queueState.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = MeloColors.surface0,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        album?.title ?: initialTitle,
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
        if (uiState.isLoading && album?.songs == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (uiState.error != null) {
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
                            viewModel.loadAlbum(
                                albumId,
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
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.22f), MeloColors.surface0),
                            endY = 600f
                        )
                    )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val effectiveArtwork = album?.artworkUrl?.takeIf { it.isNotBlank() }
                                ?: initialArtwork?.takeIf { it.isNotBlank() }
                                ?: album?.songs?.firstOrNull()?.artworkUrl?.takeIf { it.isNotBlank() }

                            MeloAsyncImage(
                                url = effectiveArtwork,
                                contentDescription = album?.title ?: initialTitle,
                                modifier = Modifier
                                    .size(200.dp)
                                    .shadow(16.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp)),
                                size = 200.dp,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = album?.title ?: initialTitle,
                                style = MeloType.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MeloColors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val subtitleParts = listOfNotNull(
                                album?.author ?: initialAuthor.ifEmpty { null },
                                album?.year,
                                album?.songs?.let { "${it.size} canciones" }
                            )

                            Text(
                                text = subtitleParts.joinToString(" • "),
                                style = MeloType.labelMedium,
                                color = MeloColors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        album?.songs?.firstOrNull()
                                            ?.let { queueViewModel.playTrack(it) }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = RoundedCornerShape(24.dp)
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
                                    onClick = { viewModel.toggleSave() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.isSaved) accentColor.copy(alpha = 0.15f) else MeloColors.surface2,
                                        contentColor = if (uiState.isSaved) accentColor else MeloColors.textPrimary
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(
                                        if (uiState.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Guardar",
                                        tint = if (uiState.isSaved) accentColor else MeloColors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        if (uiState.isSaved) "Guardado" else "Guardar",
                                        color = if (uiState.isSaved) accentColor else MeloColors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    val songs = album?.songs ?: emptyList()
                    itemsIndexed(songs) { index, song ->
                        val isPlayingThis = queueState.currentTrack?.id == song.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isPlayingThis) accentColor.copy(alpha = 0.15f)
                                    else Color.Transparent
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackRow(
                                track = song,
                                trackNumber = index + 1,
                                onClick = { queueViewModel.playTrack(song) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}