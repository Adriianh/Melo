package com.github.adriianh.cli.tui.component.screen

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.checkIsFavorite
import com.github.adriianh.cli.tui.handler.onTrackProgress
import com.github.adriianh.cli.tui.handler.onTrackStarted
import com.github.adriianh.cli.tui.handler.playback.clearQueue
import com.github.adriianh.cli.tui.handler.playback.seekBackward
import com.github.adriianh.cli.tui.handler.playback.seekForward
import com.github.adriianh.cli.tui.handler.playback.togglePlayPause
import com.github.adriianh.cli.tui.handler.search.loadNowPlayingMetadata
import com.github.adriianh.cli.tui.handler.search.translateLyricsForTrack
import com.github.adriianh.cli.tui.util.LrcParser
import com.github.adriianh.cli.tui.util.ToastKind
import com.github.adriianh.core.domain.model.LyricsTranslationMode
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.PlaybackEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun MeloScreen.handleMediaSessionPlayPause() {
    try {
        appRunner()?.runOnRenderThread { togglePlayPause() } ?: playbackManager.togglePlayPause()
    } catch (_: Throwable) {}
}

internal fun MeloScreen.handleMediaSessionNext() {
    try {
        appRunner()?.runOnRenderThread { seekForward() } ?: playbackManager.playNext()
    } catch (_: Throwable) {}
}

internal fun MeloScreen.handleMediaSessionPrevious() {
    try {
        appRunner()?.runOnRenderThread { seekBackward() } ?: playbackManager.playPrevious()
    } catch (_: Throwable) {}
}

internal fun MeloScreen.handleMediaSessionStop() {
    try {
        appRunner()?.runOnRenderThread { clearQueue(showConfirmation = false) }
            ?: clearQueue(showConfirmation = false)
    } catch (_: Throwable) {}
}

/**
 * Subscribes the TUI player state to the shared PlaybackManager flows
 * (playbackState, queueState, volume, events). Replaces the old AudioPlayer
 * onProgress/onFinish/onError callbacks as the source of playback truth.
 */
internal fun MeloScreen.observePlaybackManager() {
    observePlaybackState()
    observeQueueState()
    observeVolumeState()
    observePlaybackEvents()
}

private fun MeloScreen.observePlaybackState() {
    scope.launch {
        playbackManager.playbackState.collect { ps ->
            appRunner()?.runOnRenderThread {
                val prev = state.player
                val durationMs =
                    ps.durationMs.takeIf { it > 0L } ?: prev.nowPlaying?.durationMs ?: 0L
                val progress =
                    if (durationMs > 0L) (ps.progressMs.toDouble() / durationMs).coerceIn(0.0, 1.0)
                    else 0.0
                val isLoading = when {
                    ps.error != null -> false
                    ps.isFinished -> false
                    ps.isPlaying -> false
                    ps.isBuffering -> true
                    prev.isPlaying && !ps.isPlaying -> false
                    else -> prev.isLoadingAudio
                }
                val wasPlaying = prev.isPlaying
                val nowPlaying = prev.nowPlaying
                state = state.copy(
                    player = prev.copy(
                        isPlaying = ps.isPlaying,
                        isLoadingAudio = isLoading,
                        nowPlayingPositionMs = ps.progressMs,
                        progress = progress,
                        audioError = if (ps.isPlaying) null else (ps.error ?: prev.audioError),
                    )
                )
                mediaSession.updatePosition(ps.progressMs)
                if (ps.error != null && wasPlaying) {
                    mediaSession.notifyStopped()
                }
                if (nowPlaying != null && wasPlaying && !ps.isPlaying) {
                    if (settingsViewState.currentSettings.discordRpcEnabled) {
                        discordRpcManager.updateActivity(nowPlaying, false)
                    }
                    mediaSession.notifyPaused()
                } else if (nowPlaying != null && !wasPlaying && ps.isPlaying) {
                    if (settingsViewState.currentSettings.discordRpcEnabled) {
                        discordRpcManager.updateActivity(nowPlaying, true, ps.progressMs)
                    }
                    mediaSession.notifyResumed()
                }
                if (ps.isPlaying && nowPlaying != null) {
                    onTrackProgress(nowPlaying, ps.progressMs, trackStartedAt)
                }
            }
        }
    }
}

private fun MeloScreen.observeQueueState() {
    scope.launch {
        playbackManager.queueState.collect { qs ->
            appRunner()?.runOnRenderThread {
                val prev = state.player
                state = state.copy(
                    player = prev.copy(
                        queue = qs.tracks,
                        queueIndex = qs.currentIndex,
                        nowPlaying = qs.currentTrack,
                        repeatMode = qs.repeatMode,
                        shuffleEnabled = qs.shuffleEnabled,
                        queueCursor = minOf(
                            prev.queueCursor,
                            (qs.tracks.size - 1).coerceAtLeast(0)
                        ),
                    )
                )
            }
        }
    }
}

