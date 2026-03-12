package com.github.adriianh.cli.tui

import com.github.adriianh.core.domain.model.ThemePreset
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

    val ICON_PLAY: String get() = if (supportsUnicode) "▶" else ">"
    val ICON_PAUSE: String get() = if (supportsUnicode) "⏸" else "||"
    val ICON_PREV: String get() = if (supportsUnicode) "⏮" else "|<"
    val ICON_NEXT: String get() = if (supportsUnicode) "⏭" else ">|"
    val ICON_LOADING: String get() = if (supportsUnicode) "⏳" else "..."
    val ICON_ERROR: String get() = if (supportsUnicode) "✗" else "x"
    val ICON_VOL_MUTE: String get() = if (supportsUnicode) "🔇" else "[m]"
    val ICON_VOL_LOW: String get() = if (supportsUnicode) "🔉" else "[<]"
    val ICON_VOL_HIGH: String get() = if (supportsUnicode) "🔊" else "[>]"
    val ICON_NOTE: String get() = if (supportsUnicode) "♫" else "~"
    val ICON_HEART: String get() = if (supportsUnicode) "♥" else "<3"
    val ICON_CHECK: String get() = if (supportsUnicode) "✓" else "ok"
    val ICON_BULLET: String get() = if (supportsUnicode) "•" else "*"
    val ICON_ARROW: String get() = if (supportsUnicode) "▸" else ">"
    val ICON_HOME: String get() = if (supportsUnicode) "🏠" else "[H]"
    val ICON_SEARCH: String get() = if (supportsUnicode) "🔍" else "[S]"
    val ICON_LIBRARY: String get() = if (supportsUnicode) "📚" else "[L]"
    val ICON_CLOCK: String get() = if (supportsUnicode) "🕘" else "[R]"
    val ICON_SHUFFLE: String get() = if (supportsUnicode) "⇄" else "[Z]"
    val ICON_REPEAT: String get() = if (supportsUnicode) "↻" else "[R]"
    val ICON_REPEAT1: String get() = if (supportsUnicode) "↺1" else "[R1]"
    val ICON_QUEUE: String get() = if (supportsUnicode) "≡" else "[Q]"
    val ICON_RADIO: String get() = if (supportsUnicode) "📻" else "[~]"
    val ICON_STATS: String get() = if (supportsUnicode) "📊" else "[#]"
    val ICON_NOW_PLAYING: String get() = if (supportsUnicode) "🎵" else "[N]"
    val ICON_SETTINGS: String get() = if (supportsUnicode) "⚙️" else "[S]"

    // Brand
    var PRIMARY_COLOR: Color = Color.hex("#8b2eb9")
    var SECONDARY_COLOR: Color = Color.hex("#1AA34A")

    // Backgrounds
    var BG_DARK: Color = Color.hex("#121212")
    var BG_CARD: Color = Color.hex("#181818")
    var BG_ELEVATED: Color = Color.hex("#282828")
    var BG_HIGHLIGHT: Color = Color.hex("#2A2A2A")

    // Text
    var TEXT_PRIMARY: Color = Color.WHITE
    var TEXT_SECONDARY: Color = Color.hex("#B3B3B3")
    var TEXT_DIM: Color = Color.hex("#535353")

    // Accents
    var ACCENT_RED: Color = Color.hex("#E22134")
    var ACCENT_BLUE: Color = Color.hex("#2E77D0")

    // Borders
    var BORDER_DEFAULT: Color = Color.hex("#333333")
    var BORDER_FOCUSED: Color = PRIMARY_COLOR

    // Styles (dynamic getters since colors can change)
    val TITLE_STYLE: Style get() = Style.EMPTY.bold().fg(TEXT_PRIMARY)
    val SUBTITLE_STYLE: Style get() = Style.EMPTY.fg(TEXT_SECONDARY)
    val DIM_STYLE: Style get() = Style.EMPTY.fg(TEXT_DIM)
    val HIGHLIGHT_STYLE: Style get() = Style.EMPTY.fg(PRIMARY_COLOR).bold()
    val NOW_PLAYING_STYLE: Style get() = Style.EMPTY.fg(PRIMARY_COLOR)

    fun loadTheme(preset: ThemePreset) {
        when (preset) {
            ThemePreset.DEFAULT -> {
                PRIMARY_COLOR = Color.hex("#8b2eb9")
                SECONDARY_COLOR = Color.hex("#1AA34A")
                BG_DARK = Color.hex("#121212")
                BG_CARD = Color.hex("#181818")
                BG_ELEVATED = Color.hex("#282828")
                BG_HIGHLIGHT = Color.hex("#2A2A2A")
                TEXT_PRIMARY = Color.WHITE
                TEXT_SECONDARY = Color.hex("#B3B3B3")
                TEXT_DIM = Color.hex("#535353")
                ACCENT_RED = Color.hex("#E22134")
                ACCENT_BLUE = Color.hex("#2E77D0")
                BORDER_DEFAULT = Color.hex("#333333")
                BORDER_FOCUSED = PRIMARY_COLOR
            }

            ThemePreset.CATPPUCCIN_MOCHA -> {
                PRIMARY_COLOR = Color.hex("#cba6f7")
                SECONDARY_COLOR = Color.hex("#a6e3a1")
                BG_DARK = Color.hex("#1e1e2e")
                BG_CARD = Color.hex("#181825")
                BG_ELEVATED = Color.hex("#313244")
                BG_HIGHLIGHT = Color.hex("#45475a")
                TEXT_PRIMARY = Color.hex("#cdd6f4")
                TEXT_SECONDARY = Color.hex("#bac2de")
                TEXT_DIM = Color.hex("#6c7086")
                ACCENT_RED = Color.hex("#f38ba8")
                ACCENT_BLUE = Color.hex("#89b4fa")
                BORDER_DEFAULT = Color.hex("#313244")
                BORDER_FOCUSED = PRIMARY_COLOR
            }

            ThemePreset.GRUVBOX -> {
                PRIMARY_COLOR = Color.hex("#d3869b")
                SECONDARY_COLOR = Color.hex("#b8bb26")
                BG_DARK = Color.hex("#282828")
                BG_CARD = Color.hex("#1d2021")
                BG_ELEVATED = Color.hex("#3c3836")
                BG_HIGHLIGHT = Color.hex("#504945")
                TEXT_PRIMARY = Color.hex("#ebdbb2")
                TEXT_SECONDARY = Color.hex("#a89984")
                TEXT_DIM = Color.hex("#928374")
                ACCENT_RED = Color.hex("#cc241d")
                ACCENT_BLUE = Color.hex("#458588")
                BORDER_DEFAULT = Color.hex("#3c3836")
                BORDER_FOCUSED = PRIMARY_COLOR
            }

            ThemePreset.NORD -> {
                PRIMARY_COLOR = Color.hex("#b48ead")
                SECONDARY_COLOR = Color.hex("#a3be8c")
                BG_DARK = Color.hex("#2e3440")
                BG_CARD = Color.hex("#242933")
                BG_ELEVATED = Color.hex("#3b4252")
                BG_HIGHLIGHT = Color.hex("#434c5e")
                TEXT_PRIMARY = Color.hex("#eceff4")
                TEXT_SECONDARY = Color.hex("#e5e9f0")
                TEXT_DIM = Color.hex("#4c566a")
                ACCENT_RED = Color.hex("#bf616a")
                ACCENT_BLUE = Color.hex("#81a1c1")
                BORDER_DEFAULT = Color.hex("#3b4252")
                BORDER_FOCUSED = PRIMARY_COLOR
            }

            ThemePreset.TOKYO_NIGHT -> {
                PRIMARY_COLOR = Color.hex("#bb9af7")
                SECONDARY_COLOR = Color.hex("#9ece6a")
                BG_DARK = Color.hex("#1a1b26")
                BG_CARD = Color.hex("#16161e")
                BG_ELEVATED = Color.hex("#292e42")
                BG_HIGHLIGHT = Color.hex("#414868")
                TEXT_PRIMARY = Color.hex("#c0caf5")
                TEXT_SECONDARY = Color.hex("#a9b1d6")
                TEXT_DIM = Color.hex("#565f89")
                ACCENT_RED = Color.hex("#f7768e")
                ACCENT_BLUE = Color.hex("#7aa2f7")
                BORDER_DEFAULT = Color.hex("#292e42")
                BORDER_FOCUSED = PRIMARY_COLOR
            }
        }
    }
}