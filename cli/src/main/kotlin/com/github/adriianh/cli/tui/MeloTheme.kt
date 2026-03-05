package com.github.adriianh.cli.tui

import dev.tamboui.style.Color
import dev.tamboui.style.Style

/**
 * Color theme for the Melo TUI.
 */
object MeloTheme {
    /**
     * True if the terminal likely supports Unicode/UTF-8 rendering.
     * Requires both a UTF-8 locale AND a terminal known to render Unicode correctly.
     * Plain `xterm` without a Unicode font is excluded even if LANG is UTF-8.
     */
    val supportsUnicode: Boolean by lazy {
        val charset = System.out.charset().name()
        val lang = listOfNotNull(
            System.getenv("LANG"),
            System.getenv("LC_ALL"),
            System.getenv("LC_CTYPE"),
        ).joinToString(" ").uppercase()
        val hasUtfLocale = charset.contains("UTF", ignoreCase = true) || lang.contains("UTF")

        val term = System.getenv("TERM") ?: ""
        val termProgram = System.getenv("TERM_PROGRAM") ?: ""
        val colorTerm = System.getenv("COLORTERM") ?: ""
        val vteVersion = System.getenv("VTE_VERSION")
        val wtSession = System.getenv("WT_SESSION")
        val kitty = System.getenv("KITTY_WINDOW_ID")

        val isKnownGoodTerminal = colorTerm.isNotEmpty()
            || vteVersion != null
            || termProgram.isNotEmpty()
            || wtSession != null
            || kitty != null
            || term.startsWith("xterm-256color")
            || term == "screen-256color"
            || term == "tmux-256color"
            || term.startsWith("rxvt-unicode")

        hasUtfLocale && isKnownGoodTerminal
    }

    val ICON_PLAY: String get() = if (supportsUnicode) "▶"  else ">"
    val ICON_PAUSE: String get() = if (supportsUnicode) "⏸"  else "||"
    val ICON_PREV: String get() = if (supportsUnicode) "⏮"  else "|<"
    val ICON_NEXT: String get() = if (supportsUnicode) "⏭"  else ">|"
    val ICON_LOADING: String get() = if (supportsUnicode) "⏳"  else "..."
    val ICON_ERROR: String get() = if (supportsUnicode) "✗"   else "x"
    val ICON_VOL_MUTE: String get() = if (supportsUnicode) "🔇"  else "[m]"
    val ICON_VOL_LOW: String get() = if (supportsUnicode) "🔉"  else "[<]"
    val ICON_VOL_HIGH: String get() = if (supportsUnicode) "🔊"  else "[>]"
    val ICON_NOTE: String get() = if (supportsUnicode) "♫"   else "~"
    val ICON_HEART: String get() = if (supportsUnicode) "♥"   else "<3"
    val ICON_CHECK: String get() = if (supportsUnicode) "✓"   else "ok"
    val ICON_BULLET: String get() = if (supportsUnicode) "•"   else "*"
    val ICON_ARROW: String get() = if (supportsUnicode) "▸"   else ">"
    val ICON_HOME:     String get() = if (supportsUnicode) "🏠"  else "[H]"
    val ICON_SEARCH:   String get() = if (supportsUnicode) "🔍"  else "[S]"
    val ICON_LIBRARY:  String get() = if (supportsUnicode) "📚"  else "[L]"
    val ICON_CLOCK:    String get() = if (supportsUnicode) "🕘"  else "[R]"

    // Brand
    val PRIMARY_COLOR: Color = Color.hex("#8b2eb9")
    val SECONDARY_COLOR: Color = Color.hex("#1AA34A")

    // Backgrounds
    val BG_DARK: Color = Color.hex("#121212")
    val BG_CARD: Color = Color.hex("#181818")
    val BG_ELEVATED: Color = Color.hex("#282828")
    val BG_HIGHLIGHT: Color = Color.hex("#2A2A2A")

    // Text
    val TEXT_PRIMARY: Color = Color.WHITE
    val TEXT_SECONDARY: Color = Color.hex("#B3B3B3")
    val TEXT_DIM: Color = Color.hex("#535353")

    // Accents
    val ACCENT_RED: Color = Color.hex("#E22134")
    val ACCENT_BLUE: Color = Color.hex("#2E77D0")

    // Borders
    val BORDER_DEFAULT: Color = Color.hex("#333333")
    val BORDER_FOCUSED: Color = PRIMARY_COLOR

    // Styles
    val TITLE_STYLE: Style = Style.EMPTY.bold().fg(TEXT_PRIMARY)
    val SUBTITLE_STYLE: Style = Style.EMPTY.fg(TEXT_SECONDARY)
    val DIM_STYLE: Style = Style.EMPTY.fg(TEXT_DIM)
    val HIGHLIGHT_STYLE: Style = Style.EMPTY.fg(PRIMARY_COLOR).bold()
    val NOW_PLAYING_STYLE: Style = Style.EMPTY.fg(PRIMARY_COLOR)
}