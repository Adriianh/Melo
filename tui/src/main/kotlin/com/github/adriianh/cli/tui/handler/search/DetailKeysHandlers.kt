package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.handleGlobalShortcuts
import com.github.adriianh.cli.tui.handler.isCtrlA
import com.github.adriianh.cli.tui.handler.isCtrlF
import com.github.adriianh.cli.tui.handler.loadMoreSimilar
import com.github.adriianh.cli.tui.handler.matchesAction
import com.github.adriianh.cli.tui.handler.openPlaylistPicker
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openBatchOptions
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playList
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.cli.tui.handler.playback.seekToMs
import com.github.adriianh.cli.tui.handler.resolveSimilarTracks
import com.github.adriianh.cli.tui.handler.toggleFavorite
import com.github.adriianh.cli.tui.isFavoriteEntity
import com.github.adriianh.cli.tui.util.LrcParser
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.FavoriteEntityType
import com.github.adriianh.core.domain.model.LyricsTranslationMode
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.core.domain.model.filterAndSortTracks
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.model.toFavoriteEntity
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.time.Duration.Companion.milliseconds


internal fun MeloScreen.returnFocusFromDetail() {
    val target = when (val curScreen = state.screen) {
        is ScreenState.Search -> "results-panel"
        is ScreenState.Home -> "home-panel"
        is ScreenState.Library -> "library-panel"
        is ScreenState.Offline -> "offline-panel"
        is ScreenState.Stats -> "stats-panel"
        is ScreenState.EntityDetail -> {
            if (curScreen.entity is SearchResult.Artist) "artist-dashboard-list" else "entity-tracks-list"
        }
        else -> "sidebar-nav"
    }
    appRunner()?.focusManager()?.setFocus(target)
}

internal fun MeloScreen.handleDetailKey(event: KeyEvent): EventResult {
    if (state.languagePicker.isVisible) return handleLanguagePickerKey(event)
    val focusedId = appRunner()?.focusManager()?.focusedId()
    if (focusedId != "detail-panel") {
        return handleGlobalShortcuts(event)
    }

    when {
        event.code() == KeyCode.ESCAPE || event.code() == KeyCode.TAB -> {
            returnFocusFromDetail()
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('i') -> {
            state = state.copy(detail = state.detail.copy(detailTab = DetailTab.INFO))
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('l') -> {
            state = state.copy(
                detail = state.detail.copy(
                    detailTab = DetailTab.LYRICS,
                    lyricsScrollOffset = 0,
                    isAutoScrollLyrics = true
                )
            )
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            if (track != null && state.detail.lyrics == null && state.detail.syncedLyrics.isEmpty() && !state.detail.isLoadingLyrics) {
                loadLyrics()
            }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('s') -> {
            state = state.copy(detail = state.detail.copy(detailTab = DetailTab.SIMILAR))
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            if (track != null && state.detail.similarTracks.isEmpty() && !state.detail.isLoadingSimilar) {
                loadTrackDetails(track.id, track)
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_LEFT) -> {
            val tabs = DetailTab.entries
            val prevOrdinal = (state.detail.detailTab.ordinal - 1 + tabs.size) % tabs.size
            val nextTab = tabs[prevOrdinal]
            state = state.copy(
                detail = state.detail.copy(
                    detailTab = nextTab,
                    lyricsScrollOffset = 0,
                    isAutoScrollLyrics = true
                )
            )
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            if (track != null) {
                if (nextTab == DetailTab.LYRICS && state.detail.lyrics == null && state.detail.syncedLyrics.isEmpty() && !state.detail.isLoadingLyrics) {
                    loadLyrics()
                } else if (nextTab == DetailTab.SIMILAR && state.detail.similarTracks.isEmpty() && !state.detail.isLoadingSimilar) {
                    loadTrackDetails(track.id, track)
                }
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_RIGHT) -> {
            val tabs = DetailTab.entries
            val nextOrdinal = (state.detail.detailTab.ordinal + 1) % tabs.size
            val nextTab = tabs[nextOrdinal]
            state = state.copy(
                detail = state.detail.copy(
                    detailTab = nextTab,
                    lyricsScrollOffset = 0,
                    isAutoScrollLyrics = true
                )
            )
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            if (track != null) {
                if (nextTab == DetailTab.LYRICS && state.detail.lyrics == null && state.detail.syncedLyrics.isEmpty() && !state.detail.isLoadingLyrics) {
                    loadLyrics()
                } else if (nextTab == DetailTab.SIMILAR && state.detail.similarTracks.isEmpty() && !state.detail.isLoadingSimilar) {
                    loadTrackDetails(track.id, track)
                }
            }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('a') && state.detail.detailTab == DetailTab.LYRICS -> {
            state = state.copy(
                detail = state.detail.copy(
                    isAutoScrollLyrics = true,
                    lyricsScrollOffset = 0
                )
            )
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) && state.detail.detailTab == DetailTab.LYRICS -> {
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

        event.matches(Actions.MOVE_UP) && state.detail.detailTab == DetailTab.LYRICS -> {
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
            .ctrl() && event.isCharIgnoreCase('t'))) && state.detail.detailTab == DetailTab.LYRICS -> {
            openLanguagePicker()
            return EventResult.HANDLED
        }

        event.isChar('t') && state.detail.detailTab == DetailTab.LYRICS -> {
            cycleLyricsTranslation(isNowPlayingScreen = false)
            return EventResult.HANDLED
        }

        event.matches(Actions.SELECT) && state.detail.detailTab == DetailTab.LYRICS -> {
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

        event.matches(Actions.MOVE_DOWN) && state.detail.detailTab == DetailTab.SIMILAR -> {
            val maxIndex = (state.detail.similarTracks.size - 1).coerceAtLeast(0)
            val newCursor = minOf(maxIndex, state.detail.similarCursor + 1)
            state = state.copy(detail = state.detail.copy(similarCursor = newCursor))
            if (newCursor >= state.detail.similarTracks.size - 3 && state.detail.hasMoreSimilar && !state.detail.isLoadingMoreSimilar) {
                loadMoreSimilar()
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) && state.detail.detailTab == DetailTab.SIMILAR -> {
            state = state.copy(
                detail = state.detail.copy(
                    similarCursor = maxOf(
                        0, state.detail.similarCursor - 1
                    )
                )
            )
            return EventResult.HANDLED
        }

        (event.code() == KeyCode.ENTER || event.matches(Actions.SELECT)) && state.detail.detailTab == DetailTab.INFO -> {
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            track?.let { playTrack(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('q') && state.detail.detailTab == DetailTab.INFO -> {
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            track?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('f') && state.detail.detailTab == DetailTab.INFO -> {
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            track?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('o') && state.detail.detailTab == DetailTab.INFO -> {
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            track?.let { openTrackOptions(it) }
            return EventResult.HANDLED
        }

        (event.code() == KeyCode.ENTER || event.matches(Actions.SELECT)) && state.detail.detailTab == DetailTab.SIMILAR -> {
            state.detail.similarTracks.getOrNull(state.detail.similarCursor)?.let { playTrack(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('q') && state.detail.detailTab == DetailTab.SIMILAR -> {
            state.detail.similarTracks.getOrNull(state.detail.similarCursor)?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('f') && state.detail.detailTab == DetailTab.SIMILAR -> {
            state.detail.similarTracks.getOrNull(state.detail.similarCursor)
                ?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('o') && state.detail.detailTab == DetailTab.SIMILAR -> {
            state.detail.similarTracks.getOrNull(state.detail.similarCursor)
                ?.let { openTrackOptions(it) }
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}
