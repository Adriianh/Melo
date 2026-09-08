package com.github.adriianh.melo.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.github.adriianh.core.domain.player.MediaSessionManager
import org.koin.android.ext.android.inject

/**
 * MediaSessionService to provide persistent background playback
 * and system notification controls on Android.
 */
class MeloMediaSessionService : MediaSessionService() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "melo_playback_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Melo Playback"
    }

    private val mediaSessionManager: MediaSessionManager by inject()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        try {
            val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .build()
            setMediaNotificationProvider(notificationProvider)
        } catch (_: Throwable) {
        }

        attachSessionIfAvailable()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        attachSessionIfAvailable()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun attachSessionIfAvailable() {
        val session = (mediaSessionManager as? AndroidMediaSessionManager)?.mediaSession
        if (session != null && !sessions.contains(session)) {
            try {
                addSession(session)
            } catch (_: Throwable) {
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Melo media playback controls"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val session = (mediaSessionManager as? AndroidMediaSessionManager)?.mediaSession
        if (session != null && !sessions.contains(session)) {
            try {
                addSession(session)
            } catch (_: Throwable) {
            }
        }
        return session
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            val session = (mediaSessionManager as? AndroidMediaSessionManager)?.mediaSession
            session?.player?.pause()
        } catch (_: Throwable) {
        }
        stopSelf()
    }

    override fun onDestroy() {
        try {
            val session = (mediaSessionManager as? AndroidMediaSessionManager)?.mediaSession
            session?.player?.pause()
            if (session != null) {
                removeSession(session)
            }
        } catch (_: Throwable) {
        }
        super.onDestroy()
    }
}