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


internal fun MeloScreen.loadNowPlayingMetadata(track: Track) {
    try {
        nowPlayingMetadataJob?.cancel()
    } catch (_: Exception) {
    }
    if (state.isOfflineMode) return
    nowPlayingMetadataJob = scope.launch {
        val resolvedMetadata = if (track.artworkUrl == null || track.album.isBlank()) {
            metadataProvider.resolveMetadata(track.title, track.artist)
        } else null

        val artworkUrl = resolvedMetadata?.artworkUrl ?: track.artworkUrl
        val album = resolvedMetadata?.album ?: track.album

        if (isActive) appRunner()?.runOnRenderThread {
            if (state.player.nowPlaying?.id == track.id) {
                val updatedTrack = state.player.nowPlaying?.copy(
                    artworkUrl = artworkUrl, album = album
                ) ?: track
                state = state.copy(
                    player = state.player.copy(
                        nowPlaying = updatedTrack
                    )
                )
                if (state.player.isPlaying) {
                    mediaSession.updateTrack(updatedTrack, updatedTrack.durationMs)
                }
            }
        }

        launch {
            val artwork = artworkUrl?.let {
                try {
                    artworkRenderer.load(it)
                } catch (_: Throwable) {
                    null
                }
            }

            if (isActive) appRunner()?.runOnRenderThread {
                if (state.player.nowPlaying?.id == track.id) {
                    state = state.copy(
                        player = state.player.copy(
                            nowPlayingArtwork = artwork
                        )
                    )

                    if (settingsViewState.currentSettings.discordRpcEnabled) {
                        discordRpcManager.updateActivity(
                            state.player.nowPlaying, state.player.isPlaying
                        )
                    }
                }
            }
        }
    }
}

internal fun MeloScreen.loadLyrics() {
    val track = state.detail.selectedTrack ?: state.player.nowPlaying ?: return
    if (state.isOfflineMode) {
        state = state.copy(
            detail = state.detail.copy(
                lyrics = "Lyrics are unavailable in Offline Mode",
                syncedLyrics = emptyList(),
                isLoadingLyrics = false
            )
        )
        return
    }
    state = state.copy(
        detail = state.detail.copy(
            isLoadingLyrics = true,
            lyrics = null,
            syncedLyrics = emptyList(),
            plainLyricsTranslation = null,
            isTranslatingLyrics = false,
            lyricsScrollOffset = 0,
            isAutoScrollLyrics = true
        )
    )
    scope.launch {
        val existingSynced =
            if (state.player.nowPlaying?.id == track.id && state.player.syncedLyrics.isNotEmpty()) {
                state.player.syncedLyrics
            } else null

        if (existingSynced != null) {
            appRunner()?.runOnRenderThread {
                val currentTrackId = state.detail.selectedTrack?.id ?: state.player.nowPlaying?.id
                if (currentTrackId == track.id) {
                    state = state.copy(
                        detail = state.detail.copy(
                            syncedLyrics = existingSynced,
                            lyrics = existingSynced.joinToString("\n") { it.text },
                            isLoadingLyrics = false,
                            lyricsScrollOffset = 0,
                            isAutoScrollLyrics = true
                        )
                    )
                    if (state.detail.lyricsTranslationMode != LyricsTranslationMode.ORIGINAL && existingSynced.none { it.translation != null }) {
                        translateLyricsForTrack(track)
                    }
                }
            }
            return@launch
        }

        val lrc = try {
            getSyncedLyrics(track.artist, track.title)
        } catch (_: Exception) {
            null
        }

        val parsed = if (!lrc.isNullOrBlank()) LrcParser.parse(lrc) else emptyList()

        if (parsed.isNotEmpty()) {
            appRunner()?.runOnRenderThread {
                val currentTrackId = state.detail.selectedTrack?.id ?: state.player.nowPlaying?.id
                if (currentTrackId == track.id) {
                    state = state.copy(
                        detail = state.detail.copy(
                            syncedLyrics = parsed,
                            lyrics = parsed.joinToString("\n") { it.text },
                            isLoadingLyrics = false,
                            lyricsScrollOffset = 0,
                            isAutoScrollLyrics = true
                        )
                    )
                    if (state.detail.lyricsTranslationMode != LyricsTranslationMode.ORIGINAL) {
                        translateLyricsForTrack(track)
                    }
                }
            }
        } else {
            val plainLyrics = try {
                getLyrics(track.artist, track.title)
            } catch (_: Exception) {
                null
            }
            appRunner()?.runOnRenderThread {
                val currentTrackId = state.detail.selectedTrack?.id ?: state.player.nowPlaying?.id
                if (currentTrackId == track.id) {
                    state = state.copy(
                        detail = state.detail.copy(
                            lyrics = plainLyrics ?: "Lyrics not found",
                            syncedLyrics = emptyList(),
                            isLoadingLyrics = false,
                            lyricsScrollOffset = 0,
                            isAutoScrollLyrics = true
                        )
                    )
                    if (state.detail.lyricsTranslationMode != LyricsTranslationMode.ORIGINAL && plainLyrics != null) {
                        translateLyricsForTrack(track)
                    }
                }
            }
        }
    }
}

