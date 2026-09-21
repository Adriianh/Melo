package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.util.LrcParser
import com.github.adriianh.core.domain.model.LyricsTranslationMode
import dev.tamboui.style.Overflow
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.MarkupTextAreaElement

internal fun renderLyricsTab(
    state: MeloState,
    lyricsArea: MarkupTextAreaElement,
    terminalHeight: Int = 30,
): StyledElement<*> {
    val track = state.detail.selectedTrack ?: state.player.nowPlaying
    val isNowPlaying = track != null && state.player.nowPlaying?.id == track.id
    val isLoading = state.detail.isLoadingLyrics ||
            (isNowPlaying && state.player.isLoadingSyncedLyrics && state.player.syncedLyrics.isEmpty())

    if (isLoading) {
        return column(
            spacer(),
            text("  Loading lyrics...").dim().centered(),
            spacer()
        )
    }

    val mode =
        if (isNowPlaying) state.player.lyricsTranslationMode else state.detail.lyricsTranslationMode
    val isTranslating =
        if (isNowPlaying) state.player.isTranslatingLyrics else state.detail.isTranslatingLyrics
    val isBilingual = mode == LyricsTranslationMode.BILINGUAL

    val syncedLines = if (isNowPlaying && state.player.syncedLyrics.isNotEmpty()) {
        state.player.syncedLyrics
    } else {
        state.detail.syncedLyrics
    }

    if (syncedLines.isNotEmpty()) {
        val activeIndex = if (isNowPlaying) {
            LrcParser.currentLineIndex(syncedLines, state.player.nowPlayingPositionMs)
        } else -1

        val centerIndex = if (isNowPlaying) {
            if (state.detail.isAutoScrollLyrics) {
                if (activeIndex >= 0) activeIndex else 0
            } else {
                state.detail.lyricsScrollOffset.coerceIn(0, syncedLines.lastIndex)
            }
        } else {
            state.detail.lyricsScrollOffset.coerceIn(0, syncedLines.lastIndex)
        }

        val rawWindow = (terminalHeight - 12).coerceIn(14, 50)
        val windowSize = if (isBilingual) (rawWindow / 2).coerceAtLeast(6) else rawWindow
        val half = windowSize / 2
        val start = (centerIndex - half).coerceAtLeast(0)
        val end = (start + windowSize).coerceAtMost(syncedLines.size)
        val visibleLines = syncedLines.subList(start, end)
        val visibleActiveIndex = if (activeIndex >= 0) activeIndex - start else -1
        val visibleCenterIndex = centerIndex - start

        val currentLangCode = state.languagePicker.currentLanguage.uppercase()
        val headerBadge = when {
            isTranslating -> text("● Translating lyrics ($currentLangCode)...").dim().centered()
            isNowPlaying -> {
                if (state.detail.isAutoScrollLyrics) {
                    when (mode) {
                        LyricsTranslationMode.BILINGUAL -> text("● SYNCED [Bilingual • $currentLangCode]").bold()
                            .fg(PRIMARY_COLOR).centered()

                        LyricsTranslationMode.TRANSLATION_ONLY -> text("● SYNCED [Translated • $currentLangCode]").bold()
                            .fg(PRIMARY_COLOR).centered()

                        LyricsTranslationMode.ORIGINAL -> text("● SYNCED").bold().fg(PRIMARY_COLOR)
                            .centered()
                    }
                } else {
                    text("↕ MANUAL SCROLL [Enter to seek, 'a' to sync]").dim().centered()
                }
            }

            else -> {
                val modeSuffix = when (mode) {
                    LyricsTranslationMode.BILINGUAL -> " [Bilingual • $currentLangCode]"
                    LyricsTranslationMode.TRANSLATION_ONLY -> " [Translated • $currentLangCode]"
                    LyricsTranslationMode.ORIGINAL -> ""
                }
                text("♫ Lyrics (${syncedLines.size} lines)$modeSuffix").dim().centered()
            }
        }

        val lineElements = mutableListOf<Element>()
        visibleLines.forEachIndexed { i, lrcLine ->
            val isCurrentPlaying = (i == visibleActiveIndex)
            val isManualFocused = (!state.detail.isAutoScrollLyrics && i == visibleCenterIndex)
            val isHighlighted = isCurrentPlaying || isManualFocused

            when (mode) {
                LyricsTranslationMode.ORIGINAL -> {
                    val displayText = lrcLine.text.ifBlank { "♪ ♫ ♪" }
                    val t = text(displayText).overflow(Overflow.WRAP_WORD)
                    val styled = when {
                        isHighlighted -> t.bold().fg(PRIMARY_COLOR).centered()
                        visibleActiveIndex >= 0 && i < visibleActiveIndex -> t.fg(TEXT_DIM)
                            .centered()

                        else -> t.fg(TEXT_SECONDARY).centered()
                    }
                    lineElements.add(styled)
                }

                LyricsTranslationMode.TRANSLATION_ONLY -> {
                    val displayText = lrcLine.translation?.takeIf { it.isNotBlank() }
                        ?: lrcLine.text.ifBlank { "♪ ♫ ♪" }
                    val t = text(displayText).overflow(Overflow.WRAP_WORD)
                    val styled = when {
                        isHighlighted -> t.bold().fg(PRIMARY_COLOR).centered()
                        visibleActiveIndex >= 0 && i < visibleActiveIndex -> t.fg(TEXT_DIM)
                            .centered()

                        else -> t.fg(TEXT_SECONDARY).centered()
                    }
                    lineElements.add(styled)
                }

                LyricsTranslationMode.BILINGUAL -> {
                    val displayText = lrcLine.text.ifBlank { "♪ ♫ ♪" }
                    val t = text(displayText).overflow(Overflow.WRAP_WORD)
                    val styledOrig = when {
                        isHighlighted -> t.bold().fg(PRIMARY_COLOR).centered()
                        visibleActiveIndex >= 0 && i < visibleActiveIndex -> t.fg(TEXT_DIM)
                            .centered()

                        else -> t.fg(TEXT_SECONDARY).centered()
                    }
                    lineElements.add(styledOrig)

                    val trans = lrcLine.translation
                    if (!trans.isNullOrBlank()) {
                        val sub = text("↳ $trans").overflow(Overflow.WRAP_WORD)
                        val styledSub = when {
                            isHighlighted -> sub.fg(TEXT_PRIMARY).dim().centered()
                            else -> sub.fg(TEXT_DIM).centered()
                        }
                        lineElements.add(styledSub)
                    }
                }
            }
        }

        return column(
            headerBadge.length(1),
            spacer(),
            *lineElements.toTypedArray(),
            spacer(),
            text("").length(1),
        ).fill()
    }

    return when {
        state.detail.lyrics != null -> {
            val textToDisplay = when (mode) {
                LyricsTranslationMode.ORIGINAL -> state.detail.lyrics
                LyricsTranslationMode.TRANSLATION_ONLY -> state.detail.plainLyricsTranslation
                    ?: state.detail.lyrics

                LyricsTranslationMode.BILINGUAL -> {
                    if (state.detail.plainLyricsTranslation != null) {
                        val origLines = state.detail.lyrics.lines()
                        val transLines = state.detail.plainLyricsTranslation.lines()
                        origLines.mapIndexed { idx, orig ->
                            val trans = transLines.getOrNull(idx)
                            if (!trans.isNullOrBlank()) "$orig\n  ↳ $trans" else orig
                        }.joinToString("\n")
                    } else {
                        state.detail.lyrics
                    }
                }
            }
            if (isTranslating) {
                val currentLangCode = state.languagePicker.currentLanguage.uppercase()
                column(
                    text("● Translating lyrics ($currentLangCode)...").dim().centered().length(1),
                    lyricsArea.markup(textToDisplay).fill()
                ).fill()
            } else {
                lyricsArea.markup(textToDisplay).fill()
            }
        }

        else -> column(
            spacer(),
            text("  Press Enter or 'l' to load lyrics").fg(TEXT_SECONDARY).centered(),
            spacer()
        )
    }
}