package com.github.adriianh.cli.command.player.handler

import com.github.adriianh.cli.tui.player.AudioPlayer
import com.github.adriianh.cli.tui.player.FfplayProcessManager
import com.github.adriianh.cli.tui.player.ipc.LocalIpcServer
import com.github.adriianh.cli.tui.service.DiscordRpcManager
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.JvmMediaSessionManager
import com.github.adriianh.core.domain.player.PlaybackStatusDto
import com.github.adriianh.core.domain.provider.AgeRestrictedException
import com.github.adriianh.core.domain.repository.ScrobblingRepository
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.playback.RecordPlayUseCase
import com.github.adriianh.core.domain.usecase.search.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.ajalt.mordant.animation.progress.ThreadProgressTaskAnimator
import com.github.ajalt.mordant.animation.progress.animateOnThread
import com.github.ajalt.mordant.animation.progress.execute
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.text
import io.ktor.client.HttpClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

object PlayActionHandler : KoinComponent {
    private val httpClient: HttpClient by inject()
    private val getSimilarTracks: GetSimilarTracksUseCase by inject()
    private val searchTracks: SearchTracksUseCase by inject()
    private val scrobbling: ScrobblingRepository by inject()
    private val recordPlay: RecordPlayUseCase by inject()
    private val discordRpc: DiscordRpcManager by inject()
    private val getSettings: GetSettingsUseCase by inject()
    private val updateSettings: UpdateSettingsUseCase by inject()

    private var rpcEnabled = true

    suspend fun playTrack(
        seedTrack: Track,
        getStream: GetStreamUseCase,
        terminal: Terminal = Terminal(),
    ) {
        startPlayback(
            contextName = seedTrack.title,
            initialTracks = listOf(seedTrack),
            getStream = getStream,
            terminal = terminal,
            shouldFetchSimilar = true,
        )
    }

    suspend fun playMultiple(
        contextName: String,
        tracks: List<Track>,
        getStream: GetStreamUseCase,
        terminal: Terminal = Terminal(),
    ) {
        startPlayback(
            contextName = contextName,
            initialTracks = tracks,
            getStream = getStream,
            terminal = terminal,
            shouldFetchSimilar = false,
        )
    }

    suspend fun startDaemon(
        getStream: GetStreamUseCase,
        terminal: Terminal = Terminal(),
        idleTimeoutMinutes: Int = 0,
    ) {
        startPlayback(
            contextName = "Daemon Mode",
            initialTracks = emptyList(),
            getStream = getStream,
            terminal = terminal,
            shouldFetchSimilar = false,
            idleTimeoutMinutes = idleTimeoutMinutes,
        )
    }

