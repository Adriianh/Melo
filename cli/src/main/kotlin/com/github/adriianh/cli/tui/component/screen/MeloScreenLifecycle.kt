package com.github.adriianh.cli.tui.component.screen

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.persistSession
import com.github.adriianh.cli.tui.handler.restoreLastSession
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Duration

internal fun MeloScreen.onStartLifecycle() {
    mediaSession.init()
    scope.launch {
        getFavorites().collect { tracks ->
            appRunner()?.runOnRenderThread {
                state = state.copy(collections = state.collections.copy(favorites = tracks))
            }
        }
    }
    scope.launch {
        getRecentTracks(20).collect { entries ->
            appRunner()?.runOnRenderThread {
                state = state.copy(collections = state.collections.copy(recentTracks = entries))
            }
        }
    }
    scope.launch {
        getPlaylists().collect { playlists ->
            appRunner()?.runOnRenderThread {
                state = state.copy(collections = state.collections.copy(playlists = playlists))
            }
        }
    }
    scope.launch {
        syncOfflineTracks.invoke()
        autoCleanup.invoke(
            maxAgeDays = settingsViewState.currentSettings.maxOfflineAgeDays,
            maxSizeMb = settingsViewState.currentSettings.maxOfflineSizeMb,
        )
    }
    scope.launch {
        getOfflineTracks().collect { downloads ->
            appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Offline> { it.copy(downloads = downloads) }
                state = state.copy(
                    collections = state.collections.copy(offlineTracks = downloads)
                )
            }
        }
    }
    scope.launch { restoreLastSession() }
    scope.launch {
        getSettings().collect { settings ->
            appRunner()?.runOnRenderThread {
                MeloTheme.loadTheme(settings.theme)
                audioPlayer.setVolume(settings.volume)
                settingsViewState = settingsViewState.copy(currentSettings = settings)
                state = state.copy(isOfflineMode = settings.offlineMode)
            }
        }
    }
    marqueeJob = appRunner()?.scheduleRepeating({
        appRunner()?.runOnRenderThread {
            marqueeTick++
            if (marqueeTick > 10) {
                val track = state.detail.selectedTrack ?: return@runOnRenderThread

                // Skip state copies if text is short enough to not need a marquee
                if (track.title.length <= 30 && track.artist.length <= 30) return@runOnRenderThread

                val newOffset = state.player.marqueeOffset + 1
                val separator = "   •   "
                val full = track.title + separator
                if (newOffset % full.length == 0) marqueeTick = 0

                state = state.copy(player = state.player.copy(marqueeOffset = newOffset))
            }
        }
    }, Duration.ofMillis(150))
}

internal fun MeloScreen.onStopLifecycle() {
    marqueeJob?.cancel()
    playlistTracksJob?.cancel()
    audioPlayer.stop()
    mediaSession.destroy()
    runBlocking { persistSession() }
    scope.cancel()
}