package com.github.adriianh.melo.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.util.Log
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
 *
 * The service is intentionally self-sufficient: it asks [AndroidMediaSessionManager] for the
 * session (creating it if needed) instead of only reading an already-built one. That way a
 * system-initiated bind can still bring up the notification after the service was stopped.
 */
class MeloMediaSessionService : MediaSessionService() {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "melo_playback_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Melo Playback"

        private const val TAG = "MeloMediaSession"
    }

    private val mediaSessionManager: MediaSessionManager by inject()

    private val androidManager: AndroidMediaSessionManager?
        get() = mediaSessionManager as? AndroidMediaSessionManager

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        try {
            val notificationProvider =
                DefaultMediaNotificationProvider
                    .Builder(this)
                    .setChannelId(NOTIFICATION_CHANNEL_ID)
                    .build()
            setMediaNotificationProvider(notificationProvider)
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo configurar la notificación de reproducción", t)
        }

        attachSessionIfAvailable()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        attachSessionIfAvailable()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun attachSessionIfAvailable() {
        val session = androidManager?.ensureSession() ?: return
        if (sessions.contains(session)) return
        try {
            addSession(session)
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo adjuntar la MediaSession al servicio", t)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Melo media playback controls"
                    setShowBadge(false)
                }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        attachSessionIfAvailable()
        return androidManager?.mediaSession
    }

    override fun onDestroy() {
        androidManager?.mediaSession?.let { session ->
            try {
                removeSession(session)
            } catch (t: Throwable) {
                Log.w(TAG, "No se pudo desadjuntar la MediaSession del servicio", t)
            }
        }
        super.onDestroy()
    }
}