    private suspend fun startPlayback(
        contextName: String,
        initialTracks: List<Track>,
        getStream: GetStreamUseCase,
        terminal: Terminal,
        shouldFetchSimilar: Boolean,
        idleTimeoutMinutes: Int = 0,
    ) {
        terminal.println(cyan("Starting playback for $contextName... Press Ctrl+C to stop."))
        rpcEnabled = getSettings.getSnapshot().discordRpcEnabled
        terminal.println(gray("Media keys (Play/Pause, Next, Prev) are supported in background."))
        var currentTrack: Track? = initialTracks.firstOrNull()
        var isPlaying = false
        var currentPositionMs = 0L
        var currentVolume = 75
        var lastActiveAt = System.currentTimeMillis()
        val radioQueue = initialTracks.toMutableList()
        var queueIndex = 0
        var playPauseAction: (() -> Unit)? = null
        var nextAction: (() -> Unit)? = null
        var prevAction: (() -> Unit)? = null
        var stopAction: (() -> Unit)? = null
        val stopSignal = CompletableDeferred<Unit>()
        val playerScope = CoroutineScope(Dispatchers.IO)
        var activeProgressJob: Job? = null
        var idleWatchdogJob: Job? = null
        var activeProgressTask: ThreadProgressTaskAnimator<Unit>? = null

        var trackStartedAt = System.currentTimeMillis()
        var hasScrobbledCurrent = false

        if (rpcEnabled) {
            discordRpc.connect()
        }

        val sessionManager =
            JvmMediaSessionManager(
                httpClient = httpClient,
                onPlayPause = { playPauseAction?.invoke() },
                onNext = { nextAction?.invoke() },
                onPrevious = { prevAction?.invoke() },
                onStop = { stopAction?.invoke() },
            )

        val player =
            AudioPlayer(
                scope = playerScope,
                onProgress = { posMs ->
                    currentPositionMs = posMs
                    lastActiveAt = System.currentTimeMillis()
                    sessionManager.updatePosition(posMs)
                    activeProgressTask?.update { completed = posMs }

                    currentTrack?.let { track ->
                        if (!hasScrobbledCurrent && track.durationMs > 0) {
                            val threshold = minOf(track.durationMs / 2, 4 * 60 * 1000L)
                            if (posMs >= threshold) {
                                hasScrobbledCurrent = true
                                playerScope.launch {
                                    try {
                                        scrobbling.scrobble(track, trackStartedAt)
                                        recordPlay(track, trackStartedAt)
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }
                    }
                },
                onFinish = {
                    currentPositionMs = 0L
                    nextAction?.invoke()
                },
                onError = { _ ->
                    currentPositionMs = 0L
                    nextAction?.invoke()
                },
            )

        suspend fun playCurrentTrack() {
            val track = currentTrack ?: return
            var isAgeRestricted = false
            val url =
                try {
                    getStream(track)
                } catch (_: AgeRestrictedException) {
                    isAgeRestricted = true
                    null
                } catch (_: Exception) {
                    null
                }
            if (url != null) {
                terminal.println(green("▶ Playing: ") + track.title + gray(" by ") + track.artist)
                activeProgressJob?.cancel()
                activeProgressTask =
                    progressBarLayout {
                        text {
                            val posSec = completed / 1000L
                            val lenSec = (total ?: 0L) / 1000L
                            val posStr = formatDuration(posSec)
                            val lenStr = formatDuration(lenSec)
                            gray("$posStr / $lenStr")
                        }
                        progressBar()
                    }.animateOnThread(terminal, total = track.durationMs)
                activeProgressJob =
                    playerScope.launch {
                        activeProgressTask.execute()
                    }
                player.play(url)
                isPlaying = true
                trackStartedAt = System.currentTimeMillis()
                hasScrobbledCurrent = false
                sessionManager.updateTrack(track, track.durationMs)
                playerScope.launch {
                    try {
                        scrobbling.updateNowPlaying(track)
                    } catch (_: Exception) {
                    }
                }
                if (rpcEnabled) {
                    discordRpc.updateActivity(track, true)
                }
            } else {
                if (isAgeRestricted) {
                    terminal.println(yellow("[!] Track is age-restricted and could not be resolved: ") + track.title)
                } else {
                    terminal.println(yellow("[!] Failed to get stream for: ") + track.title)
                }
                nextAction?.invoke()
            }
        }

        val ipcServer =
            LocalIpcServer(
                onPlayPause = { playPauseAction?.invoke() },
                onNext = { nextAction?.invoke() },
                onPrevious = { prevAction?.invoke() },
                onStop = { stopAction?.invoke() },
                onQueueAdd = { track ->
                    lastActiveAt = System.currentTimeMillis()
                    val wasEmpty = radioQueue.isEmpty()
                    radioQueue.add(track)
                    terminal.println(green("\n+ Added to queue: ") + track.title + gray(" by ") + track.artist)
                    if (wasEmpty) {
                        playerScope.launch {
                            currentTrack = track
                            queueIndex = 0
                            playCurrentTrack()
                        }
                    }
                },
                onQueueRemove = { index ->
                    lastActiveAt = System.currentTimeMillis()
                    val realIndex = queueIndex + 1 + index
                    if (realIndex in (queueIndex + 1) until radioQueue.size) {
                        val removed = radioQueue.removeAt(realIndex)
                        terminal.println(gray("\nRemoved from queue: ") + removed.title)
                        true
                    } else {
                        false
                    }
                },
                onQueueClear = {
                    lastActiveAt = System.currentTimeMillis()
                    if (radioQueue.size > queueIndex + 1) {
                        val toKeep = radioQueue.subList(0, queueIndex + 1).toList()
                        radioQueue.clear()
                        radioQueue.addAll(toKeep)
                        terminal.println(gray("\nQueue cleared (except history and current track)."))
                    }
                },
                getQueue = { radioQueue.drop(queueIndex + 1) },
                onPlayNow = { track ->
                    lastActiveAt = System.currentTimeMillis()
                    playerScope.launch {
                        player.stop()
                        radioQueue.clear()
                        radioQueue.add(track)
                        queueIndex = 0
                        currentTrack = track
                        playCurrentTrack()
                    }
                },
                onPlayList = { tracks ->
                    if (tracks.isNotEmpty()) {
                        lastActiveAt = System.currentTimeMillis()
                        playerScope.launch {
                            player.stop()
                            radioQueue.clear()
                            radioQueue.addAll(tracks)
                            queueIndex = 0
                            currentTrack = tracks.first()
                            playCurrentTrack()
                        }
                    }
                },
                onQueueAddList = { tracks ->
                    if (tracks.isNotEmpty()) {
                        lastActiveAt = System.currentTimeMillis()
                        val wasEmpty = radioQueue.isEmpty()
                        radioQueue.addAll(tracks)
                        terminal.println(green("\n+ Added ${tracks.size} tracks to queue."))
                        if (wasEmpty) {
                            playerScope.launch {
                                currentTrack = tracks.first()
                                queueIndex = 0
                                playCurrentTrack()
                            }
                        }
                    }
                },
                getStatus = {
                    PlaybackStatusDto(
                        track = currentTrack,
                        isPlaying = isPlaying,
                        positionMs = currentPositionMs,
                        durationMs = currentTrack?.durationMs ?: 0L,
                        volume = currentVolume,
                        queueSize = (radioQueue.size - (queueIndex + 1)).coerceAtLeast(0),
                    )
                },
                onVolumeGet = { currentVolume },
                onVolumeSet = { vol ->
                    lastActiveAt = System.currentTimeMillis()
                    currentVolume = vol.coerceIn(0, 100)
                    player.setVolume(currentVolume)
                },
                onVolumeAdjust = { delta ->
                    lastActiveAt = System.currentTimeMillis()
                    currentVolume = (currentVolume + delta).coerceIn(0, 100)
                    player.setVolume(currentVolume)
                },
                onCustomCommand = { cmd: String, _: String ->
                    when (cmd) {
                        "RPC_TOGGLE" -> {
                            rpcEnabled = !rpcEnabled
                            playerScope.launch {
                                val currentSettings = getSettings.getSnapshot()
                                updateSettings(currentSettings.copy(discordRpcEnabled = rpcEnabled))
                            }
                            if (rpcEnabled) {
                                discordRpc.connect()
                                currentTrack?.let { discordRpc.updateActivity(it, isPlaying) }
                            } else {
                                discordRpc.disconnect()
                            }
                            "RPC is now ${if (rpcEnabled) "ENABLED" else "DISABLED"}"
                        }

                        "GET_CURRENT_TRACK" -> {
                            currentTrack?.let { Json.encodeToString(Track.serializer(), it) }
                                ?: "ERROR No track playing"
                        }

                        else -> "ERROR Unknown command"
                    }
                },
            )
        ipcServer.start(playerScope)
        sessionManager.init()

        if (idleTimeoutMinutes > 0) {
            idleWatchdogJob =
                playerScope.launch {
                    val timeoutMs = idleTimeoutMinutes * 60 * 1000L
                    while (isActive) {
                        delay(15_000L.milliseconds)
                        if (!isPlaying) {
                            val elapsed = System.currentTimeMillis() - lastActiveAt
                            if (elapsed >= timeoutMs) {
                                terminal.println(
                                    yellow("\nDaemon idle timeout reached ($idleTimeoutMinutes min). Shutting down..."),
                                )
                                stopAction?.invoke()
                                break
                            }
                        }
                    }
                }
        }

        playPauseAction = {
            lastActiveAt = System.currentTimeMillis()
            if (isPlaying) {
                player.pause()
                sessionManager.notifyPaused()
                terminal.println(yellow("⏸ Paused"))
            } else {
                player.resume()
                sessionManager.notifyResumed()
                terminal.println(green("▶ Resumed"))
            }
            isPlaying = !isPlaying
            if (rpcEnabled) {
                discordRpc.updateActivity(currentTrack, isPlaying)
            }
        }
        nextAction = {
            lastActiveAt = System.currentTimeMillis()
            playerScope.launch {
                if (queueIndex + 1 < radioQueue.size) {
                    queueIndex++
                    currentTrack = radioQueue[queueIndex]
                    playCurrentTrack()
                } else {
                    currentTrack?.let { track ->
                        if (shouldFetchSimilar) {
                            try {
                                terminal.println(gray("Fetching similar tracks..."))
                                val similarRaw =
                                    getSimilarTracks(track.artist, track.title).take(5)
                                val similarTracksResolved =
                                    similarRaw
                                        .mapNotNull { sim ->
                                            searchTracks("${sim.title} ${sim.artist}").firstOrNull()
                                        }.filter { t -> radioQueue.none { it.id == t.id } }
                                if (similarTracksResolved.isNotEmpty()) {
                                    radioQueue.addAll(similarTracksResolved)
                                    queueIndex++
                                    currentTrack = radioQueue[queueIndex]
                                    playCurrentTrack()
                                } else {
                                    terminal.println(gray("No more related tracks found."))
                                    stopAction?.invoke()
                                }
                            } catch (e: Exception) {
                                terminal.println(gray("Failed to fetch similar tracks: ${e.message}"))
                                stopAction?.invoke()
                            }
                        } else {
                            stopAction?.invoke()
                        }
                    } ?: stopAction?.invoke()
                }
            }
        }
        prevAction = {
            lastActiveAt = System.currentTimeMillis()
            playerScope.launch {
                if (queueIndex > 0) {
                    queueIndex--
                    currentTrack = radioQueue[queueIndex]
                    playCurrentTrack()
                } else {
                    player.seek(0)
                }
            }
        }
        stopAction = {
            idleWatchdogJob?.cancel()
            activeProgressJob?.cancel()
            player.release()
            sessionManager.notifyStopped()
            sessionManager.release()
            isPlaying = false
            currentPositionMs = 0L
            stopSignal.complete(Unit)
            terminal.println(cyan("Playback stopped."))
        }
        playerScope.launch {
            if (currentTrack != null) {
                playCurrentTrack()
            }
        }
        try {
            stopSignal.await()
        } finally {
            idleWatchdogJob?.cancel()
            activeProgressJob?.cancel()
            try {
                player.release()
            } catch (_: Throwable) {
            }
            try {
                sessionManager.release()
            } catch (_: Throwable) {
            }
            try {
                ipcServer.stop()
            } catch (_: Throwable) {
            }
            try {
                FfplayProcessManager.killAll()
            } catch (_: Throwable) {
            }
        }
        exitProcess(0)
    }

    private fun formatDuration(seconds: Long): String {
        val mins = (seconds / 60).toString().padStart(2, '0')
        val secs = (seconds % 60).toString().padStart(2, '0')
        return "$mins:$secs"
    }
}
