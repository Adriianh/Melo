package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.LyricsActiveLineColor
import com.github.adriianh.core.domain.model.LyricsActiveLineWeight
import com.github.adriianh.core.domain.model.LyricsAlignment
import com.github.adriianh.core.domain.model.LyricsAnchorPosition
import com.github.adriianh.core.domain.model.LyricsAutoResumeDelay
import com.github.adriianh.core.domain.model.LyricsFontScale
import com.github.adriianh.core.domain.model.LyricsInactiveOpacity
import com.github.adriianh.core.domain.model.LyricsLineSpacing
import com.github.adriianh.core.domain.model.SyncedLine
import com.github.adriianh.core.domain.model.TrackLyrics
import kotlin.math.abs

data class NowPlayingLyricsConfig(
    val blurEnabled: Boolean = true,
    val activeLineZoom: Boolean = true,
    val fontScaleFactor: Float = LyricsFontScale.NORMAL.scaleFactor,
    val lineSpacingDp: Int = LyricsLineSpacing.NORMAL.spacingDp,
    val activeLineWeight: LyricsActiveLineWeight = LyricsActiveLineWeight.EXTRA_BOLD,
    val inactiveOpacity: Float = LyricsInactiveOpacity.NORMAL.baseAlpha,
    val alignment: LyricsAlignment = LyricsAlignment.START,
    val activeLineColor: LyricsActiveLineColor = LyricsActiveLineColor.WHITE,
    val anchorPosition: LyricsAnchorPosition = LyricsAnchorPosition.UPPER,
    val syncOffsetMs: Int = 0,
    val showSourceBadge: Boolean = true,
    val showInstrumentalBreaks: Boolean = true,
    val showCompactPreviewLine: Boolean = true,
    val autoResumeDelayMs: Long? = LyricsAutoResumeDelay.SECONDS_3.delayMs,
    val keepSyncButtonOnIdle: Boolean = true,
)

internal const val INSTRUMENTAL_GAP_THRESHOLD_MS = 4_000L
private const val LYRIC_SYNC_LEAD_MS = 150L

internal sealed interface FullscreenLyricEntry {
    val key: String
    val timeMs: Long

    data class LyricLine(
        val lineIndex: Int,
        val line: SyncedLine,
        override val key: String = "line_${line.timeMs}_$lineIndex",
    ) : FullscreenLyricEntry {
        override val timeMs: Long get() = line.timeMs
    }

    data class InstrumentalGap(
        val startTimeMs: Long,
        val endTimeMs: Long,
        val sourceLineIndex: Int? = null,
        override val key: String,
    ) : FullscreenLyricEntry {
        override val timeMs: Long get() = startTimeMs
    }
}

internal fun buildFullscreenLyricEntries(
    lines: List<SyncedLine>,
    gapThresholdMs: Long = INSTRUMENTAL_GAP_THRESHOLD_MS,
    includeInstrumentalBreaks: Boolean = true,
): List<FullscreenLyricEntry> {
    if (lines.isEmpty()) return emptyList()

    val entries = ArrayList<FullscreenLyricEntry>(lines.size + 4)
    val first = lines.first()
    if (includeInstrumentalBreaks && first.text.isNotBlank() && first.timeMs > gapThresholdMs) {
        entries +=
            FullscreenLyricEntry.InstrumentalGap(
                startTimeMs = 0L,
                endTimeMs = first.timeMs,
                sourceLineIndex = null,
                key = "gap_intro_0",
            )
    }

    for (index in lines.indices) {
        val line = lines[index]
        val next = lines.getOrNull(index + 1)

        if (line.text.isBlank()) {
            if (includeInstrumentalBreaks) {
                entries +=
                    FullscreenLyricEntry.InstrumentalGap(
                        startTimeMs = line.timeMs,
                        endTimeMs = next?.timeMs ?: Long.MAX_VALUE,
                        sourceLineIndex = index,
                        key = "gap_blank_${line.timeMs}_$index",
                    )
            }
        } else {
            entries +=
                FullscreenLyricEntry.LyricLine(
                    lineIndex = index,
                    line = line,
                )
        }
    }

    return entries
}

