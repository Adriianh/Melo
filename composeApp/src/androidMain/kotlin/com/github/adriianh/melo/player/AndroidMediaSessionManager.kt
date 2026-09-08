package com.github.adriianh.melo.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.launch

/**
 * Android MediaSession integration using Jetpack Media3.
 *
 * Exposes the media session to the Android system for:
 *   - Status bar and Lockscreen media controls (with seekbar and album art)
 *   - Bluetooth headset buttons (Play, Pause, Next, Previous)
 *   - Android Auto / Wear OS / System UI media tile
 */
class AndroidMediaSessionManager(
    private val context: Context,
    private val playbackManager: PlaybackManager,
    private val meloPlayer: MeloPlayer,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) : MediaSessionManager {

    var mediaSession: MediaSession? = null
        private set

    @Volatile
    private var initialized = false
    private var queueJob: Job? = null

    @OptIn(UnstableApi::class)
    override fun init() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val androidPlayer = meloPlayer as? AndroidMeloPlayer ?: return
            try {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val forwardingPlayer = object : ForwardingPlayer(androidPlayer.exoPlayer) {
                    override fun isCommandAvailable(command: Int): Boolean {
                        val queue = playbackManager.queueState.value
                        val hasNext = queue.currentIndex < queue.tracks.lastIndex
                        val hasPrev = queue.currentIndex > 0
                        val hasTrack = queue.currentTrack != null
                        return when (command) {
                            COMMAND_PLAY_PAUSE -> hasTrack || super.isCommandAvailable(command)
                            COMMAND_SEEK_TO_NEXT,
                            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> hasNext

                            COMMAND_SEEK_TO_PREVIOUS,
                            COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> hasPrev

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
                        if (androidPlayer.exoPlayer.mediaItemCount == 0 && playbackManager.queueState.value.currentTrack != null) {
                            playbackManager.togglePlayPause()
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

                val session = MediaSession.Builder(context, forwardingPlayer)
                    .setSessionActivity(pendingIntent)
                    .build()

                mediaSession = session

                val serviceIntent = Intent(context, MeloMediaSessionService::class.java)
                try {
                    context.startService(serviceIntent)
                } catch (_: Throwable) {
                }

                queueJob = scope.launch {
                    playbackManager.queueState.collect {
                        // ForwardingPlayer delegates command availability based on queueState
                    }
                }

                initialized = true
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    override fun release() {
        queueJob?.cancel()
        scope.cancel()
        try {
            mediaSession?.release()
        } catch (_: Throwable) {
        }
        mediaSession = null
        initialized = false
    }
}