internal fun MeloScreen.cycleLyricsTranslation(isNowPlayingScreen: Boolean) {
    val track = if (isNowPlayingScreen) state.player.nowPlaying else (state.detail.selectedTrack
        ?: state.player.nowPlaying)
    val currentMode =
        if (isNowPlayingScreen) state.player.lyricsTranslationMode else state.detail.lyricsTranslationMode
    val nextMode = currentMode.next()

    val isNowPlayingTrack = track != null && state.player.nowPlaying?.id == track.id

    state = state.copy(
        player = if (isNowPlayingTrack || isNowPlayingScreen) state.player.copy(
            lyricsTranslationMode = nextMode
        ) else state.player,
        detail = if (!isNowPlayingScreen || isNowPlayingTrack) state.detail.copy(
            lyricsTranslationMode = nextMode
        ) else state.detail
    )

    if (nextMode != LyricsTranslationMode.ORIGINAL && track != null) {
        val syncedLines = if (isNowPlayingTrack && state.player.syncedLyrics.isNotEmpty()) {
            state.player.syncedLyrics
        } else {
            state.detail.syncedLyrics
        }
        val hasMissingSyncedTrans =
            syncedLines.isNotEmpty() && syncedLines.none { it.translation != null }
        val hasMissingPlainTrans =
            syncedLines.isEmpty() && !state.detail.lyrics.isNullOrBlank() && state.detail.plainLyricsTranslation == null

        if (hasMissingSyncedTrans || hasMissingPlainTrans) {
            translateLyricsForTrack(track)
        }
    }
}

internal fun MeloScreen.translateLyricsForTrack(track: Track) {
    val translateUseCase = translateLyrics ?: return
    val targetLang = settingsViewState.currentSettings.searchLanguage.ifBlank { "es" }

    val isNowPlaying = state.player.nowPlaying?.id == track.id
    val isDetail = (state.detail.selectedTrack?.id ?: state.player.nowPlaying?.id) == track.id

    val isAlreadyTranslating =
        (isNowPlaying && state.player.isTranslatingLyrics) || (isDetail && state.detail.isTranslatingLyrics)
    if (isAlreadyTranslating) return

    val syncedLines = if (isNowPlaying && state.player.syncedLyrics.isNotEmpty()) {
        state.player.syncedLyrics
    } else {
        state.detail.syncedLyrics
    }
    val plain = state.detail.lyrics

    if (syncedLines.isEmpty() && plain.isNullOrBlank()) return

    state = state.copy(
        player = if (isNowPlaying) state.player.copy(isTranslatingLyrics = true) else state.player,
        detail = if (isDetail) state.detail.copy(isTranslatingLyrics = true) else state.detail
    )

    scope.launch(Dispatchers.IO) {
        val trackLyrics = TrackLyrics(
            plainLyrics = plain,
            syncedLyrics = syncedLines,
            hasSync = syncedLines.isNotEmpty()
        )
        val result = runCatching {
            translateUseCase(track.id, trackLyrics, targetLang)
        }.getOrNull()

        appRunner()?.runOnRenderThread {
            val stillNowPlaying = state.player.nowPlaying?.id == track.id
            val stillDetail =
                (state.detail.selectedTrack?.id ?: state.player.nowPlaying?.id) == track.id

            if (result != null) {
                state = state.copy(
                    player = if (stillNowPlaying) {
                        state.player.copy(
                            syncedLyrics = if (result.hasSync) result.syncedLyrics else state.player.syncedLyrics,
                            isTranslatingLyrics = false
                        )
                    } else state.player,
                    detail = if (stillDetail) {
                        state.detail.copy(
                            syncedLyrics = if (result.hasSync) result.syncedLyrics else state.detail.syncedLyrics,
                            plainLyricsTranslation = result.plainLyrics,
                            isTranslatingLyrics = false
                        )
                    } else state.detail
                )
            } else {
                state = state.copy(
                    player = if (stillNowPlaying) state.player.copy(isTranslatingLyrics = false) else state.player,
                    detail = if (stillDetail) state.detail.copy(isTranslatingLyrics = false) else state.detail
                )
            }
        }
    }
}

