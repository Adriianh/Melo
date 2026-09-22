package com.github.adriianh.cli.tui.player

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.player.PlaybackState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

/**
 * Audio player backed by ffplay (bundled with ffmpeg).
 *
 * ffplay uses SDL2 for audio output, which auto-selects the best backend per OS:
 *   Linux → PipeWire / PulseAudio / ALSA
 *   macOS → CoreAudio
 *   Windows → DirectSound / WASAPI
 *
 * Pause: SIGSTOP / SIGCONT — instantaneous, no buffer delay.
 * Volume: WASAPI (Core Audio) on Windows, pactl set-sink-input-volume on Linux/PulseAudio (no interruption);
 *          fallback: stored and applied on next play() via -af volume= filter.
 *
 * Implements [MeloPlayer] so the same playback contract used by the Compose
 * app (and driven by `data.PlaybackManagerImpl`) can drive this ffplay engine.
 */
class AudioPlayer(
    private val scope: CoroutineScope,
    private val onProgress: (elapsedMs: Long) -> Unit = {},
    private val onFinish: () -> Unit = {},
    private val onError: (Throwable) -> Unit = {},
) : MeloPlayer {

    private var playJob: Job? = null
    private var playerProcess: Process? = null
    private var playerPid: Long? = null

    private val isPaused = AtomicBoolean(false)
    private val sessionId = AtomicLong(0L)

    @Volatile
    private var startTimeMs: Long = 0L

    @Volatile
    private var pausedAtMs: Long = 0L

    @Volatile
    private var pausedSinceMs: Long = 0L

    @Volatile
    private var volumePct: Int = 75

    @Volatile
    private var currentUrl: String? = null

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    val isPlaying: Boolean get() = playJob?.isActive == true && !isPaused.get()

    fun play(url: String) {
        startPlayback(url = url, seekMs = 0L)
    }

    /**
     * Stages a stream and starts playback immediately, mirroring [com.github.adriianh.core.domain.player.JvmMeloPlayer.load]
     * semantics: load() already starts playing, [play] is only needed to resume or
     * restart after the process has exited.
     */
    override fun load(url: String, track: Track, initialPositionMs: Long) {
        _state.update {
            it.copy(
                currentTrack = track,
                progressMs = initialPositionMs,
                durationMs = track.durationMs,
                isBuffering = true,
                isFinished = false,
                error = null,
            )
        }
        startPlayback(url = url, seekMs = initialPositionMs)
    }

    override fun play() {
        if (isPaused.get()) {
            resume()
            return
        }
        val url = currentUrl
        if (url != null && playJob?.isActive != true) {
            startPlayback(url = url, seekMs = _state.value.progressMs)
        }
    }

    private fun startPlayback(url: String, seekMs: Long) {
        val session = sessionId.incrementAndGet()
        val previousJob = playJob
        val previousProcess = playerProcess
        val previousPid = playerPid

        _state.update {
            it.copy(
                isPlaying = true,
                isBuffering = false,
                isFinished = false,
                error = null,
            )
        }

        playJob = scope.launch {
            previousJob?.cancel()
            FfplayProcessManager.destroySafely(previousProcess, previousPid)
            withTimeoutOrNull(1000.milliseconds) { previousJob?.join() }

            if (sessionId.get() != session) return@launch

            currentUrl = url
            isPaused.set(false)
            pausedSinceMs = 0L
            startTimeMs = System.currentTimeMillis()
            pausedAtMs = 0L
            launchPlayback(url, volumePct, seekMs = seekMs, session = session)
        }
    }

    override fun pause() {
        if (isPaused.get() || !isPlaying) return
        isPaused.set(true)
        pausedSinceMs = System.currentTimeMillis()
        pausedAtMs += System.currentTimeMillis() - startTimeMs
        if (FfplayProcessManager.isWindows) {
            val process = playerProcess
            val pid = playerPid
            playerProcess = null
            playerPid = null
            FfplayProcessManager.destroyImmediately(process, pid)
        } else {
            val pid = playerPid ?: return
            FfplayProcessManager.suspendProcess(pid)
        }
        _state.update { it.copy(isPlaying = false, isBuffering = false) }
    }

    fun resume() {
        if (!isPaused.get()) return
        val url = currentUrl
        val pauseDuration = System.currentTimeMillis() - pausedSinceMs
        val isAlive = playerProcess?.isAlive == true

        // If paused for more than 15s or ffplay died in the background,
        // remote HTTP/HTTPS streams (YouTube, CDNs) have already closed their TCP connection.
        // Restarting fresh at the saved position is vastly more reliable than SIGCONT on a dead socket.
        if (url != null && (!isAlive || (pauseDuration > 15_000L && (url.startsWith("http://") || url.startsWith(
                "https://"
            ))))
        ) {
            isPaused.set(false)
            pausedSinceMs = 0L
            startPlayback(url = url, seekMs = _state.value.progressMs)
            return
        }

        startTimeMs = System.currentTimeMillis()
        isPaused.set(false)
        pausedSinceMs = 0L
        val pid = playerPid ?: return
        FfplayProcessManager.resumeProcess(pid)
        _state.update { it.copy(isPlaying = true) }
    }

    val hasRealtimeVolumeControl: Boolean
        get() = (FfplayProcessManager.isWindows && WindowsVolumeController.isAvailable) || PactlVolumeController.hasPactl

    /**
     * Set volume 0–100 without interrupting playback.
     *
     * On Windows: uses WASAPI (Core Audio) ISimpleAudioVolume to adjust the ffplay
     * session volume dynamically in real time — zero interruption.
     *
     * On Linux with PulseAudio/PipeWire: uses `pactl set-sink-input-volume` to
     * adjust the volume of the ffplay stream in real time — zero interruption.
     *
     * On other platforms: stores the value; applied on the next play() call.
     */
    fun setVolume(pct: Int) {
        volumePct = pct.coerceIn(0, 100)
        val pid = playerPid ?: return
        if (FfplayProcessManager.isWindows && WindowsVolumeController.isAvailable) {
            WindowsVolumeController.applyVolume(scope, pid, volumePct)
        } else if (PactlVolumeController.hasPactl) {
            PactlVolumeController.applyVolume(scope, pid, volumePct)
        }
    }

    /** Contract override: volume is 0f..1f in the shared playback stack. */
    override fun setVolume(volume: Float) {
        setVolume((volume * 100).toInt())
    }

    override fun seekTo(positionMs: Long) {
        _state.update { it.copy(progressMs = positionMs) }
        seek(positionMs)
    }

    /**
     * Seek to [ms] milliseconds from the start of the current stream.
     * Restarts ffplay with -ss, preserving volume and paused state.
     */
    fun seek(ms: Long) {
        val url = currentUrl ?: return
        val clampedMs = ms.coerceAtLeast(0L)
        val wasPaused = isPaused.get()

        if (FfplayProcessManager.isWindows && wasPaused) {
            pausedAtMs = clampedMs
            _state.update { it.copy(progressMs = clampedMs) }
            onProgress(clampedMs)
            return
        }

        val session = sessionId.incrementAndGet()
        val previousJob = playJob
        val previousProcess = playerProcess
        val previousPid = playerPid

        playJob = scope.launch {
            previousJob?.cancel()
            FfplayProcessManager.destroySafely(previousProcess, previousPid)
            withTimeoutOrNull(1000.milliseconds) { previousJob?.join() }

            if (sessionId.get() != session) return@launch

            playerProcess = null
            playerPid = null
            isPaused.set(false)

            pausedAtMs = if (wasPaused) clampedMs else 0L
            startTimeMs = System.currentTimeMillis()

            if (wasPaused) {
                isPaused.set(true)
                pausedSinceMs = System.currentTimeMillis()
            }

            launchPlayback(url, volumePct, seekMs = clampedMs, session = session)

            if (wasPaused) {
                scope.launch {
                    delay(300.milliseconds)
                    if (isPaused.get() && sessionId.get() == session) {
                        val pid = playerPid ?: return@launch
                        FfplayProcessManager.suspendProcess(pid)
                    }
                }
            }
        }
    }

    override fun stop() {
        val previousJob = playJob
        val previousProcess = playerProcess
        val previousPid = playerPid

        playerProcess = null
        playerPid = null
        isPaused.set(false)
        pausedSinceMs = 0L
        currentUrl = null
        pausedAtMs = 0L

        _state.update {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                isFinished = false,
                error = null,
            )
        }

        playJob = scope.launch {
            withContext(NonCancellable) {
                previousJob?.cancel()
                FfplayProcessManager.destroySafely(previousProcess, previousPid)
                withTimeoutOrNull(1000.milliseconds) { previousJob?.join() }
            }
        }
    }

    override fun release() {
        val process = playerProcess
        val pid = playerPid
        playerProcess = null
        playerPid = null
        playJob?.cancel()
        FfplayProcessManager.destroyImmediately(process, pid)
    }

    override fun setIdleTrack(track: Track, initialPositionMs: Long) {
        _state.update {
            it.copy(
                currentTrack = track,
                progressMs = initialPositionMs,
                durationMs = track.durationMs,
                isPlaying = false,
                isBuffering = false,
                isFinished = false,
                error = null,
            )
        }
    }

    private suspend fun launchPlayback(url: String, volPct: Int, seekMs: Long, session: Long) {
        try {
            val process = FfplayProcessManager.buildProcess(
                url = url,
                volPct = if (hasRealtimeVolumeControl) 100 else volPct,
                seekMs = seekMs
            )
            playerProcess = process
            val pid = process.pid()
            playerPid = pid
            startTimeMs = System.currentTimeMillis()

            if (FfplayProcessManager.isWindows && WindowsVolumeController.isAvailable) {
                WindowsVolumeController.applyVolume(scope, pid, volPct)
            } else if (PactlVolumeController.hasPactl) {
                PactlVolumeController.applyVolume(scope, pid, volPct)
            }

            val progressJob = scope.launch {
                while (isActive && sessionId.get() == session) {
                    if (!isPaused.get()) {
                        val elapsed = pausedAtMs + (System.currentTimeMillis() - startTimeMs)
                        val positionMs = elapsed + seekMs
                        _state.update { it.copy(progressMs = positionMs) }
                        onProgress(positionMs)
                    }
                    delay(1000.milliseconds)
                }
            }

            val exitCode = withContext(Dispatchers.IO) {
                process.waitFor()
            }

            progressJob.cancel()
            if (sessionId.get() == session) {
                if (exitCode == 0) {
                    _state.update { it.copy(isPlaying = false, isFinished = true) }
                    onFinish()
                } else if (!isPaused.get()) {
                    val duration = _state.value.durationMs
                    val progress = _state.value.progressMs
                    if (duration > 0 && progress >= duration - 2000L) {
                        _state.update { it.copy(isPlaying = false, isFinished = true) }
                        onFinish()
                    } else {
                        _state.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                isFinished = false,
                                error = "Playback stopped unexpectedly (exit code $exitCode)",
                            )
                        }
                        onError(RuntimeException("Playback stopped unexpectedly (exit code $exitCode)"))
                    }
                }
            }
        } catch (e: CancellationException) {
            FfplayProcessManager.destroySafely(playerProcess, playerPid)
            throw e
        } catch (e: Exception) {
            FfplayProcessManager.destroySafely(playerProcess, playerPid)
            _state.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    isFinished = false,
                    error = e.message,
                )
            }
            if (sessionId.get() == session) onError(e)
        }
    }
}