private fun MeloScreen.observeVolumeState() {
    scope.launch {
        playbackManager.volume.collect { v ->
            appRunner()?.runOnRenderThread {
                state = state.copy(
                    player = state.player.copy(volume = (v * 100).toInt().coerceIn(0, 100))
                )
            }
        }
    }
}

private fun MeloScreen.observePlaybackEvents() {
    scope.launch {
        playbackManager.events.collect { event ->
            when (event) {
                is PlaybackEvent.Error -> {
                    appRunner()?.runOnRenderThread {
                        showToast(event.message.ifBlank { "Playback error" }, ToastKind.ERROR)
                        state = state.copy(
                            player = state.player.copy(
                                audioError = event.message,
                                isLoadingAudio = false,
                            )
                        )
                    }
                }

                is PlaybackEvent.TrackStarted -> handlePlaybackTrackStarted(event.track)
            }
        }
    }
}

/**
 * TUI-side side effects that used to live inside the playTrack handler
 * (metadata enrichment, lyrics, media session, discord RPC, favorites check,
 * history/auto-download) now hook onto the PM's TrackStarted event.
 */
private fun MeloScreen.handlePlaybackTrackStarted(track: Track) {
    marqueeTick = 0
    scrobbleSubmitted = false
    playRecorded = false
    trackStartedAt = System.currentTimeMillis()
    onTrackStarted(track)
    checkIsFavorite(track.id)
    loadNowPlayingMetadata(track)
    mediaSession.updateTrack(track, track.durationMs)
    if (settingsViewState.currentSettings.discordRpcEnabled) {
        discordRpcManager.updateActivity(track, true)
    }

    if (!state.isOfflineMode) {
        scope.launch(Dispatchers.IO) { markTrackAccessed(track.id) }
        if (settingsViewState.currentSettings.autoDownload) {
            val qs = playbackManager.queueState.value
            val nextTracks = (qs.currentIndex + 1 until minOf(qs.tracks.size, qs.currentIndex + 3))
                .mapNotNull { qs.tracks.getOrNull(it) }
            nextTracks.forEach { next ->
                scope.launch(Dispatchers.IO) { downloadTrack(next) }
            }
        }
    }

    scope.launch {
        try {
            val lrc = getSyncedLyrics(track.artist, track.title)
            appRunner()?.runOnRenderThread {
                if (state.player.nowPlaying?.id == track.id) {
                    val parsed = if (lrc != null) LrcParser.parse(lrc) else emptyList()
                    val isDetailTrack = state.detail.selectedTrack == null || state.detail.selectedTrack?.id == track.id
                    state = state.copy(
                        player = state.player.copy(
                            syncedLyrics = parsed,
                            isLoadingSyncedLyrics = false,
                        ),
                        detail = if (isDetailTrack) {
                            state.detail.copy(
                                syncedLyrics = parsed,
                                lyrics = parsed.joinToString("\n") { it.text },
                                plainLyricsTranslation = null,
                                isAutoScrollLyrics = true,
                                lyricsScrollOffset = 0,
                            )
                        } else state.detail
                    )
                    if (parsed.isNotEmpty() && state.player.lyricsTranslationMode != LyricsTranslationMode.ORIGINAL) {
                        translateLyricsForTrack(track)
                    }
                }
            }
        } catch (_: Exception) {
            appRunner()?.runOnRenderThread {
                state = state.copy(player = state.player.copy(isLoadingSyncedLyrics = false))
            }
        }
    }

    appRunner()?.runOnRenderThread {
        val isDetailTrack = state.detail.selectedTrack?.id == track.id
        state = state.copy(
            player = state.player.copy(
                audioError = null,
                isLoadingAudio = true,
                syncedLyrics = emptyList(),
                isLoadingSyncedLyrics = true,
                isTranslatingLyrics = false,
                nowPlayingArtwork = null,
                marqueeOffset = 0,
                nowPlayingPositionMs = 0L,
                progress = 0.0,
            ),
            screen = if (state.screen is ScreenState.NowPlaying) {
                (state.screen as ScreenState.NowPlaying).copy(
                    isAutoScrollLyrics = true,
                    lyricsScrollOffset = 0
                )
            } else state.screen,
            detail = if (isDetailTrack) {
                state.detail.copy(
                    syncedLyrics = emptyList(),
                    plainLyricsTranslation = null,
                    lyricsScrollOffset = 0,
                    isAutoScrollLyrics = true,
                    isTranslatingLyrics = false,
                )
            } else state.detail
        )
    }
}