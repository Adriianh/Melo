package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track

/**
 * Common abstraction for Discord Rich Presence (RPC) integrations.
 *
 * Provides a platform-agnostic interface to connect to a local Discord client,
 * update the user's presence/activity with current playback information, and disconnect.
 */
interface DiscordRpcManager {
    /**
     * Initiates a connection to the local Discord client via IPC.
     */
    fun connect()

    /**
     * Updates the user's Discord activity with current playback status.
     *
     * @param track The currently active or playing [Track], or `null` if nothing is selected or playback is stopped.
     * @param playing Whether playback is currently active (`true`) or paused (`false`).
     * @param positionMs The current playback progress in milliseconds, used to compute start/end timestamps.
     */
    fun updateActivity(
        track: Track?,
        playing: Boolean,
        positionMs: Long? = null,
    )

    /**
     * Clears the current presence and closes the connection to the Discord client.
     */
    fun disconnect()

    /**
     * Releases any resources or background coroutines associated with the manager.
     */
    fun release()
}

/**
 * A no-op implementation of [DiscordRpcManager] used on platforms or configurations
 * where Discord Rich Presence is unavailable or disabled.
 */
@Suppress("EmptyFunctionBlock")
class NoOpDiscordRpcManager : DiscordRpcManager {
    override fun connect() {}

    override fun updateActivity(
        track: Track?,
        playing: Boolean,
        positionMs: Long?,
    ) {}

    override fun disconnect() {}

    override fun release() {}
}
