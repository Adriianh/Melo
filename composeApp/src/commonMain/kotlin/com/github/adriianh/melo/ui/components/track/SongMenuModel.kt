package com.github.adriianh.melo.ui.components.track

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.common_action_add_to_queue
import com.github.adriianh.melo.resources.common_action_share
import com.github.adriianh.melo.resources.track_menu_add_to_likes
import com.github.adriianh.melo.resources.track_menu_add_to_playlist
import com.github.adriianh.melo.resources.track_menu_download
import com.github.adriianh.melo.resources.track_menu_downloading
import com.github.adriianh.melo.resources.track_menu_go_to_album
import com.github.adriianh.melo.resources.track_menu_go_to_artist
import com.github.adriianh.melo.resources.track_menu_play_next
import com.github.adriianh.melo.resources.track_menu_remove_download
import com.github.adriianh.melo.resources.track_menu_remove_from_library
import com.github.adriianh.melo.resources.track_menu_remove_from_likes
import com.github.adriianh.melo.resources.track_menu_remove_from_playlist
import com.github.adriianh.melo.resources.track_menu_remove_from_queue
import com.github.adriianh.melo.ui.i18n.UiText
import com.github.adriianh.melo.util.DarkMeloColors

enum class SongMenuEntryStyle {
    Normal,
    Destructive,
}

sealed interface SongMenuEntry {
    data class Action(
        val id: String,
        val icon: ImageVector,
        val label: UiText,
        val tint: Color? = null,
        val style: SongMenuEntryStyle = SongMenuEntryStyle.Normal,
        val enabled: Boolean = true,
        val onClick: () -> Unit,
    ) : SongMenuEntry

    data class Submenu(
        val id: String,
        val icon: ImageVector,
        val label: UiText,
        val content: PlaylistSubmenuContent,
    ) : SongMenuEntry

    data object Divider : SongMenuEntry
}

data class PlaylistSubmenuContent(
    val playlists: List<Playlist> = emptyList(),
    val containingPlaylistIds: Set<Long> = emptySet(),
    val onOpenSheetFallback: () -> Unit,
    val onSelectPlaylist: (Playlist) -> Unit = {},
    val onCreatePlaylistAndAdd: (String) -> Unit = {},
)

data class SongMenuContext(
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float? = null,
    val inQueue: Boolean = false,
    val isOnArtistScreen: Boolean = false,
    val isOnAlbumScreen: Boolean = false,
    val onPlayNext: () -> Unit = {},
    val onAddToQueue: () -> Unit = {},
    val onRemoveFromQueue: (() -> Unit)? = null,
    val onToggleLike: () -> Unit = {},
    val onAddToPlaylist: () -> Unit = {},
    val playlists: List<Playlist> = emptyList(),
    val containingPlaylistIds: Set<Long> = emptySet(),
    val onSelectPlaylist: (Playlist) -> Unit = {},
    val onCreatePlaylistAndAdd: (String) -> Unit = {},
    val onDownload: (() -> Unit)? = null,
    val onDeleteDownload: (() -> Unit)? = null,
    val onGoToArtist: (() -> Unit)? = null,
    val onGoToAlbum: (() -> Unit)? = null,
    val onShare: (() -> Unit)? = null,
    val onRemoveFromPlaylist: (() -> Unit)? = null,
    val onRemoveFromLibrary: (() -> Unit)? = null,
)

fun buildSongMenuEntries(
    track: Track,
    context: SongMenuContext,
): List<SongMenuEntry> {
    val nonEmptyGroups =
        listOf(
            buildPlaybackGroup(context),
            buildLibraryGroup(track, context),
            buildNavigationGroup(track, context),
            buildShareGroup(context),
            buildDestructiveGroup(context),
        ).filter { it.isNotEmpty() }

    return buildList {
        nonEmptyGroups.forEachIndexed { index, group ->
            if (index > 0) add(SongMenuEntry.Divider)
            addAll(group)
        }
    }
}

private fun buildPlaybackGroup(context: SongMenuContext): List<SongMenuEntry> =
    buildList {
        add(
            SongMenuEntry.Action(
                id = "play_next",
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                label = UiText.Resource(Res.string.track_menu_play_next),
                onClick = context.onPlayNext,
            ),
        )
        if (context.inQueue && context.onRemoveFromQueue != null) {
            add(
                SongMenuEntry.Action(
                    id = "remove_from_queue",
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    label = UiText.Resource(Res.string.track_menu_remove_from_queue),
                    tint = DarkMeloColors.menuQueueTint,
                    onClick = context.onRemoveFromQueue,
                ),
            )
        } else {
            add(
                SongMenuEntry.Action(
                    id = "add_to_queue",
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    label = UiText.Resource(Res.string.common_action_add_to_queue),
                    tint = DarkMeloColors.menuQueueTint,
                    onClick = context.onAddToQueue,
                ),
            )
        }
    }

