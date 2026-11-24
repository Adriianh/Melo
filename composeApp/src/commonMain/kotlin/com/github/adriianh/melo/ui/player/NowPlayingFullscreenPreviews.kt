@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.SyncedLine
import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.melo.theme.MeloTheme
import com.github.adriianh.melo.ui.player.components.FullscreenPlayerActions
import com.github.adriianh.melo.ui.player.components.NowPlayingFullscreenContent
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.MeloWindowSizeClass
import com.github.adriianh.melo.util.PlayerUiState
import com.github.adriianh.melo.util.WindowWidthClass

private val PREVIEW_ACCENT = Color(0xFFFF2D55)

private val PREVIEW_SYNCED_LYRICS =
    TrackLyrics(
        syncedLyrics =
            listOf(
                SyncedLine(timeMs = 1_000L, text = "Waiting in a car, waiting for a ride in the dark"),
                SyncedLine(timeMs = 5_000L, text = "The night city grows, look and see her eyes, they glow"),
                SyncedLine(
                    timeMs = 14_000L,
                    text = "Waiting in a car, waiting for the right time",
                    translation = "Esperando en un auto, esperando el momento ideal",
                ),
                SyncedLine(
                    timeMs = 18_500L,
                    text = "Waiting in a car, waiting for the right time",
                    translation = "Esperando en un auto, esperando el momento ideal",
                ),
                SyncedLine(timeMs = 23_000L, text = "Looking at the sky, neon lights are burning bright"),
            ),
        hasSync = true,
    )

private fun previewPlayerState(trackLyrics: TrackLyrics? = PREVIEW_SYNCED_LYRICS): PlayerUiState =
    PlayerUiState(
        title = "Midnight City",
        artist = "M83",
        albumArt = null,
        isPlaying = true,
        progressFraction = 0.42f,
        progressMs = 14_500L,
        durationMs = 243_000L,
        elapsedLabel = "1:42",
        remainingLabel = "2:21",
        shuffleEnabled = true,
        repeatMode = RepeatMode.ALL,
        hasNext = true,
        hasPrevious = true,
        accentColor = PREVIEW_ACCENT,
        trackLyrics = trackLyrics,
        lyrics = trackLyrics?.plainLyrics,
        isFavorite = true,
        showTranslation = false,
        targetLanguage = "es",
    )

@Composable
private fun FullscreenPreviewHost(
    width: Dp,
    height: Dp,
    sizeClass: MeloWindowSizeClass,
    isDesktop: Boolean,
    showLyrics: Boolean,
    showQueue: Boolean = false,
    reducedMotion: Boolean = false,
    positionMs: Long = 14_500L,
    state: PlayerUiState = previewPlayerState(),
) {
    MeloTheme(accentColor = PREVIEW_ACCENT, isDarkTheme = true) {
        Box(modifier = Modifier.size(width = width, height = height)) {
            NowPlayingBackground(
                artworkUrl = null,
                blurEnabled = false,
                accentColor = PREVIEW_ACCENT,
                extraDarkScrimAlpha = 0.28f,
            )
            NowPlayingFullscreenContent(
                state = state,
                positionMs = positionMs,
                progressFlow = null,
                volume = 0.72f,
                activeAccent = PREVIEW_ACCENT,
                sizeClass = sizeClass,
                isDesktop = isDesktop,
                reducedMotion = reducedMotion,
                showLyrics = showLyrics,
                showQueue = showQueue,
                controlsVisible = true,
                actions = FullscreenPlayerActions(onCollapse = {}),
                queueSidePanelContent = { PreviewQueuePlaceholder() },
            )
        }
    }
}

@Composable
private fun PreviewQueuePlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf(
            "Instant Crush" to "Daft Punk",
            "Nightcall" to "Kavinsky",
            "Resonance" to "HOME",
        ).forEach { (title, artist) ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(PREVIEW_ACCENT.copy(alpha = 0.35f)),
                )
                Column {
                    Text(title, style = MeloType.labelMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(artist, style = MeloType.labelSmall, color = Color.White.copy(alpha = 0.68f))
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewExpandedWithLyrics() {
    FullscreenPreviewHost(
        width = 1280.dp,
        height = 800.dp,
        sizeClass = MeloWindowSizeClass(WindowWidthClass.Expanded, isLandscape = true),
        isDesktop = true,
        showLyrics = true,
    )
}

@Preview
@Composable
private fun PreviewExpandedWithQueueOpen() {
    FullscreenPreviewHost(
        width = 1280.dp,
        height = 800.dp,
        sizeClass = MeloWindowSizeClass(WindowWidthClass.Expanded, isLandscape = true),
        isDesktop = true,
        showLyrics = true,
        showQueue = true,
    )
}

@Preview
@Composable
private fun PreviewExpandedWithoutLyrics() {
    FullscreenPreviewHost(
        width = 1280.dp,
        height = 800.dp,
        sizeClass = MeloWindowSizeClass(WindowWidthClass.Expanded, isLandscape = true),
        isDesktop = true,
        showLyrics = true,
        state = previewPlayerState(trackLyrics = null),
    )
}

@Preview
@Composable
private fun PreviewMediumPortraitLyrics() {
    FullscreenPreviewHost(
        width = 720.dp,
        height = 1024.dp,
        sizeClass = MeloWindowSizeClass(WindowWidthClass.Medium, isLandscape = false),
        isDesktop = false,
        showLyrics = true,
    )
}

@Preview
@Composable
private fun PreviewCompactLyrics() {
    FullscreenPreviewHost(
        width = 390.dp,
        height = 844.dp,
        sizeClass = MeloWindowSizeClass(WindowWidthClass.Compact, isLandscape = false),
        isDesktop = false,
        showLyrics = true,
    )
}

@Preview
@Composable
private fun PreviewCompactArtwork() {
    FullscreenPreviewHost(
        width = 390.dp,
        height = 844.dp,
        sizeClass = MeloWindowSizeClass(WindowWidthClass.Compact, isLandscape = false),
        isDesktop = false,
        showLyrics = false,
    )
}

@Preview
@Composable
private fun PreviewInstrumentalGap() {
    FullscreenPreviewHost(
        width = 1280.dp,
        height = 800.dp,
        sizeClass = MeloWindowSizeClass(WindowWidthClass.Expanded, isLandscape = true),
        isDesktop = true,
        showLyrics = true,
        positionMs = 9_500L,
    )
}

@Preview
@Composable
private fun PreviewReducedMotion() {
    FullscreenPreviewHost(
        width = 1280.dp,
        height = 800.dp,
        sizeClass = MeloWindowSizeClass(WindowWidthClass.Expanded, isLandscape = true),
        isDesktop = true,
        showLyrics = true,
        reducedMotion = true,
    )
}
