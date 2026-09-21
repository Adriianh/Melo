package com.github.adriianh.cli.tui.component.screen

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.checkYouTubeAuth
import com.github.adriianh.cli.tui.loadHomeFeed
import com.github.adriianh.cli.tui.handler.loadStats
import com.github.adriianh.cli.tui.handler.restoreLastSession
import com.github.adriianh.cli.tui.handler.syncYouTubeLibrary
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Duration

internal fun MeloScreen.onStartLifecycle() {
    mediaSession.init()
    observePlaybackManager()
    if (settingsViewState.currentSettings.discordRpcEnabled) {
        discordRpcManager.connect()
    }
    scope.launch {
        getFavorites().collect { tracks ->
            appRunner()?.runOnRenderThread {
                state = state.copy(collections = state.collections.copy(favorites = tracks))
            }
        }
    }
    scope.launch {
        getFavoriteEntities?.invoke()?.collect { entities ->
            appRunner()?.runOnRenderThread {
                state =
                    state.copy(collections = state.collections.copy(favoriteEntities = entities))
            }
        }
    }
    scope.launch {
        getRecentTracks(100).collect { entries ->
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
    scope.launch { checkYouTubeAuth() }
    scope.launch { restoreLastSession() }
    scope.launch { loadHomeFeed() }
    scope.launch { loadStats() }
    scope.launch {
        var lastCookies: String? = null
        var isFirstEmit = true
        getSettings().collect { settings ->
            val cookiesChanged = !isFirstEmit && settings.sessionCookies != lastCookies
            val isInitialWithCookies = isFirstEmit && !settings.sessionCookies.isNullOrBlank()
            lastCookies = settings.sessionCookies
            isFirstEmit = false
            appRunner()?.runOnRenderThread {
                MeloTheme.loadTheme(settings.theme)
                settingsViewState = settingsViewState.copy(currentSettings = settings)
                state = state.copy(
                    isOfflineMode = settings.offlineMode,
                    languagePicker = state.languagePicker.copy(
                        currentLanguage = settings.searchLanguage.ifBlank { "es" }
                    )
                )
            }
            if (cookiesChanged || isInitialWithCookies) {
                checkYouTubeAuth()
                syncYouTubeLibrary()
            }
        }
    }
    marqueeJob = appRunner()?.scheduleRepeating({
        appRunner()?.runOnRenderThread {
            marqueeTick++
            if (marqueeTick > 10) {
                val track = state.detail.selectedTrack ?: return@runOnRenderThread

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
    playbackManager.release()
    mediaSession.release()
    discordRpcManager.disconnect()
    scope.cancel()
}