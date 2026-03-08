package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.*

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.launch

private const val SCROBBLE_MIN_MS = 4 * 60 * 1000L

internal fun MeloScreen.onTrackStarted(track: Track) {
    scope.launch { updateNowPlaying(track) }
}

internal fun MeloScreen.onTrackProgress(track: Track, elapsedMs: Long, startedAt: Long) {
    if (scrobbleSubmitted) return
    val threshold = minOf(track.durationMs / 2, SCROBBLE_MIN_MS)
    if (elapsedMs < threshold) return
    scrobbleSubmitted = true
    scope.launch { scrobble(track, startedAt) }
}