private fun buildLibraryGroup(
    track: Track,
    context: SongMenuContext,
): List<SongMenuEntry> =
    buildList {
        add(
            SongMenuEntry.Action(
                id = "toggle_like",
                icon = if (context.isLiked) Icons.Default.HeartBroken else Icons.Default.FavoriteBorder,
                label =
                    UiText.Resource(
                        if (context.isLiked) {
                            Res.string.track_menu_remove_from_likes
                        } else {
                            Res.string.track_menu_add_to_likes
                        },
                    ),
                tint = DarkMeloColors.menuLikeTint,
                onClick = context.onToggleLike,
            ),
        )
        add(
            SongMenuEntry.Submenu(
                id = "add_to_playlist",
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                label = UiText.Resource(Res.string.track_menu_add_to_playlist),
                content =
                    PlaylistSubmenuContent(
                        playlists = context.playlists,
                        containingPlaylistIds = context.containingPlaylistIds,
                        onOpenSheetFallback = context.onAddToPlaylist,
                        onSelectPlaylist = context.onSelectPlaylist,
                        onCreatePlaylistAndAdd = context.onCreatePlaylistAndAdd,
                    ),
            ),
        )
        buildDownloadEntry(track, context)?.let(::add)
    }

private fun buildDownloadEntry(
    track: Track,
    context: SongMenuContext,
): SongMenuEntry? =
    when {
        context.isDownloaded && context.onDeleteDownload != null ->
            SongMenuEntry.Action(
                id = "remove_download",
                icon = Icons.Default.DeleteOutline,
                label = UiText.Resource(Res.string.track_menu_remove_download),
                tint = DarkMeloColors.menuDestructive,
                style = SongMenuEntryStyle.Destructive,
                onClick = context.onDeleteDownload,
            )

        context.isDownloading -> {
            val percent = context.downloadProgress?.let { "${(it * 100).toInt()}%" } ?: ""
            SongMenuEntry.Action(
                id = "downloading",
                icon = Icons.Default.Downloading,
                label = UiText.Resource(Res.string.track_menu_downloading, listOf(percent)),
                enabled = false,
                onClick = {},
            )
        }

        context.onDownload != null && !track.id.startsWith("local:") ->
            SongMenuEntry.Action(
                id = "download",
                icon = Icons.Default.Download,
                label = UiText.Resource(Res.string.track_menu_download),
                onClick = context.onDownload,
            )

        else -> null
    }

private fun buildNavigationGroup(
    track: Track,
    context: SongMenuContext,
): List<SongMenuEntry> =
    buildList {
        if (!context.isOnArtistScreen && track.artist.isNotBlank() && context.onGoToArtist != null) {
            add(
                SongMenuEntry.Action(
                    id = "go_to_artist",
                    icon = Icons.Default.Person,
                    label = UiText.Resource(Res.string.track_menu_go_to_artist),
                    onClick = context.onGoToArtist,
                ),
            )
        }
        if (!context.isOnAlbumScreen && track.album.isNotBlank() && context.onGoToAlbum != null) {
            add(
                SongMenuEntry.Action(
                    id = "go_to_album",
                    icon = Icons.Default.Album,
                    label = UiText.Resource(Res.string.track_menu_go_to_album),
                    onClick = context.onGoToAlbum,
                ),
            )
        }
    }

private fun buildShareGroup(context: SongMenuContext): List<SongMenuEntry> =
    buildList {
        if (context.onShare != null) {
            add(
                SongMenuEntry.Action(
                    id = "share",
                    icon = Icons.Default.Share,
                    label = UiText.Resource(Res.string.common_action_share),
                    onClick = context.onShare,
                ),
            )
        }
    }

private fun buildDestructiveGroup(context: SongMenuContext): List<SongMenuEntry> =
    buildList {
        if (context.onRemoveFromPlaylist != null) {
            add(
                SongMenuEntry.Action(
                    id = "remove_from_playlist",
                    icon = Icons.Default.DeleteOutline,
                    label = UiText.Resource(Res.string.track_menu_remove_from_playlist),
                    tint = DarkMeloColors.menuDestructive,
                    style = SongMenuEntryStyle.Destructive,
                    onClick = context.onRemoveFromPlaylist,
                ),
            )
        }
        if (context.onRemoveFromLibrary != null) {
            add(
                SongMenuEntry.Action(
                    id = "remove_from_library",
                    icon = Icons.Default.DeleteOutline,
                    label = UiText.Resource(Res.string.track_menu_remove_from_library),
                    tint = DarkMeloColors.menuDestructive,
                    style = SongMenuEntryStyle.Destructive,
                    onClick = context.onRemoveFromLibrary,
                ),
            )
        }
    }