internal fun resolveActiveFullscreenEntryIndex(
    entries: List<FullscreenLyricEntry>,
    positionMs: Long,
    leadMs: Long = LYRIC_SYNC_LEAD_MS,
    syncOffsetMs: Int = 0,
): Int {
    if (entries.isEmpty()) return -1
    val adjustedMs = positionMs + leadMs + syncOffsetMs
    var active = -1
    for (index in entries.indices) {
        if (entries[index].timeMs <= adjustedMs) {
            active = index
        } else {
            break
        }
    }
    return active
}

internal fun TrackLyrics?.hasDisplayableLyrics(): Boolean = this != null && (hasSync || !plainLyrics.isNullOrBlank())

internal fun shouldShowLyricsFloatingControls(
    showFloatingControls: Boolean,
    hasLyrics: Boolean,
    showControlsOnHoverOnly: Boolean,
    isHovered: Boolean,
    hasFocus: Boolean,
    showLanguageMenu: Boolean,
    isIdleHidden: Boolean,
): Boolean =
    when {
        !showFloatingControls || !hasLyrics -> false
        showLanguageMenu -> true
        isIdleHidden -> false
        else -> !showControlsOnHoverOnly || isHovered || hasFocus
    }

internal val LocalIdleBypassRegistry = staticCompositionLocalOf<IdleBypassRegistry?> { null }

internal class IdleBypassRegistry {
    private val rects = mutableMapOf<String, Rect>()
    var rootCoordinates: LayoutCoordinates? = null

    fun register(
        key: String,
        coordinates: LayoutCoordinates,
    ) {
        val root = rootCoordinates ?: return
        if (root.isAttached && coordinates.isAttached) {
            rects[key] = root.localBoundingBoxOf(coordinates, clipBounds = false)
        }
    }

    fun unregister(key: String) {
        rects.remove(key)
    }

    fun contains(position: Offset?): Boolean = position != null && rects.values.any { it.contains(position) }
}

internal fun shouldShowResumeLyricsSyncButton(
    autoResumeDelayMs: Long?,
    userScrolledRecently: Boolean,
    activeEntryIndex: Int,
    keepSyncButtonOnIdle: Boolean,
    isIdleHidden: Boolean,
): Boolean =
    autoResumeDelayMs == null &&
        userScrolledRecently &&
        activeEntryIndex >= 0 &&
        (keepSyncButtonOnIdle || !isIdleHidden)

internal suspend fun LazyListState.animateResumeToLyric(
    activeEntryIndex: Int,
    entryCount: Int,
    estimatedActiveHeightPx: Int,
) {
    val visibleTarget = layoutInfo.visibleItemsInfo.firstOrNull { it.index == activeEntryIndex }
    if (visibleTarget != null) {
        animateScrollBy(
            value = (visibleTarget.offset + visibleTarget.size / 2).toFloat(),
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        )
        return
    }

    val firstVisible = firstVisibleItemIndex
    if (abs(activeEntryIndex - firstVisible) > 12) {
        val step = if (firstVisible > activeEntryIndex) 8 else -8
        scrollToItem(index = (activeEntryIndex + step).coerceIn(0, entryCount - 1))
    }

    val visibleItems = layoutInfo.visibleItemsInfo
    val avgPitchPx =
        if (visibleItems.isNotEmpty()) {
            visibleItems.sumOf { it.size }.toFloat() / visibleItems.size + layoutInfo.mainAxisItemSpacing
        } else {
            estimatedActiveHeightPx.toFloat()
        }
    animateScrollBy(
        value = (activeEntryIndex - firstVisibleItemIndex) * avgPitchPx,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
    )

    val settledTarget = layoutInfo.visibleItemsInfo.firstOrNull { it.index == activeEntryIndex }
    if (settledTarget != null) {
        animateScrollBy(
            value = (settledTarget.offset + settledTarget.size / 2).toFloat(),
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        )
    } else {
        animateScrollToItem(index = activeEntryIndex, scrollOffset = estimatedActiveHeightPx / 2)
    }
}

