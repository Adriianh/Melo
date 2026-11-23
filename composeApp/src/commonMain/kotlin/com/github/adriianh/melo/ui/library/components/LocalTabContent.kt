@file:Suppress("ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.library.components

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.platform.PlatformFileSystem
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_manage_folders
import com.github.adriianh.melo.resources.a11y_more_options
import com.github.adriianh.melo.resources.a11y_play_all
import com.github.adriianh.melo.resources.a11y_rescan
import com.github.adriianh.melo.resources.common_action_close
import com.github.adriianh.melo.resources.library_local_default_folders
import com.github.adriianh.melo.resources.library_local_device_audio
import com.github.adriianh.melo.resources.library_local_empty_subtitle
import com.github.adriianh.melo.resources.library_local_empty_title
import com.github.adriianh.melo.resources.library_local_folders_button
import com.github.adriianh.melo.resources.library_local_folders_subtitle
import com.github.adriianh.melo.resources.library_local_folders_title
import com.github.adriianh.melo.resources.library_local_scan_button
import com.github.adriianh.melo.resources.library_local_scan_now
import com.github.adriianh.melo.resources.library_local_scanning
import com.github.adriianh.melo.resources.library_local_track_count
import com.github.adriianh.melo.ui.components.LocalFolderPathsEditor
import com.github.adriianh.melo.ui.components.controls.LocalSwipeRevealGroup
import com.github.adriianh.melo.ui.components.controls.MeloSwipeActions
import com.github.adriianh.melo.ui.components.controls.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.controls.rememberSwipeRevealGroup
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.desktopScroll
import com.github.adriianh.melo.util.formatDuration
import com.github.adriianh.melo.util.rememberAudioPermissionRequester
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LocalTabContent(
    localTracks: List<Track>,
    localLibraryPaths: List<String>,
    isScanning: Boolean,
    onRescan: () -> Unit,
    onAddFolder: (String) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onMoreClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
    isLiked: (Track) -> Boolean = { false },
    isQueued: (Track) -> Boolean = { false },
    onSwipeLeft: (Track) -> Unit = {},
    onSwipeRight: (Track) -> Unit = {},
    onRemoveFromQueue: ((Track) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val requestAudioPermission =
        rememberAudioPermissionRequester { isGranted ->
            if (isGranted) {
                onRescan()
            }
        }

    LaunchedEffect(Unit) {
        requestAudioPermission()
    }

    var showFoldersDialog by remember { mutableStateOf(false) }

    if (showFoldersDialog) {
        ManageFoldersDialog(
            configuredPaths = localLibraryPaths,
            onDismiss = { showFoldersDialog = false },
            onAddPath = onAddFolder,
            onRemovePath = onRemoveFolder,
            onRescan = {
                requestAudioPermission()
                showFoldersDialog = false
            },
        )
    }

    if (isScanning) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(Res.string.library_local_scanning),
                    style = MeloType.body,
                    color = MeloColors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    if (localTracks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MeloColors.textMuted,
                    modifier = Modifier.size(56.dp),
                )
                Text(
                    text = stringResource(Res.string.library_local_empty_title),
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.library_local_empty_subtitle),
                    style = MeloType.body,
                    color = MeloColors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { showFoldersDialog = true },
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(Res.string.library_local_folders_button))
                    }
                    Button(
                        onClick = requestAudioPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(Res.string.library_local_scan_button))
                    }
                }
            }
        }
        return
    }

    val listState = rememberLazyListState()
    val revealGroup = rememberSwipeRevealGroup()

    CompositionLocalProvider(LocalSwipeRevealGroup provides revealGroup) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize().desktopScroll(listState),
        ) {
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text =
                                    pluralStringResource(
                                        Res.plurals.library_local_track_count,
                                        localTracks.size,
                                        localTracks.size,
                                    ),
                                style = MeloType.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MeloColors.textPrimary,
                            )
                            Text(
                                text = stringResource(Res.string.library_local_device_audio),
                                style = MeloType.labelSmall,
                                color = MeloColors.textSecondary,
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { showFoldersDialog = true },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = stringResource(Res.string.a11y_manage_folders),
                                    tint = MeloColors.textSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = requestAudioPermission,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(Res.string.a11y_rescan),
                                    tint = MeloColors.textSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    if (localTracks.isNotEmpty()) {
                        Button(
                            onClick = onPlayAll,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(Res.string.a11y_play_all))
                        }
                    }
                }
            }

            items(localTracks, key = { it.id }) { track ->
                MeloSwipeableItem(
                    onSwipeRight = { onSwipeRight(track) },
                    trailingActions =
                        MeloSwipeActions.queueTrailingActions(
                            isQueued = isQueued(track),
                            onQueue = { onSwipeLeft(track) },
                            onRemove = onRemoveFromQueue?.let { remove -> { remove(track) } },
                        ),
                    leadingAction = MeloSwipeActions.like(isLiked(track)),
                    cornerRadius = 12,
                ) {
                    LocalTrackRow(
                        track = track,
                        onClick = { onPlayTrack(track) },
                        onMoreClick = { onMoreClick(track) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ManageFoldersDialog(
    configuredPaths: List<String>,
    onDismiss: () -> Unit,
    onAddPath: (String) -> Unit,
    onRemovePath: (String) -> Unit,
    onRescan: () -> Unit,
) {
    val defaultPaths = remember { PlatformFileSystem.getDefaultMusicPaths() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MeloColors.surface1,
        title = {
            Text(
                text = stringResource(Res.string.library_local_folders_title),
                style = MeloType.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MeloColors.textPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.library_local_folders_subtitle),
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary,
                )

                LocalFolderPathsEditor(
                    configuredPaths = configuredPaths,
                    onAddPath = onAddPath,
                    onRemovePath = onRemovePath,
                )

                if (defaultPaths.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.library_local_default_folders),
                        style = MeloType.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MeloColors.textPrimary,
                    )
                    defaultPaths.forEach { path ->
                        Surface(
                            color = MeloColors.surface2.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = path,
                                    style = MeloType.labelSmall,
                                    color = MeloColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRescan,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(stringResource(Res.string.library_local_scan_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_action_close), color = MeloColors.textSecondary)
            }
        },
    )
}

@Composable
private fun LocalTrackRow(
    track: Track,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val durationMinSec = formatDuration(track.durationMs)

    Surface(
        color = MeloColors.surface1,
        shape = RoundedCornerShape(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MeloAsyncImage(
                url = track.artworkUrl,
                contentDescription = track.title,
                size = 48.dp,
                shape = RoundedCornerShape(8.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MeloType.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MeloColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                val metaSuffix =
                    buildString {
                        if (track.album.isNotBlank()) append(" • ${track.album}")
                        if (durationMinSec.isNotBlank()) append(" • $durationMinSec")
                    }
                Text(
                    text = "${track.artist}$metaSuffix",
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(Res.string.a11y_more_options),
                    tint = MeloColors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
