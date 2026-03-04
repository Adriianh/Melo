package com.github.adriianh.cli.tui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

/**
 * Audio player that decodes any stream URL via ffmpeg and outputs PCM to javax.sound.sampled.
 * Supports all formats yt-dlp may return: opus/webm, m4a/aac, mp3, etc.
 *
 * Requires ffmpeg to be installed on the system.
 */
class AudioPlayer(
    private val scope: CoroutineScope,
    private val onProgress: (elapsedMs: Long) -> Unit = {},
    private val onFinish: () -> Unit = {},
    private val onError: (Throwable) -> Unit = {},
) {
    private var playJob: Job? = null
    private var ffmpegProcess: Process? = null
    private var audioLine: SourceDataLine? = null

    private val isPaused = AtomicBoolean(false)
    private val isStopped = AtomicBoolean(false)

    @Volatile private var currentUrl: String? = null
    @Volatile private var startTimeMs: Long = 0L
    @Volatile private var pausedAtMs: Long = 0L

    val isPlaying: Boolean get() = playJob?.isActive == true && !isPaused.get()

    private val pcmFormat = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        44100f, 16, 2, 4, 44100f, false
    )

    fun play(url: String) {
        stop()
        currentUrl = url
        isPaused.set(false)
        isStopped.set(false)
        startTimeMs = System.currentTimeMillis()
        pausedAtMs = 0L

        playJob = scope.launch(Dispatchers.IO) {
            try {
                startPlayback(url)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isStopped.get()) onError(e)
            }
        }
    }

    private suspend fun startPlayback(url: String) {
        val ffmpeg = buildFfmpegProcess(url)
        ffmpegProcess = ffmpeg

        val info = DataLine.Info(SourceDataLine::class.java, pcmFormat)
        val line = AudioSystem.getLine(info) as SourceDataLine
        audioLine = line
        line.open(pcmFormat)
        line.start()

        startTimeMs = System.currentTimeMillis()

        val progressJob = scope.launch(Dispatchers.IO) {
            while (isActive && !isStopped.get()) {
                if (!isPaused.get()) {
                    val elapsed = pausedAtMs + (System.currentTimeMillis() - startTimeMs)
                    onProgress(elapsed)
                }
                delay(500)
            }
        }

        val buffer = ByteArray(8192)
        val stream = ffmpeg.inputStream
        try {
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                if (isStopped.get()) break
                line.write(buffer, 0, read)
            }
        } finally {
            progressJob.cancel()
            line.drain()
            line.stop()
            line.close()
            ffmpeg.destroy()
            audioLine = null
            ffmpegProcess = null
            if (!isStopped.get()) onFinish()
        }
    }

    fun pause() {
        if (isPaused.get() || !isPlaying) return
        isPaused.set(true)
        pausedAtMs += System.currentTimeMillis() - startTimeMs
        audioLine?.stop()
    }

    fun resume() {
        if (!isPaused.get()) return
        isPaused.set(false)
        startTimeMs = System.currentTimeMillis()
        audioLine?.start()
    }

    fun stop() {
        isStopped.set(true)
        isPaused.set(false)
        audioLine?.stop()
        audioLine?.close()
        audioLine = null
        ffmpegProcess?.destroy()
        ffmpegProcess = null
        playJob?.cancel()
        playJob = null
        currentUrl = null
        pausedAtMs = 0L
    }

    fun togglePlayPause() {
        when {
            isPaused.get() -> resume()
            isPlaying      -> pause()
        }
    }

    private fun buildFfmpegProcess(url: String): Process {
        // ffmpeg reads the URL and outputs raw PCM s16le stereo 44100
        val cmd = listOf(
            ffmpegBinary(),
            "-reconnect", "1",
            "-reconnect_streamed", "1",
            "-reconnect_delay_max", "5",
            "-i", url,
            "-vn",                    // no video
            "-acodec", "pcm_s16le",   // raw PCM output
            "-ar", "44100",           // sample rate
            "-ac", "2",               // stereo
            "-f", "s16le",            // raw format
            "pipe:1"                  // write to stdout
        )
        return ProcessBuilder(cmd)
            .redirectErrorStream(false) // keep stderr separate so we don't mix it with PCM
            .start()
    }

    private fun ffmpegBinary(): String {
        val candidates = listOf("ffmpeg", "/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg")
        for (bin in candidates) {
            try {
                val p = ProcessBuilder(bin, "-version").redirectErrorStream(true).start()
                p.waitFor()
                if (p.exitValue() == 0) return bin
            } catch (_: Exception) {
                continue
            }
        }
        return "ffmpeg"
    }
}
