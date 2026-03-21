package com.github.adriianh.cli.tui

import com.github.adriianh.cli.config.shareDir
import com.github.adriianh.cli.tui.component.*
import com.github.adriianh.cli.tui.graphics.ClearGraphicsElement
import com.github.adriianh.cli.tui.handler.*
import com.github.adriianh.cli.tui.handler.playback.*
import com.github.adriianh.cli.tui.handler.search.handleDetailKey
import com.github.adriianh.cli.tui.handler.search.handleResultsKey
import com.github.adriianh.cli.tui.handler.search.handleSearchBarKey
import com.github.adriianh.cli.tui.handler.search.performSearch
import com.github.adriianh.cli.tui.handler.settings.handleSettingsKey
import com.github.adriianh.cli.tui.player.AudioPlayer
import com.github.adriianh.cli.tui.player.MediaSessionManager
import com.github.adriianh.cli.tui.screen.*
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.cli.tui.util.TextAnimationUtil.marqueeText
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.ArtworkProvider
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.usecase.*
import com.github.adriianh.data.remote.piped.PipedApiClient
import dev.tamboui.layout.Constraint
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.app.ToolkitApp
import dev.tamboui.toolkit.app.ToolkitRunner
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.tui.TuiConfig
import dev.tamboui.widgets.input.TextInputState
import io.ktor.client.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.time.Duration