internal data class FullscreenLyricsStyle(
    val activeFontSize: TextUnit,
    val activeLineHeight: TextUnit,
    val neighborFontSize: TextUnit,
    val neighborLineHeight: TextUnit,
    val plainFontSize: TextUnit,
    val plainLineHeight: TextUnit,
    val translationFontSize: TextUnit,
    val translationLineHeight: TextUnit,
    val itemSpacing: Dp,
    val horizontalPadding: Dp,
) {
    companion object {
        val Expanded =
            FullscreenLyricsStyle(
                activeFontSize = 42.sp,
                activeLineHeight = 52.sp,
                neighborFontSize = 32.sp,
                neighborLineHeight = 42.sp,
                plainFontSize = 24.sp,
                plainLineHeight = 36.sp,
                translationFontSize = 18.sp,
                translationLineHeight = 25.sp,
                itemSpacing = 20.dp,
                horizontalPadding = 16.dp,
            )

        val Compact =
            FullscreenLyricsStyle(
                activeFontSize = 30.sp,
                activeLineHeight = 38.sp,
                neighborFontSize = 23.sp,
                neighborLineHeight = 31.sp,
                plainFontSize = 19.sp,
                plainLineHeight = 29.sp,
                translationFontSize = 15.sp,
                translationLineHeight = 21.sp,
                itemSpacing = 16.dp,
                horizontalPadding = 8.dp,
            )
    }
}

internal fun lyricAlphaForDistance(
    distance: Int,
    baseInactiveAlpha: Float = LyricsInactiveOpacity.NORMAL.baseAlpha,
): Float {
    val clampedBase = baseInactiveAlpha.coerceIn(0.15f, 0.90f)
    val ratio = clampedBase / LyricsInactiveOpacity.NORMAL.baseAlpha
    return when (distance) {
        0 -> 1.0f
        1 -> clampedBase
        2 -> (0.40f * ratio).coerceIn(0.12f, 0.85f)
        3 -> (0.30f * ratio).coerceIn(0.10f, 0.75f)
        else -> (0.20f * ratio).coerceIn(0.08f, 0.65f)
    }
}

internal fun LyricsActiveLineWeight.toComposeFontWeight(): FontWeight =
    when (this) {
        LyricsActiveLineWeight.NORMAL -> FontWeight.Normal
        LyricsActiveLineWeight.SEMIBOLD -> FontWeight.SemiBold
        LyricsActiveLineWeight.BOLD -> FontWeight.Bold
        LyricsActiveLineWeight.EXTRA_BOLD -> FontWeight.ExtraBold
    }

internal fun lyricBlurForDistance(distance: Int): Dp =
    when (distance) {
        0 -> 0.dp
        1 -> 0.8.dp
        2 -> 1.4.dp
        else -> 2.0.dp
    }

internal fun resolveFullscreenLyricsStyle(
    base: FullscreenLyricsStyle,
    config: NowPlayingLyricsConfig,
): FullscreenLyricsStyle {
    val scale = config.fontScaleFactor.coerceIn(0.75f, 1.40f)
    val scaledNeighborSize = (base.neighborFontSize.value * scale).sp
    val scaledNeighborHeight = (base.neighborLineHeight.value * scale).sp
    val scaledActiveSize =
        if (config.activeLineZoom) {
            (base.activeFontSize.value * scale).sp
        } else {
            scaledNeighborSize
        }
    val scaledActiveHeight =
        if (config.activeLineZoom) {
            (base.activeLineHeight.value * scale).sp
        } else {
            scaledNeighborHeight
        }
    return base.copy(
        activeFontSize = scaledActiveSize,
        activeLineHeight = scaledActiveHeight,
        neighborFontSize = scaledNeighborSize,
        neighborLineHeight = scaledNeighborHeight,
        plainFontSize = (base.plainFontSize.value * scale).sp,
        plainLineHeight = (base.plainLineHeight.value * scale).sp,
        translationFontSize = (base.translationFontSize.value * scale).sp,
        translationLineHeight = (base.translationLineHeight.value * scale).sp,
        itemSpacing = config.lineSpacingDp.coerceIn(8, 40).dp,
    )
}
