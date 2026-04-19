package com.github.adriianh.cli.command.player.handler

import com.github.adriianh.cli.tui.player.AudioPlayer
import com.github.adriianh.cli.tui.player.MediaSessionManager
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.search.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.HttpClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.exitProcess

object PlayActionHandler : KoinComponent {
    private val httpClient: HttpClient by inject()
    private val getSimilarTracks: GetSimilarTracksUseCase by inject()
    private val searchTracks: SearchTracksUseCase by inject()
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

    private suspend fun startPlayback(
        contextName: String,
        initialTracks: List<Track>,
        getStream: GetStreamUseCase,
        terminal: Terminal,
        shouldFetchSimilar: Boolean
    ) {
        terminal.println(cyan("Starting playback for $contextName... Press Ctrl+C to stop."))
        terminal.println(gray("Media keys (Play/Pause, Next, Prev) are supported in background."))
        var currentTrack = initialTracks.first()
        var isPlaying = true
        val radioQueue = initialTracks.toMutableList()
        var queueIndex = 0
        val stopSignal = CompletableDeferred<Unit>()
        var playPauseAction: (() -> Unit)? = null
        var nextAction: (() -> Unit)? = null
        var prevAction: (() -> Unit)? = null
        var stopAction: (() -> Unit)? = null
        val playerScope = CoroutineScope(Dispatchers.IO)
        val player = AudioPlayer(
            scope = playerScope,
            onProgress = { /* MPRIS handles progress via interval if configured, otherwise ignore CLI output */ },
            onFinish = { nextAction?.invoke() },
            onError = { _ -> nextAction?.invoke() }
        )
        val sessionManager = MediaSessionManager(
            httpClient = httpClient,
            onPlayPause = { playPauseAction?.invoke() },
            onNext = { nextAction?.invoke() },
            onPrevious = { prevAction?.invoke() },
            onStop = { stopAction?.invoke() }
        )
        sessionManager.init()
        val playCurrentTrackFromQueue = suspend {
            val url = getStream(currentTrack)
            if (url != null) {
                terminal.println(green("▶ Playing: ") + currentTrack.title + gray(" by ") + currentTrack.artist)
                player.play(url)
                isPlaying = true
                sessionManager.updateTrack(currentTrack, currentTrack.durationMs)
            } else {
                terminal.println(yellow("⚠️ Failed to get stream for: ") + currentTrack.title)
                nextAction?.invoke()
            }
        }
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
        }
        nextAction = {
            playerScope.launch {
                if (queueIndex + 1 < radioQueue.size) {
                    queueIndex++
                    currentTrack = radioQueue[queueIndex]
                    playCurrentTrackFromQueue()
                } else {
                    if (shouldFetchSimilar) {
                        try {
                            terminal.println(gray("Fetching similar tracks..."))
                            val similarRaw =
                                getSimilarTracks(currentTrack.artist, currentTrack.title).take(5)
                            val similarTracksResolved = similarRaw.mapNotNull { sim ->
                                searchTracks("${sim.title} ${sim.artist}").firstOrNull()
                            }.filter { track -> radioQueue.none { it.id == track.id } }
                            if (similarTracksResolved.isNotEmpty()) {
                                radioQueue.addAll(similarTracksResolved)
                                queueIndex++
                                currentTrack = radioQueue[queueIndex]
                                playCurrentTrackFromQueue()
                            } else {
                                terminal.println(gray("No more related tracks found."))
                                stopAction?.invoke()
                            }
                        } catch (e: Exception) {
                            terminal.println(gray("Failed to fetch similar tracks: \${e.message}"))
                            stopAction?.invoke()
                        }
                    } else {
                        stopAction?.invoke()
                    }
                }
            }
        }
        prevAction = {
            playerScope.launch {
                if (queueIndex > 0) {
                    queueIndex--
                    currentTrack = radioQueue[queueIndex]
                    playCurrentTrackFromQueue()
                } else {
                    player.seek(0)
                }
            }
        }
        stopAction = {
            player.stop()
            sessionManager.notifyStopped()
            sessionManager.destroy()
            isPlaying = false
            stopSignal.complete(Unit)
            terminal.println(cyan("Playback stopped."))
        }
        playerScope.launch {
            playCurrentTrackFromQueue()
        }
        // Keep running until stopAction is invoked (e.g. by session manager directly or error)
        try {
            stopSignal.await()
        } finally {
            player.stop()
            sessionManager.destroy()
        }
        exitProcess(0)
    }
}
