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
import dev.tamboui.style.Style
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.Toolkit.panel
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

/** Native panel layout: top border + content row + bottom border. */
private const val TOAST_BOX_HEIGHT = 3

/** Blank rows between stacked toast boxes. */
private const val TOAST_GAP = 1

/**
 * Floating, non-interactive confirmation toasts rendered just above the player
 * bar. Each toast is a small bordered, centered box rendered with a native
 * TamboUI panel (rounded border and elevated background); up to
 * [com.github.adriianh.cli.tui.util.MAX_VISIBLE_TOASTS] stack from the bottom
 * (newest) up. Animation is derived from timestamps, so no per-frame state
 * mutation is required: the box fades + slides one row on entrance (the native
 * border glows in the accent color) and fades out on exit; periodic pruning
 * removes fully expired toasts.
 */
class ToastOverlay(
    private val stateProvider: () -> MeloState,
) : Element {

    /** Area painted last frame, cleared first so shrinking stacks leave no artifacts. */
    private var previousDrawnArea: Rect? = null

    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        previousDrawnArea?.let { frame.buffer().clear(it) }
        previousDrawnArea = null

        val now = System.currentTimeMillis()
        val alive = pruneExpiredToasts(stateProvider().toasts, now)
        if (alive.isEmpty()) return

        val terminalW = area.width()
        val toastW = (terminalW / 2).coerceIn(TOAST_MIN_WIDTH, TOAST_MAX_WIDTH)
        val x = area.x() + (terminalW - toastW) / 2
        val laneBaseY = area.height() - PLAYER_BAR_ROWS - 1

        var minRow = Int.MAX_VALUE
        var maxRow = -1

        alive.forEachIndexed { index, toast ->
            val isNewest = index == alive.lastIndex
            val phase = toastPhase(toast, now, isNewest)
            val offset = rowOffset(phase, toast, now)
            val boxesBelow = alive.size - 1 - index
            val contentRow = laneBaseY - boxesBelow * (TOAST_BOX_HEIGHT + TOAST_GAP) - 1 + offset
            val topRow = contentRow - 1
            val bottomRow = contentRow + 1

            minRow = minOf(minRow, topRow)
            maxRow = maxOf(maxRow, bottomRow)

            val kindColor = kindColor(toast.kind)
            val (iconColor, msgColor, borderColor) = phaseColors(phase, toast, now, kindColor)

            drawToast(frame, toastW, x, topRow, toast, iconColor, msgColor, borderColor, context)
        }

        previousDrawnArea = Rect(x, minRow, toastW, maxRow - minRow + 1)
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

private fun drawToast(
    frame: Frame,
    toastW: Int,
    x: Int,
    topRow: Int,
    toast: ToastMessage,
    iconColor: Color,
    msgColor: Color,
    borderColor: Color,
    context: RenderContext,
) {
    val icon = iconOf(toast.kind)
    val maxMsgLen = (toastW - 2 - (icon.length + 2) - 2).coerceAtLeast(4)
    val message = ellipsize(toast.message, maxMsgLen)
    val contentLen = icon.length + 2 + message.length
    val padLeft = ((toastW - 2 - contentLen) / 2).coerceAtLeast(1)
    val padRight = (toastW - 2 - contentLen - padLeft).coerceAtLeast(1)

    panel(
        row(
            text(" ".repeat(padLeft)),
            text(" $icon ").fg(iconColor).bold(),
            text(message).fg(msgColor),
            text(" ".repeat(padRight)),
        )
    )
        .rounded()
        .borderColor(borderColor)
        .style(Style.EMPTY.bg(BG_ELEVATED))
        .render(frame, Rect(x, topRow, toastW, TOAST_BOX_HEIGHT), context)
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

/**
 * Returns (iconColor, messageColor, borderColor) for the current phase.
 *
 * The border is always visible: it glows in the accent color while entering and
 * settles into a subtle [TEXT_DIM] tone the rest of the time (its default
 * [com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT] is identical to the
 * elevated fill in every theme, which made the box look borderless).
 */
private fun phaseColors(
    phase: ToastPhase,
    toast: ToastMessage,
    now: Long,
    kindColor: Color,
): Triple<Color, Color, Color> = when (phase) {
    ToastPhase.ACTIVE -> Triple(kindColor, TEXT_PRIMARY, TEXT_DIM)
    ToastPhase.ENTERING -> {
        val frac = (now - toast.shownAtMs).toFloat() / TOAST_FADE_IN_MS
        Triple(kindColor, fadeTier(frac), PRIMARY_COLOR)
    }

    ToastPhase.LEAVING -> {
        val frac = (now - toast.shownAtMs - toast.ttlMs).toFloat() / TOAST_FADE_OUT_MS
        Triple(kindColor, fadeTier(1f - frac), TEXT_DIM)
    }
}

private fun ellipsize(text: String, maxLen: Int): String =
    if (text.length <= maxLen) text else text.take((maxLen - 1).coerceAtLeast(1)) + "…"