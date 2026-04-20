package com.github.adriianh.cli.command.player.handler

import com.github.adriianh.cli.tui.player.AudioPlayer
import com.github.adriianh.cli.tui.player.MediaSessionManager
import com.github.adriianh.cli.tui.player.ipc.LocalIpcServer
import com.github.adriianh.cli.tui.service.DiscordRpcManager
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.ScrobblingRepository
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.playback.RecordPlayUseCase
import com.github.adriianh.core.domain.usecase.search.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
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
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.exitProcess

object PlayActionHandler : KoinComponent {
    private val httpClient: HttpClient by inject()
    private val getSimilarTracks: GetSimilarTracksUseCase by inject()
    private val searchTracks: SearchTracksUseCase by inject()
    private val scrobbling: ScrobblingRepository by inject()
    private val recordPlay: RecordPlayUseCase by inject()
    private val discordRpc: DiscordRpcManager by inject()

    private var rpcEnabled = true
    suspend fun playTrack(
        seedTrack: Track,
        getStream: GetStreamUseCase,
        terminal: Terminal = Terminal()
    ) {
        startPlayback(
            contextName = seedTrack.title,
            initialTracks = listOf(seedTrack),
            getStream = getStream,
            terminal = terminal,
            shouldFetchSimilar = true
        )
    }

    suspend fun playMultiple(
        contextName: String,
        tracks: List<Track>,
        getStream: GetStreamUseCase,
        terminal: Terminal = Terminal()
    ) {
        startPlayback(
            contextName = contextName,
            initialTracks = tracks,
            getStream = getStream,
            terminal = terminal,
            shouldFetchSimilar = false
        )
    }

    suspend fun startDaemon(
        getStream: GetStreamUseCase,
        terminal: Terminal = Terminal()
    ) {
        startPlayback(
            contextName = "Daemon Mode",
            initialTracks = emptyList(),
            getStream = getStream,
            terminal = terminal,
            shouldFetchSimilar = false
        )
    }

    private suspend fun startPlayback(
        contextName: String,
        initialTracks: List<Track>,
        getStream: GetStreamUseCase,
        terminal: Terminal,
        shouldFetchSimilar: Boolean
    ) {
        terminal.println(cyan("Starting playback for $contextName... Press Ctrl+C to stop."))
        terminal.println(gray("Media keys (Play/Pause, Next, Prev) are supported in background."))
        var currentTrack: Track? = initialTracks.firstOrNull()
        var isPlaying = false
        val radioQueue = initialTracks.toMutableList()
        var queueIndex = 0
        var playPauseAction: (() -> Unit)? = null
        var nextAction: (() -> Unit)? = null
        var prevAction: (() -> Unit)? = null
        var stopAction: (() -> Unit)? = null
        val stopSignal = CompletableDeferred<Unit>()
        val playerScope = CoroutineScope(Dispatchers.IO)
        var activeProgressJob: Job? = null
        var activeProgressTask: ThreadProgressTaskAnimator<Unit>? = null

        var trackStartedAt = System.currentTimeMillis()
        var hasScrobbledCurrent = false

        if (rpcEnabled) {
            discordRpc.connect()
        }

        val sessionManager = MediaSessionManager(
            httpClient = httpClient,
            onPlayPause = { playPauseAction?.invoke() },
            onNext = { nextAction?.invoke() },
            onPrevious = { prevAction?.invoke() },
            onStop = { stopAction?.invoke() }
        )

        val player = AudioPlayer(
            scope = playerScope,
            onProgress = { posMs ->
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
            onFinish = { nextAction?.invoke() },
            onError = { _ -> nextAction?.invoke() }
        )

        // Declare playCurrentTrackFromQueue as a local function to avoid circularity issues
        suspend fun playCurrentTrack() {
            val track = currentTrack ?: return
            val url = getStream(track)
            if (url != null) {
                terminal.println(green("▶ Playing: ") + track.title + gray(" by ") + track.artist)
                activeProgressJob?.cancel()
                activeProgressTask = progressBarLayout {
                    text {
                        val posSec = completed / 1000L
                        val lenSec = (total ?: 0L) / 1000L
                        val posStr = String.format("%02d:%02d", posSec / 60, posSec % 60)
                        val lenStr = String.format("%02d:%02d", lenSec / 60, lenSec % 60)
                        gray("$posStr / $lenStr")
                    }
                    progressBar()
                }.animateOnThread(terminal, total = track.durationMs)
                activeProgressJob = playerScope.launch {
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
                terminal.println(yellow("⚠️ Failed to get stream for: ") + track.title)
                nextAction?.invoke()
            }
        }

        val ipcServer = LocalIpcServer(
            onPlayPause = { playPauseAction?.invoke() },
            onNext = { nextAction?.invoke() },
            onPrevious = { prevAction?.invoke() },
            onStop = { stopAction?.invoke() },
            onQueueAdd = { track ->
                val wasEmpty = radioQueue.isEmpty()
                radioQueue.add(track)
                terminal.println(green("\n🎵 Added to queue: ") + track.title + gray(" by ") + track.artist)
                if (wasEmpty) {
                    playerScope.launch {
                        currentTrack = track
                        queueIndex = 0
                        playCurrentTrack()
                    }
                }
            },
            onQueueRemove = { index ->
                val realIndex = queueIndex + 1 + index
                if (realIndex in (queueIndex + 1) until radioQueue.size) {
                    val removed = radioQueue.removeAt(realIndex)
                    terminal.println(gray("\nRemoved from queue: ") + removed.title)
                    true
                } else false
            },
            onQueueClear = {
                if (radioQueue.size > queueIndex + 1) {
                    val toKeep = radioQueue.subList(0, queueIndex + 1).toList()
                    radioQueue.clear()
                    radioQueue.addAll(toKeep)
                    terminal.println(gray("\nQueue cleared (except history and current track)."))
                }
            },
            getQueue = { radioQueue.drop(queueIndex + 1) },
            onCustomCommand = { cmd: String, _: String ->
                when (cmd) {
                    "RPC_TOGGLE" -> {
                        rpcEnabled = !rpcEnabled
                        if (rpcEnabled) {
                            discordRpc.connect()
                            currentTrack?.let { discordRpc.updateActivity(it, isPlaying) }
                        } else {
                            discordRpc.disconnect()
                        }
                        "RPC is now ${if (rpcEnabled) "ENABLED" else "DISABLED"}"
                    }

                    "GET_CURRENT_TRACK" -> {
                        currentTrack?.let { Json.encodeToString(it) } ?: "ERROR No track playing"
                    }

                    else -> "ERROR Unknown command"
                }
            }
        )
        ipcServer.start(playerScope)
        sessionManager.init()

        playPauseAction = {
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
                                val similarTracksResolved = similarRaw.mapNotNull { sim ->
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
            activeProgressJob?.cancel()
            player.stop()
            sessionManager.notifyStopped()
            sessionManager.destroy()
            isPlaying = false
            stopSignal.complete(Unit)
            terminal.println(cyan("Playback stopped."))
        }
        playerScope.launch {
            if (currentTrack != null) {
                playCurrentTrack()
            }
        }
        // Keep running until stopAction is invoked (e.g. by session manager directly or error)
        try {
            stopSignal.await()
        } finally {
            activeProgressJob?.cancel()
            player.stop()
            sessionManager.destroy()
            ipcServer.stop()
        }
        exitProcess(0)
    }
}