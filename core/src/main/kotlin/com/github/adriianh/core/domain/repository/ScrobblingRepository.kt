package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.Track

interface ScrobblingRepository {
    /** Returns the stored Last.fm session key, or null if not authenticated. */
    fun getSessionKey(): String?

    /** Authenticate with Last.fm and persist the session key. Returns true on success. */
    suspend fun authenticate(username: String, password: String): Boolean

    /**
     * Requests a temporary token and returns the URL the user must open to authorize the app.
     * Returns null if the token could not be obtained.
     */
    suspend fun startWebAuth(): String?

    /**
     * Exchanges the given token for a permanent session key and persists it.
     * Must be called after the user has authorized the token in the browser.
     * Returns true on success.
     */
    suspend fun completeWebAuth(token: String): Boolean

    /** Notify Last.fm of the currently playing track. Fails silently. */
    suspend fun updateNowPlaying(track: Track)

    /** Scrobble a track. Should be called when playback reaches >50% or >4 minutes. */
    suspend fun scrobble(track: Track, startedAt: Long)
}