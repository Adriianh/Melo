@file:Suppress("FunctionNaming", "MatchingDeclarationName", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.track

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.common_action_add_to_queue
import com.github.adriianh.melo.resources.common_action_share
import com.github.adriianh.melo.resources.menu_action_play
import com.github.adriianh.melo.resources.menu_action_save_to_library
import com.github.adriianh.melo.resources.menu_action_shuffle
import com.github.adriianh.melo.resources.menu_action_start_radio
import com.github.adriianh.melo.resources.track_menu_go_to_artist
import com.github.adriianh.melo.resources.track_menu_play_next
import com.github.adriianh.melo.resources.track_menu_remove_from_library
import com.github.adriianh.melo.ui.i18n.UiText
import com.github.adriianh.melo.util.DarkMeloColors

data class EntityMenuContext(
    val isSaved: Boolean = false,
    val onPlay: (() -> Unit)? = null,
    val onShuffle: (() -> Unit)? = null,
    val onPlayNext: (() -> Unit)? = null,
    val onAddToQueue: (() -> Unit)? = null,
    val onToggleSave: (() -> Unit)? = null,
    val onGoToArtist: (() -> Unit)? = null,
    val onStartRadio: (() -> Unit)? = null,
    val onShare: (() -> Unit)? = null,
)

fun buildAlbumOrPlaylistMenuEntries(
    isAlbum: Boolean,
    author: String,
    context: EntityMenuContext,
): List<SongMenuEntry> {
    val playbackGroup = buildEntityPlaybackGroup(context)
    val libraryGroup = buildEntityLibraryGroup(context)
    val navigationGroup =
        if (isAlbum && author.isNotBlank() && context.onGoToArtist != null) {
            listOf(
                SongMenuEntry.Action(
                    id = "go_to_artist",
                    icon = Icons.Default.Person,
                    label = UiText.Resource(Res.string.track_menu_go_to_artist),
                    onClick = context.onGoToArtist,
                ),
            )
        } else {
            emptyList()
        }
    val shareGroup =
        listOfNotNull(
            context.onShare?.let { onShare ->
                SongMenuEntry.Action(
                    id = "share",
                    icon = Icons.Default.Share,
                    label = UiText.Resource(Res.string.common_action_share),
                    onClick = onShare,
                )
            },
        )
    return joinMenuGroups(playbackGroup, libraryGroup, navigationGroup, shareGroup)
}

private fun buildEntityPlaybackGroup(context: EntityMenuContext): List<SongMenuEntry> =
    buildList {
        context.onPlay?.let { onPlay ->
            add(
                SongMenuEntry.Action(
                    id = "play",
                    icon = Icons.Default.PlayArrow,
                    label = UiText.Resource(Res.string.menu_action_play),
                    onClick = onPlay,
                ),
            )
        }
        context.onShuffle?.let { onShuffle ->
            add(
                SongMenuEntry.Action(
                    id = "shuffle",
                    icon = Icons.Default.Shuffle,
                    label = UiText.Resource(Res.string.menu_action_shuffle),
                    onClick = onShuffle,
                ),
            )
        }
        context.onPlayNext?.let { onPlayNext ->
            add(
                SongMenuEntry.Action(
                    id = "play_next",
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    label = UiText.Resource(Res.string.track_menu_play_next),
                    onClick = onPlayNext,
                ),
            )
        }
        context.onAddToQueue?.let { onAddToQueue ->
            add(
                SongMenuEntry.Action(
                    id = "add_to_queue",
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    label = UiText.Resource(Res.string.common_action_add_to_queue),
                    tint = DarkMeloColors.menuQueueTint,
                    onClick = onAddToQueue,
                ),
            )
        }
    }

private fun buildEntityLibraryGroup(context: EntityMenuContext): List<SongMenuEntry> =
    listOfNotNull(
        context.onToggleSave?.let { onToggleSave ->
            SongMenuEntry.Action(
                id = "toggle_save",
                icon = if (context.isSaved) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd,
                label =
                    UiText.Resource(
                        if (context.isSaved) {
                            Res.string.track_menu_remove_from_library
                        } else {
                            Res.string.menu_action_save_to_library
                        },
                    ),
                onClick = onToggleSave,
            )
        },
    )

fun buildArtistMenuEntries(context: EntityMenuContext): List<SongMenuEntry> {
    val playbackGroup =
        buildList {
            context.onPlay?.let { onPlay ->
                add(
                    SongMenuEntry.Action(
                        id = "play",
                        icon = Icons.Default.PlayArrow,
                        label = UiText.Resource(Res.string.menu_action_play),
                        onClick = onPlay,
                    ),
                )
            }
            context.onStartRadio?.let { onStartRadio ->
                add(
                    SongMenuEntry.Action(
                        id = "start_radio",
                        icon = Icons.Default.Radio,
                        label = UiText.Resource(Res.string.menu_action_start_radio),
                        onClick = onStartRadio,
                    ),
                )
            }
        }
    val navigationGroup =
        buildList {
            context.onGoToArtist?.let { onGoToArtist ->
                add(
                    SongMenuEntry.Action(
                        id = "go_to_artist",
                        icon = Icons.Default.Person,
                        label = UiText.Resource(Res.string.track_menu_go_to_artist),
                        onClick = onGoToArtist,
                    ),
                )
            }
        }
    val shareGroup =
        buildList {
            context.onShare?.let { onShare ->
                add(
                    SongMenuEntry.Action(
                        id = "share",
                        icon = Icons.Default.Share,
                        label = UiText.Resource(Res.string.common_action_share),
                        onClick = onShare,
                    ),
                )
            }
        }
    return joinMenuGroups(playbackGroup, navigationGroup, shareGroup)
}

private fun joinMenuGroups(vararg groups: List<SongMenuEntry>): List<SongMenuEntry> {
    val nonEmpty = groups.filter { it.isNotEmpty() }
    return buildList {
        nonEmpty.forEachIndexed { index, group ->
            if (index > 0) add(SongMenuEntry.Divider)
            addAll(group)
        }
    }
}

@Composable
fun EntityContextMenu(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    entries: List<SongMenuEntry>,
    anchor: SongMenuAnchor?,
    onDismissRequest: () -> Unit,
) {
    val windowSize = rememberSongMenuWindowSize()
    val resolvedEntries = remember(entries) { entries }
    when (windowSize) {
        SongMenuWindowSize.Compact ->
            SongMenuCompactSheet(
                title = title,
                subtitle = subtitle,
                artworkUrl = artworkUrl,
                entries = resolvedEntries,
                onDismissRequest = onDismissRequest,
            )

        SongMenuWindowSize.Medium,
        SongMenuWindowSize.Expanded,
        ->
            SongMenuAnchoredPopup(
                entries = resolvedEntries,
                anchor = anchor,
                windowSize = windowSize,
                onDismissRequest = onDismissRequest,
            )
    }
}
