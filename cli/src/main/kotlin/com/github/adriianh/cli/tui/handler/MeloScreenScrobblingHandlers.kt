package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.launch

private const val SCROBBLE_MIN_MS = 4 * 60 * 1000L
private const val RECORD_PLAY_MIN_MS = 30 * 1000L

internal fun MeloScreen.onTrackStarted(track: Track) {
    updateNowPlayingJob?.cancel()
    updateNowPlayingJob = scope.launch { updateNowPlaying(track) }
}

internal fun MeloScreen.onTrackProgress(track: Track, elapsedMs: Long, startedAt: Long) {
    if (!playRecorded) {
        val playThreshold = minOf(track.durationMs / 4, RECORD_PLAY_MIN_MS)
        if (elapsedMs >= playThreshold) {
            playRecorded = true
            scope.launch { recordPlay(track) }
        }
    }

    if (scrobbleSubmitted) return
    val threshold = minOf(track.durationMs / 2, SCROBBLE_MIN_MS)
    if (elapsedMs < threshold) return
    scrobbleSubmitted = true
    scrobbleJob?.cancel()
    scrobbleJob = scope.launch { scrobble(track, startedAt) }
}