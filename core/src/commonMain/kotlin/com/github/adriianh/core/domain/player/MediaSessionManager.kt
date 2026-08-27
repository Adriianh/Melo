package com.github.adriianh.core.domain.player

/**
 * Common abstraction for platform-specific OS media session integrations
 * (e.g., MPRIS/SMTC on Desktop via JMTC, MediaSession on Android via Media3, MPNowPlayingInfoCenter on iOS).
 */
interface MediaSessionManager {
    fun init()
    fun release()
}

class NoOpMediaSessionManager : MediaSessionManager {
    override fun init() {}
    override fun release() {}
}