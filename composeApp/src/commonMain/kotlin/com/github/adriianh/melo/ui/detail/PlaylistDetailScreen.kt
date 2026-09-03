package com.github.adriianh.melo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.LocalSelectionMode
import com.github.adriianh.melo.ui.components.BatchSelectionBottomBar
import com.github.adriianh.melo.ui.components.MeloErrorState
import com.github.adriianh.melo.ui.components.TrackInteractionContextMenu
import com.github.adriianh.melo.ui.components.rememberReorderableState
import com.github.adriianh.melo.ui.components.rememberTrackInteraction
import com.github.adriianh.melo.ui.detail.components.DeletePlaylistDialog
import com.github.adriianh.melo.ui.detail.components.EntityHeaderCard
import com.github.adriianh.melo.ui.detail.components.ExpandableDescriptionCard
import com.github.adriianh.melo.ui.detail.components.PlaylistTopBarActions
import com.github.adriianh.melo.ui.detail.components.RenamePlaylistDialog
import com.github.adriianh.melo.ui.detail.components.ReorderablePlaylistTrackItem
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

    val isCustomPlaylist = remember(playlistId) {
        playlistId.startsWith("local:")
                || playlistId.startsWith("custom:")
                || (playlistId.toLongOrNull() != null && !playlistId.startsWith(
            "VL"
        ) && !playlistId.startsWith("PL") && !playlistId.startsWith("RD"))
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTrackIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedTrackIds.isNotEmpty()

    val selectionModeState = LocalSelectionMode.current
    LaunchedEffect(isSelectionMode) {
        selectionModeState.value = isSelectionMode
    }
    DisposableEffect(Unit) {
        onDispose {
            selectionModeState.value = false
        }
    }

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId, initialTitle, initialArtwork, initialAuthor)
    }

    val playlist = (uiState.entity as? SearchResult.Playlist)?.takeIf {
        it.id == playlistId || it.id == "local:$playlistId" ||
                it.id.removePrefix("local:") == playlistId.removePrefix("local:")
    }
    val songs = remember(playlist) { playlist?.songs.orEmpty() }
    val effectiveTitle = remember(playlist, initialTitle) {
        playlist?.title?.takeIf { it.isNotBlank() }
            ?: initialTitle.takeIf { it.isNotBlank() }
            ?: "Playlist"
    }

    if (showRenameDialog) {
        RenamePlaylistDialog(
            currentName = effectiveTitle,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                viewModel.renameLocalPlaylist(newName)
                showRenameDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        DeletePlaylistDialog(
            playlistName = effectiveTitle,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteLocalPlaylist(onBack)
            }
        )
    }

    TrackInteractionContextMenu(
        interaction = interaction,
        libraryState = libraryState,
        activeAccent = activeAccent,
        onArtistClick = onArtistClick,
        onAlbumClick = onAlbumClick,
        onRemoveFromPlaylist = if (isCustomPlaylist) { track ->
            viewModel.removeTrackFromLocalPlaylist(track.id)
            interaction.snackbar.show(
                msg = "Eliminada de la playlist",
                vector = Icons.Default.DeleteOutline,
                action = "Deshacer",
                actionColor = activeAccent,
                onAction = {
                    val playlistIdLong =
                        playlistId.removePrefix("local:").removePrefix("custom:").toLongOrNull()
                    if (playlistIdLong != null) {
                        libraryViewModel.addTrackToPlaylist(playlistIdLong, track)
                    }
                }
            )
        } else null
    )

    Scaffold(
        containerColor = MeloColors.surface0,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSelectionMode) "${selectedTrackIds.size} seleccionadas" else effectiveTitle,
                        style = MeloType.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedTrackIds = emptySet() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancelar selección",
                                tint = MeloColors.textPrimary
                            )
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = MeloColors.textPrimary
                            )
                        }
                    }
                },
                actions = {
                    PlaylistTopBarActions(
                        isSelectionMode = isSelectionMode,
                        selectedCount = selectedTrackIds.size,
                        totalCount = songs.size,
                        isCustomPlaylist = isCustomPlaylist,
                        accentColor = accentColor,
                        onToggleSelectAll = {
                            selectedTrackIds = if (selectedTrackIds.size == songs.size) {
                                emptySet()
                            } else {
                                songs.map { it.id }.toSet()
                            }
                        },
                        onRenameRequest = { showRenameDialog = true },
                        onDeleteRequest = { showDeleteDialog = true },
                        onAddAllToPlaylist = { interaction.openAddToPlaylist(songs) },
                        onStartSelection = {
                            if (songs.isNotEmpty()) selectedTrackIds = setOf(songs.first().id)
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MeloColors.textPrimary
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (uiState.error != null && songs.isEmpty()) {
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

                val isGenericAuthor = { s: String? ->
                    val c = s?.trim()?.lowercase() ?: ""
                    c.isBlank() || c == "unknown" || c == "desconocido" || c == "playlist" ||
                            c == "lista de reproducción" || c == "lista de reproduccion" ||
                            c == "álbum" || c == "album" || c == "youtube music"
                }

                val authorText =
                    playlist?.author?.takeIf { !isGenericAuthor(it) }
                        ?: initialAuthor.takeIf { !isGenericAuthor(it) }
                        ?: if (isCustomPlaylist) "Tú" else playlist?.author?.takeIf { it.isNotBlank() }
                            ?: initialAuthor.takeIf { it.isNotBlank() } ?: "YouTube Music"
                val metaParts = listOfNotNull(
                    if (songs.isNotEmpty()) "${songs.size} canciones" else null,
                    totalDurationFormatted.takeIf { it.isNotBlank() }
                ).joinToString(" • ")

                val effectiveArtwork = playlist?.artworkUrl?.takeIf { it.isNotBlank() }
                    ?: initialArtwork?.takeIf { it.isNotBlank() }
                    ?: songs.firstOrNull()?.artworkUrl?.takeIf { it.isNotBlank() }

                val lazyListState = rememberLazyListState()
                val reorderState = rememberReorderableState(lazyListState) { from, to ->
                    viewModel.moveLocalPlaylistTrack(from, to)
                }

                LazyColumn(
                    state = lazyListState,
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
                            artworkUrls = if (isCustomPlaylist) songs.mapNotNull { it.artworkUrl } else null,
                            title = effectiveTitle,
                            subtitle = authorText,
                            onSubtitleClick = if (isCustomPlaylist || authorText.equals(
                                    "Tú",
                                    ignoreCase = true
                                )
                            ) null else {
                                { onArtistClick(authorText) }
                            },
                            metadataText = metaParts,
                            isSaved = uiState.isSaved,
                            isDownloaded = uiState.isDownloaded,
                            isDownloading = uiState.isDownloading,
                            downloadProgress = uiState.downloadProgress,
                            downloadedCount = uiState.downloadedTrackCount,
                            totalTrackCount = uiState.totalTrackCount,
                            currentDownloadingTrackTitle = uiState.currentDownloadingTrackTitle,
                            onDownloadClick = if (songs.isNotEmpty()) viewModel::toggleDownload else null,
                            onPlayClick = { queueViewModel.playTracks(songs, 0) },
                            onShuffleClick = { queueViewModel.playShuffled(songs) },
                            onToggleSave = if (isCustomPlaylist) null else viewModel::toggleSave,
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
                        if (isCustomPlaylist) {
                            itemsIndexed(
                                songs,
                                key = { _, song -> "custom_pl_${song.id}" }
                            ) { index, song ->
                                ReorderablePlaylistTrackItem(
                                    song = song,
                                    index = index,
                                    totalCount = songs.size,
                                    reorderState = reorderState,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = song.id in selectedTrackIds,
                                    isCurrent = queueState.currentTrack?.id == song.id,
                                    isPlaying = queueState.currentTrack?.id == song.id && playerUiState.isPlaying,
                                    isDownloaded = song.id in uiState.downloadedTrackIds,
                                    isDownloading = song.id in uiState.activeDownloadsMap,
                                    downloadProgress = uiState.activeDownloadsMap[song.id] ?: 0f,
                                    itemKey = "custom_pl_${song.id}",
                                    accentColor = accentColor,
                                    activeAccent = activeAccent,
                                    onSelectionToggle = {
                                        selectedTrackIds = if (song.id in selectedTrackIds) {
                                            selectedTrackIds - song.id
                                        } else {
                                            selectedTrackIds + song.id
                                        }
                                    },
                                    onLongPress = {
                                        if (!isSelectionMode) selectedTrackIds = setOf(song.id)
                                    },
                                    onPlay = { queueViewModel.playTracks(songs, index) },
                                    onMoreClick = if (isSelectionMode) null else {
                                        { interaction.openContextMenu(song) }
                                    },
                                    onSwipeLeft = {
                                        interaction.showAddedToQueueSnackbar(song, activeAccent)
                                    },
                                    onSwipeRight = {
                                        interaction.showToggledLikeSnackbar(
                                            song,
                                            libraryState.likedSongs.any { it.id == song.id },
                                            activeAccent
                                        )
                                    },
                                    swipeRightIcon = if (
                                        libraryState.likedSongs.any { it.id == song.id }
                                    ) Icons.Default.HeartBroken else Icons.Default.Favorite
                                )
                            }
                        } else {
                            detailTrackItems(
                                tracks = songs,
                                currentTrackId = queueState.currentTrack?.id,
                                isPlaying = playerUiState.isPlaying,
                                isLiked = { song -> libraryState.likedSongs.any { it.id == song.id } },
                                isDownloaded = { song -> song.id in uiState.downloadedTrackIds },
                                isDownloading = { song -> song.id in uiState.activeDownloadsMap },
                                downloadProgress = { song ->
                                    uiState.activeDownloadsMap[song.id] ?: 0f
                                },
                                isSelectionMode = isSelectionMode,
                                selectedTrackIds = selectedTrackIds,
                                onTrackLongClick = { song ->
                                    if (!isSelectionMode) {
                                        selectedTrackIds = setOf(song.id)
                                    }
                                },
                                onToggleSelectTrack = { song ->
                                    selectedTrackIds = if (song.id in selectedTrackIds) {
                                        selectedTrackIds - song.id
                                    } else {
                                        selectedTrackIds + song.id
                                    }
                                },
                                onTrackClick = { index, _ ->
                                    queueViewModel.playTracks(
                                        songs,
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
                                keyPrefix = "playlist"
                            )
                        }
                    }
                }

                BatchSelectionBottomBar(
                    isVisible = isSelectionMode,
                    selectedCount = selectedTrackIds.size,
                    totalCount = songs.size,
                    onClearSelection = { selectedTrackIds = emptySet() },
                    onSelectAllToggle = {
                        selectedTrackIds =
                            if (selectedTrackIds.size == songs.size) emptySet() else songs.map { it.id }
                                .toSet()
                    },
                    onAddToPlaylist = {
                        val selTracks = songs.filter { it.id in selectedTrackIds }
                        interaction.openAddToPlaylist(selTracks)
                        selectedTrackIds = emptySet()
                    },
                    onAddToQueue = {
                        val selTracks = songs.filter { it.id in selectedTrackIds }
                        queueViewModel.addAllToQueue(selTracks)
                        interaction.snackbar.show(
                            msg = if (selTracks.size == 1) "1 canción añadida a la cola" else "${selTracks.size} canciones añadidas a la cola",
                            actionColor = activeAccent
                        )
                        selectedTrackIds = emptySet()
                    },
                    onDownload = {
                        val selTracks = songs.filter { it.id in selectedTrackIds }
                        selTracks.forEach { libraryViewModel.downloadTrack(it) }
                        interaction.snackbar.show(
                            msg = "Descargando ${selTracks.size} canciones...",
                            actionColor = activeAccent
                        )
                        selectedTrackIds = emptySet()
                    },
                    onDeleteSelected = if (isCustomPlaylist) {
                        {
                            val selIds = selectedTrackIds.toList()
                            selIds.forEach { viewModel.removeTrackFromLocalPlaylist(it) }
                            selectedTrackIds = emptySet()
                            interaction.snackbar.show(
                                msg = if (selIds.size == 1) "1 canción eliminada de la playlist" else "${selIds.size} canciones eliminadas de la playlist",
                                actionColor = activeAccent
                            )
                        }
                    } else null,
                    accentColor = accentColor,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