class MeloScreen(
    // Shared infrastructure
    internal val httpClient: HttpClient,
    // Search
    internal val searchTracks: SearchTracksUseCase,
    internal val loadMoreTracks: LoadMoreTracksUseCase,
    internal val getTrack: GetTrackUseCase,
    internal val getLyrics: GetLyricsUseCase,
    internal val getSyncedLyrics: GetSyncedLyricsUseCase,
    internal val getSimilarTracks: GetSimilarTracksUseCase,
    internal val pipedApiClient: PipedApiClient,
    // Favorites
    internal val getFavorites: GetFavoritesUseCase,
    internal val addFavorite: AddFavoriteUseCase,
    internal val removeFavorite: RemoveFavoriteUseCase,
    internal val isFavoriteUseCase: IsFavoriteUseCase,
    // History & playback
    internal val getRecentTracks: GetRecentTracksUseCase,
    internal val recordPlay: RecordPlayUseCase,
    internal val getStream: GetStreamUseCase,
    // Playlists
    internal val getPlaylists: GetPlaylistsUseCase,
    internal val getPlaylistTracks: GetPlaylistTracksUseCase,
    internal val createPlaylist: CreatePlaylistUseCase,
    internal val renamePlaylist: RenamePlaylistUseCase,
    internal val deletePlaylist: DeletePlaylistUseCase,
    internal val addTrackToPlaylist: AddTrackToPlaylistUseCase,
    internal val removeTrackFromPlaylist: RemoveTrackFromPlaylistUseCase,
    // Session
    internal val saveSession: SaveSessionUseCase,
    internal val restoreSession: RestoreSessionUseCase,
    internal val clearSession: ClearSessionUseCase,
    // Scrobbling
    internal val updateNowPlaying: UpdateNowPlayingUseCase,
    internal val scrobble: ScrobbleUseCase,
    // Statistics
    internal val getTopTracks: GetTopTracksUseCase,
    internal val getTopArtists: GetTopArtistsUseCase,
    internal val getListeningStats: GetListeningStatsUseCase,
    // Settings
    internal val getSettings: GetSettingsUseCase,
    internal val updateSettings: UpdateSettingsUseCase,
    // Offline
    internal val offlineRepository: OfflineRepository,
    internal val getOfflineTracks: GetOfflineTracksUseCase,
    internal val syncOfflineTracks: SyncOfflineTracksUseCase,
    internal val downloadTrack: DownloadTrackUseCase,
    internal val deleteDownloadedTrack: DeleteDownloadedTrackUseCase,
    internal val markTrackAccessed: MarkTrackAccessedUseCase,
    internal val autoCleanup: AutoCleanupUseCase,
    // Artwork
    internal val artworkRenderer: ArtworkRenderer,
    internal val artworkProvider: ArtworkProvider,
    internal val audioProvider: AudioProvider,
    dispatcher: CoroutineDispatcher
) : ToolkitApp() {

    internal var state = MeloState()
    internal val scope = CoroutineScope(dispatcher)
    internal var detailsJob: Job? = null
    internal var loadMoreJob: Job? = null
    internal var playlistTracksJob: Job? = null
    internal var nowPlayingMetadataJob: Job? = null
    internal var lastQuery = ""
    internal var marqueeJob: ToolkitRunner.ScheduledAction? = null
    internal var marqueeTick = 0
    internal var scrobbleSubmitted = false
    internal var trackStartedAt = 0L
    internal var updateNowPlayingJob: Job? = null
    internal var scrobbleJob: Job? = null
    internal val downloadSemaphore = Semaphore(2)

    internal var settingsViewState = SettingsViewState()

    /**
     * Helper to update the current screen state in a type-safe way.
     */
    internal inline fun <reified T : ScreenState> updateScreen(update: (T) -> T) {
        val current = state.screen
        if (current is T) {
            state = state.copy(screen = update(current))
        }
    }

    /** Exposes the protected runner() for internal extension functions. */
    internal fun appRunner() = runner()

    internal val mediaSession = MediaSessionManager(
        httpClient = httpClient,
        onPlayPause = {
            runner()?.runOnRenderThread { togglePlayPause() }
        },
        onNext = {
            runner()?.runOnRenderThread { seekForward() }
        },
        onPrevious = {
            runner()?.runOnRenderThread { seekBackward() }
        },
        onStop = {
            runner()?.runOnRenderThread {
                audioPlayer.stop()
                state = state.copy(player = state.player.copy(isPlaying = false, progress = 0.0))
            }
        },
    )

    internal var resolveStreamJob: Job? = null

    internal val audioPlayer: AudioPlayer = AudioPlayer(
        scope = scope,
        onProgress = { elapsedMs ->
            runner()?.runOnRenderThread {
                val duration = state.player.nowPlaying?.durationMs ?: 0L
                val progress = if (duration > 0) (elapsedMs.toDouble() / duration).coerceIn(0.0, 1.0) else 0.0
                state = state.copy(player = state.player.copy(nowPlayingPositionMs = elapsedMs, progress = progress))
                mediaSession.updatePosition(elapsedMs)
                state.player.nowPlaying?.let { onTrackProgress(it, elapsedMs, trackStartedAt) }
            }
        },
        onFinish = {
            runner()?.runOnRenderThread {
                state =
                    state.copy(player = state.player.copy(isPlaying = false, isLoadingAudio = false, progress = 0.0))
                seekForward()
            }
        },
        onError = { err ->
            runner()?.runOnRenderThread {
                state = state.copy(
                    player = state.player.copy(
                        isPlaying = false,
                        isLoadingAudio = false,
                        audioError = err.message
                    )
                )
                mediaSession.notifyStopped()
            }
        },
    )

    internal val searchInputState = TextInputState()

    internal val homeRecentList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .focusable()
        .id("home-recent-list")

    internal val homeFavoritesList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .focusable()
        .id("home-favorites-list")

    internal val resultList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()

    internal val favoritesList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("library-list")

    internal val playlistsList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("playlists-list")

    internal val playlistTracksList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("playlist-tracks-list")

    internal val localLibraryList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("local-library-list")

    internal val sidebarNavList: ListElement<*> = list()
        .items(
            "${MeloTheme.ICON_HOME} Home",
            "${MeloTheme.ICON_SEARCH} Search",
            "${MeloTheme.ICON_LIBRARY} Your Library",
            "${MeloTheme.ICON_NOW_PLAYING} Now Playing",
        )
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .selected(SidebarSection.HOME.ordinal)

    internal val sidebarUtilList: ListElement<*> = list()
        .items(
            "${MeloTheme.ICON_STATS} Statistics",
            "${MeloTheme.ICON_OFFLINE} Downloads",
            "${MeloTheme.ICON_SETTINGS}  Settings",
        )
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .selected(-1)

    internal val lyricsArea = markupTextArea()
        .scrollbar()
        .wrapWord()
        .focusable()
        .id("lyrics-area")

    internal val similarArea: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_BULLET} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("similar-area")

    internal val queueList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("queue-list")

    internal val offlineList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("offline-list")

    internal val settingsSectionList: ListElement<*> = list()
        .highlightSymbol("> ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .focusable()
        .id("settings-section-list")

    internal val settingsList: ListElement<*> = list()
        .highlightSymbol("> ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .focusable()
        .id("settings-list")

    private val playlistInputOverlay = PlaylistInputOverlay { state }
    private val playlistPickerOverlay = PlaylistPickerOverlay { state }
    private val queueOverlay = QueueOverlay({ state }, queueList, ::handleQueueKey)
    private val settingsOverlay = SettingsOverlay(
        { state },
        { settingsViewState },
        settingsSectionList,
        settingsList,
        ::handleSettingsKey
    )
    private val directoryPickerOverlay = DirectoryPickerOverlay(
        { settingsViewState },
        ::handleSettingsKey
    )
    private val trackOptionsOverlay = TrackOptionsOverlay({ state }, ::handleTrackOptionsKey)

    override fun configure(): TuiConfig = TuiConfig.builder().mouseCapture(true).build()

    override fun onStart() {
        mediaSession.init()
        scope.launch {
            getFavorites().collect { tracks ->
                runner()?.runOnRenderThread {
                    state = state.copy(collections = state.collections.copy(favorites = tracks))
                }
            }
        }
        scope.launch {
            getRecentTracks(20).collect { entries ->
                runner()?.runOnRenderThread {
                    state = state.copy(collections = state.collections.copy(recentTracks = entries))
                }
            }
        }
        scope.launch {
            getPlaylists().collect { playlists ->
                runner()?.runOnRenderThread {
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
                runner()?.runOnRenderThread {
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
                runner()?.runOnRenderThread {
                    MeloTheme.loadTheme(settings.theme)
                    audioPlayer.setVolume(settings.volume)
                    settingsViewState = settingsViewState.copy(currentSettings = settings)
                    state = state.copy(isOfflineMode = settings.offlineMode)
                }
            }
        }
        marqueeJob = runner()?.scheduleRepeating({
            runner()?.runOnRenderThread {
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

    override fun onStop() {
        marqueeJob?.cancel()
        playlistTracksJob?.cancel()
        audioPlayer.stop()
        mediaSession.destroy()
        runBlocking { persistSession() }
        scope.cancel()
    }

    override fun render(): Element {
        val mainLayout = dock()
            .top(buildSearchBar(searchInputState, ::performSearch, ::handleSearchBarKey), Constraint.length(3))
            .bottom(
                buildPlayerBar(
                    state, ::formatDuration, ::handlePlayerBarKey,
                    ::togglePlayPause, ::adjustVolume, ::seekForward, ::seekBackward,
                    ::toggleShuffle, ::cycleRepeat, ::toggleQueue,
                ),
                Constraint.length(4),
            )
            .left(
                buildSidebar(sidebarNavList, sidebarUtilList, state.navigation.sidebarInUtil, ::handleSidebarKey),
                Constraint.length(22)
            )
            .center(renderMainContent())

        val withQueue = if (state.player.isQueueVisible) stack(mainLayout, queueOverlay) else mainLayout
        val withSettings = if (state.isSettingsVisible) stack(withQueue, settingsOverlay) else withQueue
        val withDirectoryPicker = if (settingsViewState.isPickingDirectory)
            stack(withSettings, directoryPickerOverlay) else withSettings
        val withTrackOptions =
            if (state.trackOptions.isVisible) stack(withDirectoryPicker, trackOptionsOverlay) else withDirectoryPicker

        return when (state.playlistInteraction.playlistInputMode) {
            PlaylistInputMode.CREATE,
            PlaylistInputMode.RENAME -> stack(withTrackOptions, playlistInputOverlay)

            PlaylistInputMode.PICKER -> stack(withTrackOptions, playlistPickerOverlay)
            PlaylistInputMode.NONE -> withTrackOptions
        }
    }

    private fun renderMainContent(): Element {
        if (state.needsGraphicsClear) {
            val pending = state.navigation.pendingSection
            val targetSection = pending ?: state.navigation.activeSection
            val targetScreen = when (targetSection) {
                SidebarSection.HOME -> ScreenState.Home()
                SidebarSection.SEARCH -> ScreenState.Search()
                SidebarSection.LIBRARY -> ScreenState.Library()
                SidebarSection.NOW_PLAYING -> ScreenState.NowPlaying()
                SidebarSection.STATS -> ScreenState.Stats()
                SidebarSection.OFFLINE -> ScreenState.Offline(downloads = state.collections.offlineTracks)
                SidebarSection.SETTINGS -> state.screen
            }
            state = state.copy(
                needsGraphicsClear = false,
                navigation = state.navigation.copy(activeSection = targetSection, pendingSection = null),
                screen = targetScreen,
                detail = state.detail.copy(artworkData = if (targetSection != SidebarSection.SEARCH) null else state.detail.artworkData),
            )
            if (targetSection == SidebarSection.NOW_PLAYING) {
                appRunner()?.focusManager()?.setFocus("now-playing-panel")
            }
            val targetContent = when (targetSection) {
                SidebarSection.HOME -> renderHomeScreen(
                    state, homeRecentList, homeFavoritesList,
                    onKeyEvent = ::handleHomeKey,
                )

                SidebarSection.SEARCH -> renderSearchScreen(
                    state, resultList, lyricsArea, similarArea,
                    ::marqueeText, ::handleResultsKey, ::handleDetailKey,
                )

                SidebarSection.LIBRARY -> renderLibraryScreen(
                    state,
                    settingsViewState,
                    favoritesList,
                    playlistsList,
                    playlistTracksList,
                    localLibraryList,
                    ::handleLibraryKey,
                )

                SidebarSection.NOW_PLAYING -> renderNowPlayingScreen(state, ::marqueeText, ::handlePlayerBarKey)
                SidebarSection.STATS -> renderStatsScreen(state, ::handleStatsKey)
                SidebarSection.OFFLINE -> renderOfflineScreen(state, offlineList, ::handleOfflineKey)
                SidebarSection.SETTINGS -> renderHomeScreen(
                    state,
                    homeRecentList,
                    homeFavoritesList,
                    onKeyEvent = ::handleHomeKey
                )
            }
            return stack(ClearGraphicsElement().fill(), targetContent)
        }

        return when (state.screen) {
            is ScreenState.Home -> renderHomeScreen(
                state, homeRecentList, homeFavoritesList,
                onKeyEvent = ::handleHomeKey,
            )

            is ScreenState.Search -> renderSearchScreen(
                state, resultList, lyricsArea, similarArea,
                ::marqueeText, ::handleResultsKey, ::handleDetailKey,
            )

            is ScreenState.Library -> renderLibraryScreen(
                state,
                settingsViewState,
                favoritesList,
                playlistsList,
                playlistTracksList,
                localLibraryList,
                ::handleLibraryKey,
            )

            is ScreenState.NowPlaying -> renderNowPlayingScreen(state, ::marqueeText, ::handlePlayerBarKey)
            is ScreenState.Stats -> renderStatsScreen(state, ::handleStatsKey)
            is ScreenState.Offline -> renderOfflineScreen(state, offlineList, ::handleOfflineKey)
        }
    }

    internal fun loadLocalTracks() {
        scope.launch {
            updateScreen<ScreenState.Library> { it.copy(isLoading = true) }
            val paths = settingsViewState.currentSettings.localLibraryPaths
            val tracks = offlineRepository.scanLocalTracks(paths)
            runner()?.runOnRenderThread {
                updateScreen<ScreenState.Library> {
                    it.copy(localTracks = tracks, isLoading = false)
                }
            }
        }
    }

    internal fun deleteDownloadedTrack(trackId: String) {
        scope.launch {
            deleteDownloadedTrack.invoke(trackId)
        }
    }

    internal fun downloadTrack(track: Track, downloadType: DownloadType = DownloadType.PREFETCH) {
        scope.launch {
            downloadSemaphore.withPermit {
                try {
                    val existing = offlineRepository.getOfflineTrack(track.id)
                    val sourceId = track.sourceId ?: audioProvider.getSourceId(
                        artist = track.artist,
                        title = track.title,
                        durationMs = track.durationMs,
                    ) ?: return@launch
                    val downloadsDir = File(
                        when (downloadType) {
                            DownloadType.MANUAL -> settingsViewState.currentSettings.downloadPath
                                ?: File(shareDir, "downloads").absolutePath

                            DownloadType.PREFETCH -> settingsViewState.currentSettings.cachePath
                                ?: File(shareDir, "cache").absolutePath
                        }
                    )
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()

                    if (existing != null && (existing.downloadStatus == DownloadStatus.COMPLETED || existing.downloadStatus == DownloadStatus.DOWNLOADING)) {
                        // If it's completed as cache and user wants manual, just copy the file
                        if (existing.downloadStatus == DownloadStatus.COMPLETED &&
                            existing.downloadType == DownloadType.PREFETCH &&
                            downloadType == DownloadType.MANUAL &&
                            existing.localFilePath != null
                        ) {
                            val cachedFile = File(existing.localFilePath!!)
                            if (cachedFile.exists()) {
                                val newFile = File(downloadsDir, cachedFile.name)
                                cachedFile.copyTo(newFile, overwrite = true)

                                val upgradedTrack = existing.copy(
                                    localFilePath = newFile.absolutePath,
                                    downloadType = DownloadType.MANUAL,
                                    downloadedAt = System.currentTimeMillis()
                                )
                                downloadTrack.invoke(upgradedTrack)
                                return@launch
                            }
                        }
                        return@launch
                    }

                    val offlineTrack = OfflineTrack(
                        track = track,
                        downloadStatus = DownloadStatus.DOWNLOADING,
                        downloadType = downloadType
                    )
                    downloadTrack.invoke(offlineTrack)

                    val downloadedPath = audioProvider.downloadAudio(
                        source = sourceId,
                        destination = downloadsDir.absolutePath,
                        format = settingsViewState.currentSettings.downloadFormat.displayName,
                        quality = settingsViewState.currentSettings.downloadQuality.displayName,
                        embedMetadata = (downloadType == DownloadType.MANUAL)
                    )
                    if (downloadedPath != null) {
                        val file = File(downloadedPath)
                        val completedTrack = offlineTrack.copy(
                            localFilePath = file.absolutePath,
                            downloadStatus = DownloadStatus.COMPLETED,
                            fileSize = file.length(),
                            downloadedAt = System.currentTimeMillis()
                        )
                        downloadTrack.invoke(completedTrack)
                        autoCleanup.invoke(
                            maxAgeDays = settingsViewState.currentSettings.maxOfflineAgeDays,
                            maxSizeMb = settingsViewState.currentSettings.maxOfflineSizeMb
                        )
                    } else {
                        downloadTrack.invoke(
                            offlineTrack.copy(downloadStatus = DownloadStatus.FAILED)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    downloadTrack.invoke(
                        OfflineTrack(
                            track = track,
                            downloadStatus = DownloadStatus.FAILED
                        )
                    )
                }
            }
        }
    }
}