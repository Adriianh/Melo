package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.handler.loadMoreSimilar
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.cli.tui.handler.playback.seekToMs
import com.github.adriianh.cli.tui.handler.toggleFavorite
import com.github.adriianh.cli.tui.util.LrcParser
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

internal fun MeloScreen.handleLyricsTabKey(event: KeyEvent): EventResult {
    when {
        event.isCharIgnoreCase('a') -> {
            state = state.copy(
                detail = state.detail.copy(
                    isAutoScrollLyrics = true,
                    lyricsScrollOffset = 0
                )
            )
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) -> {
            val track = state.detail.selectedTrack
            val isNowPlaying = track != null && state.player.nowPlaying?.id == track.id
            val lines = if (isNowPlaying && state.player.syncedLyrics.isNotEmpty()) {
                state.player.syncedLyrics
            } else {
                state.detail.syncedLyrics
            }
            if (lines.isNotEmpty()) {
                if (isNowPlaying) {
                    val base = if (state.detail.isAutoScrollLyrics) {
                        LrcParser.currentLineIndex(lines, state.player.nowPlayingPositionMs)
                            .coerceAtLeast(0)
                    } else {
                        state.detail.lyricsScrollOffset
                    }
                    val maxOffset = lines.lastIndex
                    state = state.copy(
                        detail = state.detail.copy(
                            isAutoScrollLyrics = false,
                            lyricsScrollOffset = minOf(maxOffset, base + 1)
                        )
                    )
                } else {
                    val maxOffset = (lines.size - 1).coerceAtLeast(0)
                    state = state.copy(
                        detail = state.detail.copy(
                            lyricsScrollOffset = minOf(
                                maxOffset,
                                state.detail.lyricsScrollOffset + 1
                            )
                        )
                    )
                }
                return EventResult.HANDLED
            } else if (state.detail.lyrics != null) {
                return lyricsArea.handleKeyEvent(event, false)
            }
        }

        event.matches(Actions.MOVE_UP) -> {
            val track = state.detail.selectedTrack
            val isNowPlaying = track != null && state.player.nowPlaying?.id == track.id
            val lines = if (isNowPlaying && state.player.syncedLyrics.isNotEmpty()) {
                state.player.syncedLyrics
            } else {
                state.detail.syncedLyrics
            }
            if (lines.isNotEmpty()) {
                if (isNowPlaying) {
                    val base = if (state.detail.isAutoScrollLyrics) {
                        LrcParser.currentLineIndex(lines, state.player.nowPlayingPositionMs)
                            .coerceAtLeast(0)
                    } else {
                        state.detail.lyricsScrollOffset
                    }
                    state = state.copy(
                        detail = state.detail.copy(
                            isAutoScrollLyrics = false,
                            lyricsScrollOffset = maxOf(0, base - 1)
                        )
                    )
                } else {
                    state = state.copy(
                        detail = state.detail.copy(
                            lyricsScrollOffset = maxOf(0, state.detail.lyricsScrollOffset - 1)
                        )
                    )
                }
                return EventResult.HANDLED
            } else if (state.detail.lyrics != null) {
                return lyricsArea.handleKeyEvent(event, false)
            }
        }

        (event.isChar('T') || (event.modifiers()
            .ctrl() && event.isCharIgnoreCase('t'))) -> {
            openLanguagePicker()
            return EventResult.HANDLED
        }

        event.isChar('t') -> {
            cycleLyricsTranslation(isNowPlayingScreen = false)
            return EventResult.HANDLED
        }

        event.matches(Actions.SELECT) -> {
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            val isNowPlaying = track != null && state.player.nowPlaying?.id == track.id
            val lines = if (isNowPlaying && state.player.syncedLyrics.isNotEmpty()) {
                state.player.syncedLyrics
            } else {
                state.detail.syncedLyrics
            }
            if (state.detail.lyrics == null && lines.isEmpty()) {
                loadLyrics()
            } else if (!state.detail.isAutoScrollLyrics && lines.isNotEmpty()) {
                val targetLine = lines.getOrNull(state.detail.lyricsScrollOffset)
                if (isNowPlaying && targetLine != null) {
                    seekToMs(targetLine.timeMs)
                } else if (!isNowPlaying && track != null) {
                    playTrack(track)
                    if (targetLine != null) {
                        seekToMs(targetLine.timeMs)
                    }
                }
                state = state.copy(
                    detail = state.detail.copy(
                        isAutoScrollLyrics = true,
                        lyricsScrollOffset = 0
                    )
                )
            }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handleSimilarTabKey(event: KeyEvent): EventResult {
    when {
        event.matches(Actions.MOVE_DOWN) -> {
            val maxIndex = (state.detail.similarTracks.size - 1).coerceAtLeast(0)
            val newCursor = minOf(maxIndex, state.detail.similarCursor + 1)
            state = state.copy(detail = state.detail.copy(similarCursor = newCursor))
            if (newCursor >= state.detail.similarTracks.size - 3 && state.detail.hasMoreSimilar && !state.detail.isLoadingMoreSimilar) {
                loadMoreSimilar()
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            state = state.copy(
                detail = state.detail.copy(
                    similarCursor = maxOf(
                        0, state.detail.similarCursor - 1
                    )
                )
            )
            return EventResult.HANDLED
        }

        (event.code() == KeyCode.ENTER || event.matches(Actions.SELECT)) -> {
            state.detail.similarTracks.getOrNull(state.detail.similarCursor)?.let { playTrack(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('q') -> {
            state.detail.similarTracks.getOrNull(state.detail.similarCursor)?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('f') -> {
            state.detail.similarTracks.getOrNull(state.detail.similarCursor)
                ?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('o') -> {
            state.detail.similarTracks.getOrNull(state.detail.similarCursor)
                ?.let { openTrackOptions(it) }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handleInfoTabKey(event: KeyEvent): EventResult {
    when {
        (event.code() == KeyCode.ENTER || event.matches(Actions.SELECT)) -> {
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            track?.let { playTrack(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('q') -> {
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            track?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('f') -> {
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            track?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('o') -> {
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            track?.let { openTrackOptions(it) }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}