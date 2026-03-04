package com.github.adriianh.cli.tui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine

/**
 * Audio player that decodes any stream URL via ffmpeg and outputs PCM to javax.sound.sampled.
 * Supports all formats yt-dlp may return: opus/webm, m4a/aac, mp3, etc.
 *
 * Pause: blocks the PCM write loop so ffmpeg keeps buffering internally — resumes exactly
 *        where it left off without restarting the stream.
 * Volume: controlled via FloatControl.Type.MASTER_GAIN on the SourceDataLine.
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

    private val pauseLock = ReentrantLock()
    private val resumeCondition = pauseLock.newCondition()

    @Volatile private var startTimeMs: Long = 0L
    @Volatile private var pausedAtMs: Long = 0L

    @Volatile private var volumePct: Int = 75

    val isPlaying: Boolean get() = playJob?.isActive == true && !isPaused.get()

    private val pcmFormat = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        44100f, 16, 2, 4, 44100f, false
    )

    fun play(url: String) {
        stop()
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

    private fun startPlayback(url: String) {
        val ffmpeg = buildFfmpegProcess(url)
        ffmpegProcess = ffmpeg

        val info = DataLine.Info(SourceDataLine::class.java, pcmFormat)
        val line = AudioSystem.getLine(info) as SourceDataLine
        audioLine = line
        line.open(pcmFormat)
        applyVolume(line, volumePct)
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

                pauseLock.lock()
                try {
                    while (isPaused.get() && !isStopped.get()) {
                        resumeCondition.await()
                    }
                } finally {
                    pauseLock.unlock()
                }

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
        audioLine?.flush()
    }

    fun resume() {
        if (!isPaused.get()) return
        startTimeMs = System.currentTimeMillis()
        audioLine?.start()
        isPaused.set(false)
        pauseLock.lock()
        try {
            resumeCondition.signalAll()
        } finally {
            pauseLock.unlock()
        }
    }

    fun setVolume(pct: Int) {
        volumePct = pct.coerceIn(0, 100)
        audioLine?.let { applyVolume(it, volumePct) }
    }

    fun stop() {
        isStopped.set(true)
        pauseLock.lock()
        try {
            isPaused.set(false)
            resumeCondition.signalAll()
        } finally {
            pauseLock.unlock()
        }
        audioLine?.stop()
        audioLine?.close()
        audioLine = null
        ffmpegProcess?.destroy()
        ffmpegProcess = null
        playJob?.cancel()
        playJob = null
        pausedAtMs = 0L
    }

    fun togglePlayPause() {
        when {
            isPaused.get() -> resume()
            isPlaying      -> pause()
        }
    }

    // ── Volume helper ──────────────────────────────────────────────────────────

    /**
     * Converts a 0–100 percentage to decibels and applies it to the line's MASTER_GAIN control.
     * 100  →  0.0 dB  (full volume, no attenuation)
     *  50  → ~-6 dB
     *   0  → minimum dB (silence)
     */
    private fun applyVolume(line: SourceDataLine, pct: Int) {
        if (!line.isControlSupported(FloatControl.Type.MASTER_GAIN)) return
        val gain = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val dB = if (pct == 0) {
            gain.minimum
        } else {
            gain.minimum + (0f - gain.minimum) * (pct / 100f)
        }
        gain.value = dB.coerceIn(gain.minimum, gain.maximum)
    }

    // ── ffmpeg helpers ─────────────────────────────────────────────────────────

    private fun buildFfmpegProcess(url: String): Process {
        val cmd = listOf(
            ffmpegBinary(),
            "-reconnect", "1",
            "-reconnect_streamed", "1",
            "-reconnect_delay_max", "5",
            "-i", url,
            "-vn",
            "-acodec", "pcm_s16le",
            "-ar", "44100",
            "-ac", "2",
            "-f", "s16le",
            "pipe:1"
        )
        return ProcessBuilder(cmd)
            .redirectErrorStream(false)
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
