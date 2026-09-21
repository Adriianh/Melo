package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track

/**
 * Events emitted by [PlaybackManager] so UI layers (TUI / Compose) can react
 * to playback milestones without duplicating resolution or orchestration logic.
 */
sealed interface PlaybackEvent {
    /** A track could not be resolved/played and the queue moved on (or stopped). */
    data class Error(val message: String) : PlaybackEvent

    /** A track successfully started loading into the player. */
    data class TrackStarted(
        val track: Track,
        val positionMs: Long = 0L,
    ) : PlaybackEvent
}