internal fun MeloScreen.openLanguagePicker() {
    val currentLang = settingsViewState.currentSettings.searchLanguage.ifBlank { "es" }
    val langs = state.languagePicker.languages
    val currentIdx = langs.indexOfFirst { it.first.equals(currentLang, ignoreCase = true) }
    state = state.copy(
        languagePicker = state.languagePicker.copy(
            isVisible = true,
            currentLanguage = currentLang,
            selectedIndex = if (currentIdx >= 0) currentIdx else 0
        )
    )
}

internal fun MeloScreen.selectLanguage(code: String) {
    state = state.copy(
        languagePicker = state.languagePicker.copy(
            isVisible = false,
            currentLanguage = code
        )
    )
    val currentLang = settingsViewState.currentSettings.searchLanguage.ifBlank { "es" }
    if (currentLang.equals(code, ignoreCase = true)) return

    val updatedSettings = settingsViewState.currentSettings.copy(searchLanguage = code)
    settingsViewState = settingsViewState.copy(currentSettings = updatedSettings)
    scope.launch(Dispatchers.IO) {
        updateSettings(updatedSettings)
    }

    val playerTrack = state.player.nowPlaying
    val isPlayerTranslating = state.player.lyricsTranslationMode != LyricsTranslationMode.ORIGINAL
    val resetPlayerSynced = state.player.syncedLyrics.map { it.copy(translation = null) }

    val detailTrack = state.detail.selectedTrack ?: state.player.nowPlaying
    val isDetailTranslating = state.detail.lyricsTranslationMode != LyricsTranslationMode.ORIGINAL
    val resetDetailSynced = state.detail.syncedLyrics.map { it.copy(translation = null) }

    state = state.copy(
        player = state.player.copy(
            syncedLyrics = resetPlayerSynced,
            isTranslatingLyrics = false
        ),
        detail = state.detail.copy(
            syncedLyrics = resetDetailSynced,
            plainLyricsTranslation = null,
            isTranslatingLyrics = false
        )
    )

    if (isPlayerTranslating && playerTrack != null) {
        translateLyricsForTrack(playerTrack)
    } else if (isDetailTranslating && detailTrack != null) {
        translateLyricsForTrack(detailTrack)
    }
}

internal fun MeloScreen.handleLanguagePickerKey(event: KeyEvent): EventResult {
    if (!state.languagePicker.isVisible) return EventResult.UNHANDLED
    val languages = state.languagePicker.languages
    when {
        event.matches(Actions.MOVE_UP) || event.isChar('k') -> {
            val newIdx = (state.languagePicker.selectedIndex - 1).coerceAtLeast(0)
            state = state.copy(languagePicker = state.languagePicker.copy(selectedIndex = newIdx))
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) || event.isChar('j') -> {
            val newIdx = (state.languagePicker.selectedIndex + 1).coerceAtMost(languages.lastIndex)
            state = state.copy(languagePicker = state.languagePicker.copy(selectedIndex = newIdx))
            return EventResult.HANDLED
        }

        event.matches(Actions.SELECT) -> {
            val selected = languages.getOrNull(state.languagePicker.selectedIndex)
            if (selected != null) {
                selectLanguage(selected.first)
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(languagePicker = state.languagePicker.copy(isVisible = false))
            return EventResult.HANDLED
        }
    }
    return EventResult.HANDLED
}
