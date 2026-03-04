package com.github.adriianh.cli.tui.player

import javazoom.jl.player.advanced.AdvancedPlayer
import javazoom.jl.player.advanced.PlaybackEvent
import javazoom.jl.player.advanced.PlaybackListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Headless audio player backed by JLayer.
 * Streams audio directly from a URL without fully downloading it.
 *
 * Usage:
 *   player.play(url)
 *   player.pause()
 *   player.resume()
 *   player.stop()
 */
class AudioPlayer(
    private val scope: CoroutineScope,
    private val onProgress: (elapsedMs: Long) -> Unit = {},
    private val onFinish: () -> Unit = {},
    private val onError: (Throwable) -> Unit = {},
) {
    private var player: AdvancedPlayer? = null
    private var playJob: Job? = null

    private val isPaused = AtomicBoolean(false)
    private val isStopped = AtomicBoolean(false)

    private var startTimeMs: Long = 0L
    private var pausedAtMs: Long = 0L

    @Volatile private var currentUrl: String? = null

    val isPlaying: Boolean get() = playJob?.isActive == true && !isPaused.get()
    val isIdle: Boolean get() = playJob == null || playJob?.isCompleted == true

    fun play(url: String) {
        stop()
        currentUrl = url
        isPaused.set(false)
        isStopped.set(false)
        startTimeMs = System.currentTimeMillis()

        playJob = scope.launch(Dispatchers.IO) {
            try {
                val stream = BufferedInputStream(URI(url).toURL().openStream())
                val adv = AdvancedPlayer(stream)
                player = adv

                adv.playBackListener = object : PlaybackListener() {
                    override fun playbackStarted(evt: PlaybackEvent) {
                        startTimeMs = System.currentTimeMillis()
                    }
                    override fun playbackFinished(evt: PlaybackEvent) {
                        if (!isStopped.get()) onFinish()
                    }
                }

                val progressJob = scope.launch(Dispatchers.IO) {
                    while (!isStopped.get() && !isPaused.get()) {
                        val elapsed = pausedAtMs + (System.currentTimeMillis() - startTimeMs)
                        onProgress(elapsed)
                        kotlinx.coroutines.delay(500)
                    }
                }

                adv.play()
                progressJob.cancel()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun pause() {
        if (isPaused.get()) return
        isPaused.set(true)
        pausedAtMs += System.currentTimeMillis() - startTimeMs
        player?.stop()
        playJob?.cancel()
        playJob = null
    }

    fun resume() {
        val url = currentUrl ?: return
        if (!isPaused.get()) return
        isPaused.set(false)
        isStopped.set(false)
        startTimeMs = System.currentTimeMillis()

        playJob = scope.launch(Dispatchers.IO) {
            try {
                val stream = BufferedInputStream(URI(url).toURL().openStream())
                val adv = AdvancedPlayer(stream)
                player = adv

                adv.playBackListener = object : PlaybackListener() {
                    override fun playbackFinished(evt: PlaybackEvent) {
                        if (!isStopped.get()) onFinish()
                    }
                }

                val progressJob = scope.launch(Dispatchers.IO) {
                    while (!isStopped.get() && !isPaused.get()) {
                        val elapsed = pausedAtMs + (System.currentTimeMillis() - startTimeMs)
                        onProgress(elapsed)
                        kotlinx.coroutines.delay(500)
                    }
                }

                adv.play()
                progressJob.cancel()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun stop() {
        isStopped.set(true)
        isPaused.set(false)
        pausedAtMs = 0L
        player?.stop()
        player = null
        playJob?.cancel()
        playJob = null
        currentUrl = null
    }

    fun togglePlayPause() {
        when {
            isPaused.get() -> resume()
            isPlaying      -> pause()
        }
    }
}

