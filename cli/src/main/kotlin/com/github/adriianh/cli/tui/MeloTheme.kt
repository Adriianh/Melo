package com.github.adriianh.cli.tui

import dev.tamboui.style.Color
import dev.tamboui.style.Style

/**
 * Spotify-inspired color theme for the Melo TUI.
 */
object MeloTheme {
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