package com.github.adriianh.cli.tui.component.screen

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.checkYouTubeAuth
import com.github.adriianh.cli.tui.handler.loadStats
import com.github.adriianh.cli.tui.handler.restoreLastSession
import com.github.adriianh.cli.tui.handler.syncYouTubeLibrary
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.loadHomeFeed
import com.github.adriianh.cli.tui.player.FfplayProcessManager
import com.github.adriianh.cli.tui.util.EQUALIZER_TICK_MS
import com.github.adriianh.cli.tui.util.TOAST_TICK_MS
import com.github.adriianh.cli.tui.util.ToastKind
import com.github.adriianh.cli.tui.util.pruneExpiredToasts
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.util.MeloVersion
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Duration

internal fun MeloScreen.onStartLifecycle() {
    appRunner()?.focusManager()?.setFocus("home-panel")
    mediaSession.init()
    observePlaybackManager()
    if (settingsViewState.currentSettings.discordRpcEnabled) {
        discordRpcManager.connect()
    }

    val cachedLib = libraryCache?.getSync()
    if (cachedLib != null) {
        state =
            state.copy(
                collections =
                    state.collections.copy(
                        remotePlaylists = cachedLib.playlists,
                        remoteFavorites = cachedLib.likedSongs,
                        remoteAlbums = cachedLib.albums,
                        remoteArtists = cachedLib.artists,
                        remoteRecentTracks = cachedLib.remoteHistory,
                    ),
            )
    } else {
        scope.launch {
            val asyncLib = libraryCache?.get() ?: return@launch
            appRunner()?.runOnRenderThread {
                state =
                    state.copy(
                        collections =
                            state.collections.copy(
                                remotePlaylists = asyncLib.playlists,
                                remoteFavorites = asyncLib.likedSongs,
                                remoteAlbums = asyncLib.albums,
                                remoteArtists = asyncLib.artists,
                                remoteRecentTracks = asyncLib.remoteHistory,
                            ),
                    )
            }
        }
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
                state =
                    state.copy(
                        collections = state.collections.copy(offlineTracks = downloads),
                    )

                val currentIds = downloads.map { it.track.id }.toSet()
                val freshlyCompleted =
                    downloads.filter { offline ->
                        val previous = lastDownloadStatusById[offline.track.id]
                        offline.downloadStatus == DownloadStatus.COMPLETED &&
                            previous != null &&
                            previous != DownloadStatus.COMPLETED
                    }
                lastDownloadStatusById.keys.retainAll(currentIds)
                downloads.forEach { lastDownloadStatusById[it.track.id] = it.downloadStatus }
                freshlyCompleted.forEach { offline ->
                    showToast("Download complete: ${offline.track.title}", ToastKind.SUCCESS)
                }
            }
        }
    }
    if (settingsViewState.currentSettings.autoCheckUpdates && checkForUpdate != null) {
        scope.launch {
            checkForUpdate.invoke(MeloVersion.CURRENT).onSuccess { release ->
                if (release != null) {
                    appRunner()?.runOnRenderThread {
                        showToast(
                            "Update available: v${release.version} (run ':update' or 'melo update')",
                            ToastKind.INFO,
                        )
                    }
                }
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
        var wasOnline: Boolean? = null
        val isOnlineFlow = networkMonitor?.isOnline ?: MutableStateFlow(true)
        combine(getSettings(), isOnlineFlow) { settings, isOnline ->
            settings to isOnline
        }.collect { (settings, isOnline) ->
            val cookiesChanged = !isFirstEmit && settings.sessionCookies != lastCookies
            val isInitialWithCookies = isFirstEmit && !settings.sessionCookies.isNullOrBlank()
            val reconnected = wasOnline == false && isOnline

            wasOnline = isOnline
            lastCookies = settings.sessionCookies
            isFirstEmit = false

            val isOffline = settings.offlineMode || !isOnline
            val unplayableNowPlaying = state.player.nowPlaying?.let { !state.isPlayable(it) } ?: false
            val unplayableBlocked = isOffline && unplayableNowPlaying
            val shouldResetAudio = state.player.isLoadingAudio && (reconnected || unplayableBlocked)

            appRunner()?.runOnRenderThread {
                MeloTheme.loadTheme(settings.theme)
                settingsViewState = settingsViewState.copy(currentSettings = settings)
                state =
                    state.copy(
                        isOfflineMode = isOffline,
                        languagePicker =
                            state.languagePicker.copy(
                                currentLanguage = settings.searchLanguage.ifBlank { "es" },
                            ),
                        player =
                            if (shouldResetAudio) {
                                state.player.copy(isLoadingAudio = false)
                            } else {
                                state.player
                            },
                    )

                if (reconnected) {
                    showToast("Reconnected to the internet", ToastKind.SUCCESS)
                }
            }
            if (cookiesChanged || isInitialWithCookies || reconnected) {
                checkYouTubeAuth()
                syncYouTubeLibrary()
            }
            if (reconnected) {
                loadHomeFeed()
            }
        }
    }
    marqueeJob =
        appRunner()?.scheduleRepeating({
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

    toastJob =
        appRunner()?.scheduleRepeating({
            appRunner()?.runOnRenderThread {
                if (state.toasts.isNotEmpty()) {
                    state =
                        state.copy(
                            toasts = pruneExpiredToasts(state.toasts, System.currentTimeMillis()),
                        )
                }
            }
        }, Duration.ofMillis(TOAST_TICK_MS))

    equalizerJob =
        appRunner()?.scheduleRepeating({
            appRunner()?.runOnRenderThread {
                if (state.player.isPlaying) {
                    state =
                        state.copy(
                            player =
                                state.player.copy(
                                    equalizerTick = state.player.equalizerTick + 1,
                                ),
                        )
                }
            }
        }, Duration.ofMillis(EQUALIZER_TICK_MS))
}

internal fun MeloScreen.onStopLifecycle() {
    marqueeJob?.cancel()
    toastJob?.cancel()
    equalizerJob?.cancel()
    playlistTracksJob?.cancel()
    try {
        playbackManager.release()
    } catch (_: Throwable) {
    }
    try {
        audioPlayer.release()
    } catch (_: Throwable) {
    }
    try {
        mediaSession.release()
    } catch (_: Throwable) {
    }
    try {
        discordRpcManager.disconnect()
    } catch (_: Throwable) {
    }
    try {
        FfplayProcessManager.killAll()
    } catch (_: Throwable) {
    }
    scope.cancel()
}
