package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_RED
import com.github.adriianh.cli.tui.MeloTheme.BG_ELEVATED
import com.github.adriianh.cli.tui.MeloTheme.ICON_CHECK
import com.github.adriianh.cli.tui.MeloTheme.ICON_ERROR
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_INFO
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.SECONDARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.util.TOAST_FADE_IN_MS
import com.github.adriianh.cli.tui.util.TOAST_FADE_OUT_MS
import com.github.adriianh.cli.tui.util.ToastKind
import com.github.adriianh.cli.tui.util.ToastMessage
import com.github.adriianh.cli.tui.util.ToastPhase
import com.github.adriianh.cli.tui.util.pruneExpiredToasts
import com.github.adriianh.cli.tui.util.toastPhase
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Rect
import dev.tamboui.style.Color
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.Size
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

/** Rows consumed by the player bar at the bottom of the screen. */
private const val PLAYER_BAR_ROWS = 4
private const val TOAST_MAX_WIDTH = 60
private const val TOAST_MIN_WIDTH = 28

/**
 * Floating, non-interactive confirmation strips ("toasts") rendered just above
 * the player bar. Every toast is a single filled row; up to [MAX_VISIBLE_TOASTS]
 * stack from the bottom (newest) up. Animation is derived from timestamps, so no
 * per-frame state mutation is required: entrance fades + slides one row, exit
 * fades out, and periodic pruning removes fully-expired toasts.
 */
class ToastOverlay(
    private val stateProvider: () -> MeloState,
) : Element {

    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val toasts = stateProvider().toasts
        if (toasts.isEmpty()) return

        val now = System.currentTimeMillis()
        val alive = pruneExpiredToasts(toasts, now)
        if (alive.isEmpty()) return

        val terminalW = area.width()
        val toastW = (terminalW / 2).coerceIn(TOAST_MIN_WIDTH, TOAST_MAX_WIDTH)
        val laneBaseY = area.height() - PLAYER_BAR_ROWS - 1
        val x = area.x() + (terminalW - toastW) / 2

        val firstRow = laneBaseY - (alive.size - 1) - 1
        val lastRow = laneBaseY + 1
        frame.buffer().clear(Rect(x, firstRow, toastW, lastRow - firstRow + 1))

        alive.forEachIndexed { index, toast ->
            val isNewest = index == alive.lastIndex
            val phase = toastPhase(toast, now, isNewest)
            val row = rowOffset(phase, toast, now) + laneBaseY - (alive.size - 1 - index)
            val kindColor = kindColor(toast.kind)
            val (iconColor, msgColor) = phaseColors(phase, toast, now, kindColor)

            val icon = iconOf(toast.kind)
            val iconCell = text(" $icon ")
                .fg(iconColor)
                .bold()
                .bg(BG_ELEVATED)
                .length(icon.length + 2)
            val msgWidth = (toastW - icon.length - 2).coerceAtLeast(4)
            val msgCell = text(ellipsize(toast.message, msgWidth))
                .fg(msgColor)
                .bg(BG_ELEVATED)
                .length(msgWidth)

            row(iconCell, msgCell)
                .render(frame, Rect(x, row, toastW, 1), context)
        }
    }

    override fun preferredSize(
        availableWidth: Int,
        availableHeight: Int,
        context: RenderContext
    ): Size = Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()

    /** Toasts are visual-only: never consume keys or mouse input. */
    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult =
        EventResult.UNHANDLED
}

private fun iconOf(kind: ToastKind): String = when (kind) {
    ToastKind.INFO -> ICON_INFO
    ToastKind.SUCCESS -> ICON_CHECK
    ToastKind.ERROR -> ICON_ERROR
    ToastKind.HEART -> ICON_HEART
}

private fun kindColor(kind: ToastKind): Color = when (kind) {
    ToastKind.INFO -> PRIMARY_COLOR
    ToastKind.SUCCESS -> SECONDARY_COLOR
    ToastKind.ERROR -> ACCENT_RED
    ToastKind.HEART -> PRIMARY_COLOR
}

/** Brightness tier used by the entrance/exit fades (3 discrete steps at tick cadence). */
private fun fadeTier(fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return when {
        f < 0.33f -> TEXT_DIM
        f < 0.66f -> TEXT_SECONDARY
        else -> TEXT_PRIMARY
    }
}

private fun rowOffset(phase: ToastPhase, toast: ToastMessage, now: Long): Int = when (phase) {
    ToastPhase.ENTERING -> {
        val frac = (now - toast.shownAtMs).toFloat() / TOAST_FADE_IN_MS
        if (frac < 0.5f) 1 else 0
    }

    ToastPhase.LEAVING -> -1
    ToastPhase.ACTIVE -> 0
}

/** Returns (iconColor, messageColor) for the current phase. */
private fun phaseColors(
    phase: ToastPhase,
    toast: ToastMessage,
    now: Long,
    kindColor: Color,
): Pair<Color, Color> = when (phase) {
    ToastPhase.ACTIVE -> kindColor to TEXT_PRIMARY
    ToastPhase.ENTERING -> {
        val frac = (now - toast.shownAtMs).toFloat() / TOAST_FADE_IN_MS
        kindColor to fadeTier(frac)
    }

    ToastPhase.LEAVING -> {
        val frac = (now - toast.shownAtMs - toast.ttlMs).toFloat() / TOAST_FADE_OUT_MS
        kindColor to fadeTier(1f - frac)
    }
}

private fun ellipsize(text: String, maxLen: Int): String =
    if (text.length <= maxLen) text else text.take((maxLen - 1).coerceAtLeast(1)) + "…"