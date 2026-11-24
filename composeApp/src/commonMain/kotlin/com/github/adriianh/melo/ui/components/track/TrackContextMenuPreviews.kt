@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.track

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.theme.MeloTheme
import com.github.adriianh.melo.util.MeloColors

private val previewTrack =
    Track(
        id = "yt:preview1",
        title = "Midnight City",
        artist = "M83",
        album = "Hurry Up, We're Dreaming",
        durationMs = 243_000L,
        genres = emptyList(),
        artworkUrl = null,
        sourceId = null,
    )

private val previewPlaylists =
    listOf(
        Playlist(id = 1L, name = "Late Night Drive", trackCount = 18, createdAt = 0L, artworks = emptyList()),
        Playlist(id = 2L, name = "Synthwave Essentials", trackCount = 42, createdAt = 0L, artworks = emptyList()),
        Playlist(id = 3L, name = "Focus Flow", trackCount = 11, createdAt = 0L, artworks = emptyList()),
    )

@Preview(widthDp = 360)
@Composable
private fun SongMenuCompactSheetPreview() {
    MeloTheme(isDarkTheme = true) {
        val entries =
            buildSongMenuEntries(
                track = previewTrack,
                context =
                    SongMenuContext(
                        onDownload = {},
                        onGoToArtist = {},
                        onGoToAlbum = {},
                        onShare = {},
                    ),
            )
        Surface(color = MeloColors.menuSheetSurface) {
            SongMenuCompactSheetContent(
                track = previewTrack,
                entries = entries,
                onDismissRequest = {},
            )
        }
    }
}

@Preview(widthDp = 380)
@Composable
private fun SongMenuMediumAnchoredPreview() {
    MeloTheme(isDarkTheme = true) {
        val entries =
            buildSongMenuEntries(
                track = previewTrack,
                context =
                    SongMenuContext(
                        onDownload = {},
                        onGoToArtist = {},
                        onGoToAlbum = {},
                        onShare = {},
                    ),
            )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MeloColors.surface0)
                    .padding(16.dp),
        ) {
            SongMenuPopupSurface(
                entries = entries,
                isExpandedDesktop = false,
                onDismissRequest = {},
            )
        }
    }
}

@Preview(widthDp = 620)
@Composable
private fun SongMenuExpandedWithSubmenuPreview() {
    MeloTheme(isDarkTheme = true) {
        val entries =
            buildSongMenuEntries(
                track = previewTrack,
                context =
                    SongMenuContext(
                        playlists = previewPlaylists,
                        containingPlaylistIds = setOf(2L),
                        onDownload = {},
                        onGoToArtist = {},
                        onGoToAlbum = {},
                        onShare = {},
                    ),
            )
        val submenuContent =
            entries.filterIsInstance<SongMenuEntry.Submenu>().first().content

        Row(
            modifier =
                Modifier
                    .background(MeloColors.surface0)
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SongMenuPopupSurface(
                entries = entries,
                isExpandedDesktop = true,
                onDismissRequest = {},
            )
            PlaylistFlyoutContent(
                content = submenuContent,
                onDismissSubmenu = {},
                onDismissAll = {},
            )
        }
    }
}

@Preview(widthDp = 340)
@Composable
private fun SongMenuDestructiveContextualPreview() {
    MeloTheme(isDarkTheme = true) {
        val entries =
            buildSongMenuEntries(
                track = previewTrack,
                context =
                    SongMenuContext(
                        isOnAlbumScreen = true,
                        onDownload = {},
                        onGoToArtist = {},
                        onGoToAlbum = {},
                        onShare = {},
                        onRemoveFromPlaylist = {},
                    ),
            )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MeloColors.surface0)
                    .padding(16.dp),
        ) {
            SongMenuPopupSurface(
                entries = entries,
                isExpandedDesktop = true,
                onDismissRequest = {},
            )
        }
    }
}

@Preview(widthDp = 340)
@Composable
private fun SongMenuLikedAndDownloadedPreview() {
    MeloTheme(isDarkTheme = true) {
        val entries =
            buildSongMenuEntries(
                track = previewTrack,
                context =
                    SongMenuContext(
                        isLiked = true,
                        isDownloaded = true,
                        inQueue = true,
                        onRemoveFromQueue = {},
                        onDeleteDownload = {},
                        onGoToArtist = {},
                        onGoToAlbum = {},
                        onShare = {},
                    ),
            )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MeloColors.surface0)
                    .padding(16.dp),
        ) {
            SongMenuPopupSurface(
                entries = entries,
                isExpandedDesktop = true,
                onDismissRequest = {},
            )
        }
    }
}
