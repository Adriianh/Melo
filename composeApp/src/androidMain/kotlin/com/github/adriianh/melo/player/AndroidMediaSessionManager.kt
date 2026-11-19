package com.github.adriianh.melo.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.github.adriianh.core.domain.player.AndroidMeloPlayer
import com.github.adriianh.core.domain.player.MediaSessionManager
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.melo.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Android MediaSession integration using Jetpack Media3.
 *
 * Exposes the media session to the Android system for:
 *   - Status bar and Lockscreen media controls (with seekbar and album art)
 *   - Bluetooth headset buttons (Play, Pause, Next, Previous)
 *   - Android Auto / Wear OS / System UI media tile
 *
 * The [MediaSession] and its [MeloMediaSessionService] have independent lifetimes:
 * the session belongs to the process, while the service can be stopped by the system
 * (idle-service reclaim, task removal, media3 no longer needing a foreground service).
 * The service is therefore re-armed from the playback state instead of being started
 * once at startup, so a stopped service is recovered on the next playback.
 */
class AndroidMediaSessionManager(
    private val context: Context,
    private val playbackManager: PlaybackManager,
    private val meloPlayer: MeloPlayer,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) : MediaSessionManager {
    companion object {
        private const val TAG = "MeloMediaSession"
    }

    @Volatile
    var mediaSession: MediaSession? = null
        private set

    private val sessionLock = Any()
    private var playbackJob: Job? = null

    override fun init() {
        ensureSession()
        observePlayback()
    }

    /**
     * Creates the [MediaSession] if it does not exist yet.
     *
     * Idempotent and retryable: unlike the previous one-shot `initialized` latch, a failed
     * creation does not poison the manager, and every playback start gets another attempt.
     */
    fun ensureSession(): MediaSession? {
        mediaSession?.let { return it }
        synchronized(sessionLock) {
            mediaSession?.let { return it }
            val androidPlayer = meloPlayer as? AndroidMeloPlayer ?: return null
            return try {
                val session =
                    MediaSession
                        .Builder(context, createForwardingPlayer(androidPlayer))
                        .setSessionActivity(buildSessionActivityIntent())
                        .build()
                mediaSession = session
                session
            } catch (t: Throwable) {
                Log.e(TAG, "No se pudo crear la MediaSession", t)
                null
            }
        }
    }

    /**
     * Starts [MeloMediaSessionService] so it can host the session notification.
     *
     * Deliberately not latched: `onStartCommand` re-attaches the session, so calling this on
     * every playback start is cheap and recovers a service the system stopped. A rejected
     * background start is logged instead of silently swallowed, and retried on the next event.
     */
    fun ensureServiceStarted() {
        try {
            context.startService(Intent(context, MeloMediaSessionService::class.java))
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo arrancar MeloMediaSessionService", t)
        }
    }

    private fun observePlayback() {
        if (playbackJob?.isActive == true) return
        playbackJob =
            scope.launch {
                playbackManager.playbackState
                    .map { it.isPlaying }
                    .distinctUntilChanged()
                    .collect { isPlaying ->
                        if (isPlaying) {
                            ensureSession()
                            ensureServiceStarted()
                        }
                    }
            }
    }

    private fun buildSessionActivityIntent(): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    @OptIn(UnstableApi::class)
    private fun createForwardingPlayer(androidPlayer: AndroidMeloPlayer): ForwardingPlayer =
        object : ForwardingPlayer(androidPlayer.exoPlayer) {
            override fun isCommandAvailable(command: Int): Boolean {
                val queue = playbackManager.queueState.value
                val hasNext = queue.currentIndex < queue.tracks.lastIndex
                val hasPrev = queue.currentIndex > 0
                val hasTrack = queue.currentTrack != null
                return when (command) {
                    COMMAND_PLAY_PAUSE -> hasTrack || super.isCommandAvailable(command)
                    COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> hasNext
                    COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> hasPrev
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun getAvailableCommands(): Player.Commands {
                val queue = playbackManager.queueState.value
                val hasNext = queue.currentIndex < queue.tracks.lastIndex
                val hasPrev = queue.currentIndex > 0
                val hasTrack = queue.currentTrack != null
                val builder = super.getAvailableCommands().buildUpon()
                if (hasTrack) {
                    builder.add(COMMAND_PLAY_PAUSE)
                }
                if (hasNext) {
                    builder.add(COMMAND_SEEK_TO_NEXT)
                    builder.add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                } else {
                    builder.remove(COMMAND_SEEK_TO_NEXT)
                    builder.remove(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                }
                if (hasPrev) {
                    builder.add(COMMAND_SEEK_TO_PREVIOUS)
                    builder.add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                } else {
                    builder.remove(COMMAND_SEEK_TO_PREVIOUS)
                    builder.remove(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                }
                return builder.build()
            }

            override fun play() {
                // A parked player has no media item (end of queue, stream unavailable), so
                // playing it is a no-op. Route the command through PlaybackManager, which can
                // resolve the stream again, outside the player callback to avoid re-entrancy.
                if (androidPlayer.exoPlayer.mediaItemCount == 0 && playbackManager.queueState.value.currentTrack != null) {
                    scope.launch { playbackManager.togglePlayPause() }
                } else {
                    super.play()
                }
            }

            override fun seekTo(positionMs: Long) {
                if (androidPlayer.exoPlayer.mediaItemCount == 0 && playbackManager.queueState.value.currentTrack != null) {
                    playbackManager.seekTo(positionMs)
                } else {
                    super.seekTo(positionMs)
                }
            }

            override fun seekToNext() {
                playbackManager.playNext()
            }

            override fun seekToNextMediaItem() {
                playbackManager.playNext()
            }

            override fun seekToPrevious() {
                playbackManager.playPrevious()
            }

            override fun seekToPreviousMediaItem() {
                playbackManager.playPrevious()
            }
        }

    override fun release() {
        playbackJob?.cancel()
        playbackJob = null
        scope.cancel()
        try {
            mediaSession?.release()
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo liberar la MediaSession", t)
        }
        mediaSession = null
    }
}
