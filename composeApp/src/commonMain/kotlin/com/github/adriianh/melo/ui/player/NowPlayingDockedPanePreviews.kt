@file:Suppress("FunctionNaming", "LongMethod", "TooManyFunctions", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.SyncedLine
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.player.components.DockedArtistSectionContent
import com.github.adriianh.melo.ui.player.components.DockedLyricsPaneContent
import com.github.adriianh.melo.ui.player.components.DockedQueueSectionContent
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.PlayerUiState

private val PreviewAccentPink = Color(0xFFEC407A)
private val PreviewAccentCyan = Color(0xFF00E5FF)
private val PreviewAccentPurple = Color(0xFFAB47BC)

private fun previewTrack(
    id: String,
    title: String,
    artist: String,
    album: String = "Random Access Memories",
): Track =
    Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 240_000L,
        genres = emptyList(),
        artworkUrl = null,
        sourceId = null,
    )

private fun previewSampleLyrics(): TrackLyrics =
    TrackLyrics(
        plainLyrics = null,
        syncedLyrics =
            listOf(
                SyncedLine(timeMs = 6_000L, text = "We've come too far to give up who we are"),
                SyncedLine(timeMs = 12_000L, text = "So let's raise the bar and our cups to the stars"),
                SyncedLine(timeMs = 18_000L, text = ""),
                SyncedLine(timeMs = 25_000L, text = "She's up all night 'til the sun"),
                SyncedLine(timeMs = 31_000L, text = "I'm up all night to get some"),
            ),
    )

private fun previewArtistDetails(
    name: String = "Daft Punk",
    topSongs: List<Track> =
        listOf(
            previewTrack("t1", "Get Lucky", "Daft Punk feat. Pharrell Williams"),
            previewTrack("t2", "Instant Crush", "Daft Punk feat. Julian Casablancas"),
            previewTrack("t3", "One More Time", "Daft Punk"),
        ),
): SearchResult.Artist =
    SearchResult.Artist(
        id = "artist_daft_punk",
        name = name,
        artworkUrl = null,
        subscriberCountText = "2.96M subscribers",
        topSongs = topSongs,
    )

@Composable
private fun DockPreviewContainer(
    section: PanelSection,
    title: String = "Get Lucky",
    artist: String = "Daft Punk feat. Pharrell Williams",
    accent: Color = PreviewAccentPink,
    dockWidth: Dp = 310.dp,
    fontScale: Float = 1.0f,
    reducedMotion: Boolean = false,
) {
    val currentTrack = previewTrack("t1", title, artist)
    val state =
        PlayerUiState(
            currentTrack = currentTrack,
            title = title,
            artist = artist,
            isFavorite = true,
            isPlaying = true,
            accentColor = accent,
            trackLyrics = previewSampleLyrics(),
        )
    val upNext =
        listOf(
            previewTrack("q1", "Instant Crush (Extended Club Mix Version)", "Daft Punk feat. Julian Casablancas"),
            previewTrack("q2", "Lose Yourself to Dance", "Daft Punk"),
            previewTrack("q3", "Giorgio by Moroder", "Daft Punk"),
        )
    val suggestions =
        listOf(
            previewTrack("s1", "Starboy (Deluxe Remastered Edition)", "The Weeknd feat. Daft Punk"),
            previewTrack("s2", "Midnight City", "M83"),
        )
    val artistDetails =
        previewArtistDetails(
            name = artist,
            topSongs = listOf(currentTrack, upNext[0], upNext[1]),
        )

    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density = baseDensity.density, fontScale = fontScale),
    ) {
        MaterialTheme {
            Box(
                modifier =
                    Modifier
                        .height(720.dp)
                        .background(MeloColors.surface0)
                        .padding(12.dp),
            ) {
                DesktopNowPlayingDockedPaneContent(
                    state = state,
                    artistDetails = artistDetails,
                    activeAccent = accent,
                    selectedSection = section,
                    reducedMotion = reducedMotion,
                    dockWidth = dockWidth,
                    queueContent = { activeAccent ->
                        DockedQueueSectionContent(
                            currentTrackId = currentTrack.id,
                            isPlaying = true,
                            upNextTracks = upNext,
                            suggestions = suggestions,
                            isLoadingSuggestions = false,
                            activeAccent = activeAccent,
                            onPlayQueueTrack = {},
                            onRemoveQueueTrack = {},
                            onMoveQueueTrack = { _, _ -> },
                            onClearQueue = {},
                            onSaveAsPlaylist = {},
                            onRefreshSuggestions = {},
                            onPlaySuggestion = {},
                            onAddSuggestionToQueue = {},
                            initialExpanded = true,
                        )
                    },
                    lyricsContent = { activeAccent ->
                        DockedLyricsPaneContent(
                            trackLyrics = state.trackLyrics,
                            positionMs = 13_000L,
                            isLoading = false,
                            showTranslation = false,
                            isTranslating = false,
                            targetLanguage = "es",
                            activeAccent = activeAccent,
                            reducedMotion = reducedMotion,
                            onSeekTo = {},
                            onToggleTranslation = {},
                            onSelectLanguage = {},
                        )
                    },
                    artistContent = { activeAccent ->
                        DockedArtistSectionContent(
                            state = state,
                            artistDetails = artistDetails,
                            activeAccent = activeAccent,
                            onArtistClick = {},
                            onAlbumClick = {},
                            onTrackClick = {},
                        )
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewDockedPaneQueue() {
    DockPreviewContainer(section = PanelSection.QUEUE, accent = PreviewAccentPink)
}

@Preview
@Composable
private fun PreviewDockedPaneLyrics() {
    DockPreviewContainer(section = PanelSection.LYRICS, accent = PreviewAccentCyan)
}

@Preview
@Composable
private fun PreviewDockedPaneArtist() {
    DockPreviewContainer(section = PanelSection.ARTIST, accent = PreviewAccentPurple)
}

@Preview
@Composable
private fun PreviewDockedPaneQueueLongTitles() {
    DockPreviewContainer(
        section = PanelSection.QUEUE,
        title = "A Very Long Song Title That Must Truncate Gracefully With Ellipsis",
        artist = "Daft Punk, Pharrell Williams, Nile Rodgers & Friends Orchestra",
    )
}

@Preview
@Composable
private fun PreviewDockedPaneLyricsLongTitles() {
    DockPreviewContainer(
        section = PanelSection.LYRICS,
        title = "A Very Long Song Title That Must Truncate Gracefully With Ellipsis",
        artist = "Daft Punk, Pharrell Williams, Nile Rodgers & Friends Orchestra",
        accent = PreviewAccentCyan,
    )
}

@Preview
@Composable
private fun PreviewDockedPaneArtistLongTitles() {
    DockPreviewContainer(
        section = PanelSection.ARTIST,
        title = "A Very Long Song Title That Must Truncate Gracefully With Ellipsis",
        artist = "The Extremely Long Symphonic Electronic Collective Name",
        accent = PreviewAccentPurple,
    )
}

@Preview
@Composable
private fun PreviewDockedPaneLargeFontScale() {
    DockPreviewContainer(
        section = PanelSection.QUEUE,
        fontScale = 1.3f,
    )
}

@Preview
@Composable
private fun PreviewDockedPaneNarrowWidth() {
    DockPreviewContainer(
        section = PanelSection.QUEUE,
        dockWidth = 260.dp,
    )
}

@Preview
@Composable
private fun PreviewDockedPaneReducedMotionLyrics() {
    DockPreviewContainer(
        section = PanelSection.LYRICS,
        accent = PreviewAccentCyan,
        reducedMotion = true,
    )
}
