package com.github.adriianh.melo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_back
import com.github.adriianh.melo.resources.a11y_clear_selection
import com.github.adriianh.melo.resources.a11y_deselect_all
import com.github.adriianh.melo.resources.a11y_more_options
import com.github.adriianh.melo.resources.a11y_select_all
import com.github.adriianh.melo.resources.detail_about_album
import com.github.adriianh.melo.resources.detail_album_offline_empty_message
import com.github.adriianh.melo.resources.detail_menu_add_all_to_playlist
import com.github.adriianh.melo.resources.detail_menu_select_tracks
import com.github.adriianh.melo.resources.detail_offline_empty_title
import com.github.adriianh.melo.resources.detail_other_versions
import com.github.adriianh.melo.resources.detail_type_album
import com.github.adriianh.melo.resources.selection_count
import com.github.adriianh.melo.ui.LocalSelectionMode
import com.github.adriianh.melo.ui.components.layout.AdaptiveLazyRow
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.track.BatchSelectionBottomBar
import com.github.adriianh.melo.ui.components.feedback.MeloEmptyState
import com.github.adriianh.melo.ui.components.feedback.MeloErrorState
import com.github.adriianh.melo.ui.components.layout.SectionHeader
import com.github.adriianh.melo.ui.components.track.TrackInteractionContextMenu
import com.github.adriianh.melo.ui.components.track.rememberTrackInteraction
import com.github.adriianh.melo.ui.detail.components.EntityHeaderCard
import com.github.adriianh.melo.ui.detail.components.ExpandableDescriptionCard
import com.github.adriianh.melo.ui.detail.components.detailTrackItems
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.formatCollectionDuration
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    initialTitle: String = "",
    initialArtwork: String? = null,
    initialAuthor: String = "",
    bottomPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: EntityDetailViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying by remember(playerViewModel) {
        playerViewModel.stableUiState
            .map { it.isPlaying }
            .distinctUntilChanged()
    }.collectAsState(initial = playerViewModel.stableUiState.value.isPlaying)
    val accentPalette by playerViewModel.accentPalette.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val queueState by queueViewModel.queueState.collectAsState()

    val interaction = rememberTrackInteraction(libraryViewModel, queueViewModel)
    val activeAccent = interaction.resolveActiveAccent(accentPalette.dominant)
    val accentColor = MaterialTheme.colorScheme.primary
    val likedTrackIds =
        remember(libraryState.likedSongs) {
            libraryState.likedSongs.map { it.id }.toSet()
        }

    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId, initialTitle, initialArtwork, initialAuthor)
    }

    val album = (uiState.entity as? SearchResult.Album)?.takeIf { it.id == albumId }
    val songs = remember(album) { album?.songs.orEmpty() }
    val fallbackAlbumTitle = stringResource(Res.string.detail_type_album)
    val effectiveTitle =
        remember(album, initialTitle, songs, fallbackAlbumTitle) {
            album?.title?.takeIf { it.isNotBlank() }
                ?: initialTitle.takeIf { it.isNotBlank() }
                ?: songs.firstOrNull()?.album?.takeIf { it.isNotBlank() }
                ?: fallbackAlbumTitle
        }

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

    var showTopMenu by remember { mutableStateOf(false) }

    TrackInteractionContextMenu(
        interaction = interaction,
        libraryState = libraryState,
        activeAccent = activeAccent,
        onArtistClick = onArtistClick,
        onAlbumClick = onAlbumClick,
    )

    Scaffold(
        containerColor = MeloColors.surface0,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSelectionMode) {
                            pluralStringResource(
                                Res.plurals.selection_count,
                                selectedTrackIds.size,
                                selectedTrackIds.size,
                            )
                        } else {
                            effectiveTitle
                        },
                        style = MeloType.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedTrackIds = emptySet() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(Res.string.a11y_clear_selection),
                                tint = MeloColors.textPrimary,
                            )
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.a11y_back),
                                tint = MeloColors.textPrimary,
                            )
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        val isAllSelected = selectedTrackIds.size == songs.size
                        IconButton(
                            onClick = {
                                selectedTrackIds =
                                    if (isAllSelected) emptySet() else songs.map { it.id }.toSet()
                            },
                        ) {
                            Icon(
                                Icons.Default.SelectAll,
                                contentDescription =
                                    stringResource(
                                        if (isAllSelected) Res.string.a11y_deselect_all else Res.string.a11y_select_all,
                                    ),
                                tint = if (isAllSelected) accentColor else MeloColors.textPrimary,
                            )
                        }
                    } else if (songs.isNotEmpty()) {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(Res.string.a11y_more_options),
                                tint = MeloColors.textPrimary,
                            )
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(Res.string.detail_menu_add_all_to_playlist),
                                        style = MeloType.body,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = null,
                                        tint = accentColor,
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    interaction.openAddToPlaylist(songs)
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(Res.string.detail_menu_select_tracks),
                                        style = MeloType.body,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Checklist,
                                        contentDescription = null,
                                        tint = MeloColors.textPrimary,
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    if (songs.isNotEmpty()) {
                                        selectedTrackIds = setOf(songs.first().id)
                                    }
                                },
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MeloColors.textPrimary,
                    ),
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading && songs.isEmpty() && initialTitle.isBlank() && initialArtwork == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (uiState.error != null && songs.isEmpty() && initialTitle.isBlank()) {
            if (uiState.isOffline) {
                MeloEmptyState(
                    icon = Icons.Default.CloudOff,
                    title = stringResource(Res.string.detail_offline_empty_title),
                    message = stringResource(Res.string.detail_album_offline_empty_message),
                    modifier = Modifier.padding(paddingValues),
                )
            } else {
                MeloErrorState(
                    error = uiState.error,
                    onRetry = {
                        viewModel.loadAlbum(
                            albumId,
                            initialTitle,
                            initialArtwork,
                            initialAuthor,
                        )
                    },
                    modifier = Modifier.padding(paddingValues),
                )
            }
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(accentColor.copy(alpha = 0.20f), MeloColors.surface0),
                                endY = 600f,
                            ),
                        ),
            ) {
                val totalDurationMs = remember(songs) { songs.sumOf { it.durationMs } }
                val totalDurationFormatted =
                    remember(totalDurationMs) {
                        formatCollectionDuration(totalDurationMs)
                    }

                val authorText = album?.author?.ifEmpty { null } ?: initialAuthor.ifEmpty { null }
                val metaParts =
                    listOfNotNull(
                        album?.year?.takeIf { it.isNotBlank() },
                        if (songs.isNotEmpty()) "${songs.size} canciones" else null,
                        totalDurationFormatted.takeIf { it.isNotBlank() },
                    ).joinToString(" • ")

                val effectiveArtwork =
                    album?.artworkUrl?.takeIf { it.isNotBlank() }
                        ?: initialArtwork?.takeIf { it.isNotBlank() }
                        ?: songs.firstOrNull()?.artworkUrl?.takeIf { it.isNotBlank() }

                val isTrackDownloaded: (Track) -> Boolean =
                    remember(uiState.downloadedTrackIds) {
                        { track ->
                            track.id in uiState.downloadedTrackIds ||
                                track.id.removePrefix("piped:") in uiState.downloadedTrackIds ||
                                (track.sourceId != null && track.sourceId in uiState.downloadedTrackIds)
                        }
                    }

                val playableTracks =
                    remember(songs, uiState.downloadedTrackIds, uiState.isOffline) {
                        if (uiState.isOffline) {
                            songs.filter { isTrackDownloaded(it) }
                        } else {
                            songs
                        }
                    }

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 32.dp + bottomPadding.calculateBottomPadding()),
                ) {
                    item {
                        EntityHeaderCard(
                            artworkUrl = effectiveArtwork,
                            title = effectiveTitle,
                            subtitle = authorText,
                            onSubtitleClick = authorText?.let { { onArtistClick(it) } },
                            metadataText = metaParts,
                            isSaved = uiState.isSaved,
                            isDownloaded = uiState.isDownloaded,
                            isDownloading = uiState.isDownloading,
                            downloadProgress = uiState.downloadProgress,
                            downloadedCount = uiState.downloadedTrackCount,
                            totalTrackCount = uiState.totalTrackCount,
                            currentDownloadingTrackTitle = uiState.currentDownloadingTrackTitle,
                            onDownloadClick =
                                if (!uiState.isOffline && songs.isNotEmpty()) {
                                    viewModel::toggleDownload
                                } else {
                                    null
                                },
                            onPlayClick = {
                                if (playableTracks.isNotEmpty()) {
                                    queueViewModel.playTracks(playableTracks, 0)
                                }
                            },
                            onShuffleClick = {
                                if (playableTracks.isNotEmpty()) {
                                    queueViewModel.playShuffled(playableTracks)
                                }
                            },
                            onToggleSave = viewModel::toggleSave,
                            onAddToQueue = {
                                if (playableTracks.isNotEmpty()) {
                                    queueViewModel.addAllToQueue(playableTracks)
                                }
                            },
                            accentColor = accentColor,
                            hasTracks = playableTracks.isNotEmpty(),
                        )
                    }

                    val description = album?.description
                    if (!description.isNullOrBlank()) {
                        item {
                            ExpandableDescriptionCard(
                                title = stringResource(Res.string.detail_about_album),
                                description = description,
                                accentColor = accentColor,
                            )
                        }
                    }

                    if (songs.isNotEmpty()) {
                        detailTrackItems(
                            tracks = songs,
                            currentTrackId = queueState.currentTrack?.id,
                            isPlaying = isPlaying,
                            isLiked = { song -> song.id in likedTrackIds },
                            isOffline = uiState.isOffline,
                            isDownloaded = isTrackDownloaded,
                            isDownloading = { song ->
                                song.id in uiState.activeDownloadsMap ||
                                    song.id.removePrefix("piped:") in uiState.activeDownloadsMap
                            },
                            downloadProgress = { song ->
                                uiState.activeDownloadsMap[song.id]
                                    ?: uiState.activeDownloadsMap[song.id.removePrefix("piped:")]
                                    ?: 0f
                            },
                            isSelectionMode = isSelectionMode,
                            selectedTrackIds = selectedTrackIds,
                            onTrackLongClick = { song ->
                                if (!isSelectionMode) {
                                    selectedTrackIds = setOf(song.id)
                                }
                            },
                            onToggleSelectTrack = { song ->
                                selectedTrackIds =
                                    if (song.id in selectedTrackIds) {
                                        selectedTrackIds - song.id
                                    } else {
                                        selectedTrackIds + song.id
                                    }
                            },
                            onTrackClick = { _, song ->
                                val trackIndex = playableTracks.indexOfFirst { it.id == song.id }
                                if (trackIndex != -1) {
                                    queueViewModel.playTracks(playableTracks, trackIndex)
                                }
                            },
                            onMoreClick = { interaction.openContextMenu(it) },
                            onSwipeLeft = {
                                interaction.showAddedToQueueSnackbar(
                                    it,
                                    activeAccent,
                                )
                            },
                            onSwipeRight = { song ->
                                val isLiked = song.id in likedTrackIds
                                interaction.showToggledLikeSnackbar(
                                    song,
                                    isLiked,
                                    activeAccent,
                                )
                            },
                            accentColor = accentColor,
                            keyPrefix = "album",
                        )
                    } else if (uiState.isLoading) {
                        items(6) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MeloColors.surface2.copy(alpha = 0.5f)),
                            )
                        }
                    } else if (uiState.isOffline && songs.isEmpty()) {
                        item {
                            MeloEmptyState(
                                icon = Icons.Default.CloudOff,
                                title = stringResource(Res.string.detail_offline_empty_title),
                                message = stringResource(Res.string.detail_album_offline_empty_message),
                                modifier = Modifier.padding(vertical = 32.dp),
                            )
                        }
                    }

                    val otherVersions = album?.otherVersions.orEmpty()
                    if (otherVersions.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeader(title = stringResource(Res.string.detail_other_versions))
                            AdaptiveLazyRow(
                                items = otherVersions,
                                minCardWidth = 130.dp,
                                spacing = 12.dp,
                            ) { item, cardWidth ->
                                AlbumCard(
                                    title = item.title,
                                    subtitle = item.author,
                                    artworkUrl = item.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { onAlbumClick(item.id) },
                                )
                            }
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
                            if (selectedTrackIds.size == songs.size) {
                                emptySet()
                            } else {
                                songs
                                    .map { it.id }
                                    .toSet()
                            }
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
                            msg =
                                if (selTracks.size ==
                                    1
                                ) {
                                    "1 canción añadida a la cola"
                                } else {
                                    "${selTracks.size} canciones añadidas a la cola"
                                },
                            actionColor = activeAccent,
                        )
                        selectedTrackIds = emptySet()
                    },
                    onDownload = {
                        val selTracks = songs.filter { it.id in selectedTrackIds }
                        selTracks.forEach { libraryViewModel.downloadTrack(it) }
                        interaction.snackbar.show(
                            msg = "Descargando ${selTracks.size} canciones...",
                            actionColor = activeAccent,
                        )
                        selectedTrackIds = emptySet()
                    },
                    accentColor = accentColor,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
