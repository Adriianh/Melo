package com.github.adriianh.cli.tui

import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.service.YouTubeAuthService
import com.github.adriianh.cli.tui.component.CommandBarSuggestionsOverlay
import com.github.adriianh.cli.tui.component.DirectoryPickerOverlay
import com.github.adriianh.cli.tui.component.PlaylistInputOverlay
import com.github.adriianh.cli.tui.component.PlaylistPickerOverlay
import com.github.adriianh.cli.tui.component.QueueOverlay
import com.github.adriianh.cli.tui.component.SearchSuggestionsOverlay
import com.github.adriianh.cli.tui.component.SettingsOverlay
import com.github.adriianh.cli.tui.component.SettingsViewState
import com.github.adriianh.cli.tui.component.TrackOptionsOverlay
import com.github.adriianh.cli.tui.component.screen.deleteDownloadedTrackAction
import com.github.adriianh.cli.tui.component.screen.downloadTrackAction
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionNext
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionPlayPause
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionPrevious
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionStop
import com.github.adriianh.cli.tui.component.screen.loadLocalTracksAction
import com.github.adriianh.cli.tui.component.screen.onStartLifecycle
import com.github.adriianh.cli.tui.component.screen.onStopLifecycle
import com.github.adriianh.cli.tui.component.screen.renderRoot
import com.github.adriianh.cli.tui.handler.loadStats
import com.github.adriianh.cli.tui.handler.playback.handleQueueKey
import com.github.adriianh.cli.tui.handler.playback.handleTrackOptionsKey
import com.github.adriianh.cli.tui.handler.search.handleSearchQueryChange
import com.github.adriianh.cli.tui.handler.settings.handleSettingsKey
import com.github.adriianh.cli.tui.handler.syncYouTubeLibrary
import com.github.adriianh.cli.tui.player.AudioPlayer
import com.github.adriianh.cli.tui.service.DiscordRpcManager
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.core.domain.interactor.DiscoveryInteractors
import com.github.adriianh.core.domain.interactor.LibraryInteractors
import com.github.adriianh.core.domain.interactor.OfflineInteractors
import com.github.adriianh.core.domain.interactor.PlaybackInteractors
import com.github.adriianh.core.domain.interactor.SearchInteractors
import com.github.adriianh.core.domain.interactor.SessionInteractors
import com.github.adriianh.core.domain.interactor.SettingsInteractors
import com.github.adriianh.core.domain.interactor.StatsInteractors
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.player.JvmMediaSessionManager
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.provider.MetadataProvider
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.data.player.PlaybackManagerImpl
import com.github.adriianh.data.remote.piped.PipedApiClient
import dev.tamboui.toolkit.Toolkit.list
import dev.tamboui.toolkit.Toolkit.markupTextArea
import dev.tamboui.toolkit.app.ToolkitApp
import dev.tamboui.toolkit.app.ToolkitRunner
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.tui.TuiConfig
import dev.tamboui.widgets.input.TextInputState
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

class MeloScreen(
    // Shared infrastructure
    internal val httpClient: HttpClient,
    internal val pipedApiClient: PipedApiClient,
    // Interactors
    internal val searchInteractors: SearchInteractors,
    internal val discoveryInteractors: DiscoveryInteractors,
    internal val libraryInteractors: LibraryInteractors,
    internal val playbackInteractors: PlaybackInteractors,
    internal val offlineInteractors: OfflineInteractors,
    internal val statsInteractors: StatsInteractors,
    internal val sessionInteractors: SessionInteractors,
    internal val settingsInteractors: SettingsInteractors,
    // Additional dependencies
    internal val offlineRepository: OfflineRepository,
    internal val artworkRenderer: ArtworkRenderer,
    internal val metadataProvider: MetadataProvider,
    internal val audioProvider: AudioProvider,
    internal val discordRpcManager: DiscordRpcManager,
    internal val youTubeAuthService: YouTubeAuthService,
    dispatcher: CoroutineDispatcher
) : ToolkitApp() {

    // Bridging properties to keep existing code working during refactor
    internal val getHome get() = discoveryInteractors.getHome
    internal val searchTracks get() = searchInteractors.searchTracks
    internal val searchAlbums get() = searchInteractors.searchAlbums
    internal val searchArtists get() = searchInteractors.searchArtists
    internal val searchPlaylists get() = searchInteractors.searchPlaylists
    internal val loadMoreTracks get() = searchInteractors.loadMoreTracks
    internal val loadMoreArtists get() = searchInteractors.loadMoreArtists
    internal val loadMoreAlbums get() = searchInteractors.loadMoreAlbums
    internal val loadMorePlaylists get() = searchInteractors.loadMorePlaylists
    internal val getTrack get() = searchInteractors.getTrack
    internal val getLyrics get() = searchInteractors.getLyrics
    internal val getSyncedLyrics get() = searchInteractors.getSyncedLyrics
    internal val getSimilarTracks get() = searchInteractors.getSimilarTracks
    internal val getEntityDetails get() = searchInteractors.getEntityDetails
    internal val getArtistTags get() = searchInteractors.getArtistTags

    internal val getFavorites get() = libraryInteractors.getFavorites
    internal val addFavorite get() = libraryInteractors.addFavorite
    internal val removeFavorite get() = libraryInteractors.removeFavorite
    internal val isFavoriteUseCase get() = libraryInteractors.isFavorite
    internal val getFavoriteEntities get() = libraryInteractors.getFavoriteEntities
    internal val removeFavoriteEntity get() = libraryInteractors.removeFavoriteEntity
    internal val toggleFavoriteEntity get() = libraryInteractors.toggleFavoriteEntity

    internal val getRecentTracks get() = playbackInteractors.getRecentTracks
    internal val recordPlay get() = playbackInteractors.recordPlay
    internal val getStream get() = playbackInteractors.getStream
    internal val updateNowPlaying get() = playbackInteractors.updateNowPlaying
    internal val scrobble get() = playbackInteractors.scrobble

    internal val getPlaylists get() = libraryInteractors.getPlaylists
    internal val getPlaylistTracks get() = libraryInteractors.getPlaylistTracks
    internal val createPlaylist get() = libraryInteractors.createPlaylist
    internal val renamePlaylist get() = libraryInteractors.renamePlaylist
    internal val deletePlaylist get() = libraryInteractors.deletePlaylist
    internal val addTrackToPlaylist get() = libraryInteractors.addTrackToPlaylist
    internal val removeTrackFromPlaylist get() = libraryInteractors.removeTrackFromPlaylist
    internal val getLikedSongs get() = libraryInteractors.getLikedSongs
    internal val getUserPlaylists get() = libraryInteractors.getUserPlaylists
    internal val toggleLikeTrack get() = libraryInteractors.toggleLikeTrack
    internal val getUserAlbums get() = libraryInteractors.getUserAlbums
    internal val getUserArtists get() = libraryInteractors.getUserArtists
    internal val toggleLikeAlbum get() = libraryInteractors.toggleLikeAlbum
    internal val toggleLikePlaylist get() = libraryInteractors.toggleLikePlaylist
    internal val subscribeChannel get() = libraryInteractors.subscribeChannel

    internal val saveSession get() = sessionInteractors.saveSession
    internal val restoreSession get() = sessionInteractors.restoreSession
    internal val clearSession get() = sessionInteractors.clearSession

    internal val getTopTracks get() = statsInteractors.getTopTracks
    internal val getTopArtists get() = statsInteractors.getTopArtists
    internal val getListeningStats get() = statsInteractors.getListeningStats

    internal val getSettings get() = settingsInteractors.getSettings
    internal val updateSettings get() = settingsInteractors.updateSettings

    internal val getOfflineTracks get() = offlineInteractors.getOfflineTracks
    internal val syncOfflineTracks get() = offlineInteractors.syncOfflineTracks
    internal val downloadTrack get() = offlineInteractors.downloadTrack
    internal val deleteDownloadedTrack get() = offlineInteractors.deleteDownloadedTrack
    internal val markTrackAccessed get() = offlineInteractors.markTrackAccessed
    internal val autoCleanup get() = offlineInteractors.autoCleanup

    internal var cachedHomeScreen: ScreenState.Home = ScreenState.Home()
    internal var cachedStatsScreen: ScreenState.Stats = ScreenState.Stats()

    internal var state = MeloState(screen = cachedHomeScreen)
    internal val scope = CoroutineScope(dispatcher)
    internal var searchJob: Job? = null
    internal var suggestionsJob: Job? = null
    internal var detailsJob: Job? = null
    internal var loadMoreJob: Job? = null
    internal var playlistTracksJob: Job? = null
    internal var nowPlayingMetadataJob: Job? = null
    internal var lastQuery = ""
    internal var marqueeJob: ToolkitRunner.ScheduledAction? = null
    internal var marqueeTick = 0
    internal var scrobbleSubmitted = false
    internal var playRecorded = false
    internal var trackStartedAt = 0L
    internal var updateNowPlayingJob: Job? = null
    internal var scrobbleJob: Job? = null
    internal val downloadSemaphore = Semaphore(2)
    internal var enrichSectionJob: Job? = null
    internal val trackMetadataCache =
        java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()

    init {
        loadDiskMetadataCache()
    }

    internal var settingsViewState = SettingsViewState()

    /**
     * Helper to update the current screen state in a type-safe way.
     */
    @Suppress("UNCHECKED_CAST")
    internal inline fun <reified T : ScreenState> updateScreen(update: (T) -> T) {
        val current = state.screen
        if (current is T) {
            val updated = update(current)
            if (updated is ScreenState.Home) cachedHomeScreen = updated
            if (updated is ScreenState.Stats) cachedStatsScreen = updated
            state = state.copy(screen = updated)
        } else {
            if (T::class == ScreenState.Home::class) {
                cachedHomeScreen = update(cachedHomeScreen as T) as ScreenState.Home
            } else if (T::class == ScreenState.Stats::class) {
                cachedStatsScreen = update(cachedStatsScreen as T) as ScreenState.Stats
            }
        }
    }

    /** Exposes the protected runner() for internal extension functions. */
    internal fun appRunner() = runner()

    internal val mediaSession = JvmMediaSessionManager(
        httpClient = httpClient,
        onPlayPause = ::handleMediaSessionPlayPause,
        onNext = ::handleMediaSessionNext,
        onPrevious = ::handleMediaSessionPrevious,
        onStop = ::handleMediaSessionStop,
    )

    internal val audioPlayer: AudioPlayer = AudioPlayer(scope = scope)

    /**
     * Shared playback orchestrator (data layer). Single source of truth for the
     * queue, playback state and volume; the TUI mirrors its flows into PlayerState.
     * Owned by this screen and released in [onStopLifecycle].
     */
    internal val playbackManager: PlaybackManager = PlaybackManagerImpl(
        meloPlayer = audioPlayer,
        getStreamUseCase = getStream,
        scope = scope,
        getRadioUseCase = discoveryInteractors.getRadio,
        getSettingsUseCase = getSettings,
        offlineRepository = offlineRepository,
        updateSettingsUseCase = updateSettings,
        saveSessionUseCase = saveSession,
        restoreSessionUseCase = restoreSession,
        clearSessionUseCase = clearSession,
        ioDispatcher = dispatcher,
    )

    internal val searchInputState = TextInputState()

    @Volatile
    internal var lastObservedSearchQuery = ""

    init {
        observeSearchInput()
    }

    private fun observeSearchInput() {
        scope.launch {
            var lastObservedFocus = false
            var lastObservedFocusId: String? = null
            while (isActive) {
                val currentQuery = searchInputState.text()
                val focusedId = appRunner()?.focusManager()?.focusedId()
                val isFocused = focusedId == "search-bar"

                if (state.screen is ScreenState.Search) {
                    if (currentQuery != lastObservedSearchQuery) {
                        lastObservedSearchQuery = currentQuery
                        handleSearchQueryChange(currentQuery)
                    } else if (isFocused && !lastObservedFocus) {
                        handleSearchQueryChange(currentQuery)
                    } else if (!isFocused && lastObservedFocus) {
                        updateScreen<ScreenState.Search> { it.copy(isShowingSuggestions = false) }
                    }
                }

                if (focusedId != lastObservedFocusId) {
                    when (focusedId) {
                        "home-panel", "home-feed-items" -> {
                            val home = (state.screen as? ScreenState.Home) ?: cachedHomeScreen
                            if (home.feedSections.isEmpty() && !home.isLoadingFeed && home.feedError == null) {
                                loadHomeFeed()
                            }
                        }

                        "stats-panel" -> {
                            val stats = (state.screen as? ScreenState.Stats) ?: cachedStatsScreen
                            if (stats.statsListening == null && !stats.statsLoading) {
                                loadStats()
                            }
                        }

                        "library-panel" -> {
                            val isLoggedIn =
                                !settingsViewState.currentSettings.sessionCookies.isNullOrBlank()
                            if (isLoggedIn && state.collections.remotePlaylists.isEmpty()) {
                                syncYouTubeLibrary()
                            }
                        }
                    }
                    lastObservedFocusId = focusedId
                }

                lastObservedFocus = isFocused
                delay(100.milliseconds)
            }
        }
    }

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

    internal val entityTracksList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .focusable()
        .id("entity-tracks-list")

    internal val artistDashboardList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("artist-dashboard-list")

    internal val localLibraryList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("local-library-list")

    internal val homeFeedSectionList: ListElement<*> = list()
        .highlightSymbol("")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("home-feed-sections")

    internal val homeFeedItemList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("home-feed-items")

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
            "${MeloTheme.ICON_SETTINGS} Settings",
        )
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .selected(-1)

    internal val lyricsArea = markupTextArea()
        .scrollbar()
        .wrapWord()
        .focusable()
        .id("lyrics-area")

    internal val entityDescriptionArea = markupTextArea()
        .scrollbar()
        .wrapWord()
        .focusable()
        .id("desc-area")

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

    internal val playlistInputOverlay = PlaylistInputOverlay { state }
    internal val searchSuggestionsOverlay = SearchSuggestionsOverlay { state }
    internal val playlistPickerOverlay = PlaylistPickerOverlay { state }
    internal val queueOverlay = QueueOverlay({ state }, queueList, ::handleQueueKey)
    internal val settingsOverlay = SettingsOverlay(
        { state },
        { settingsViewState },
        settingsSectionList,
        settingsList,
        ::handleSettingsKey
    )
    internal val directoryPickerOverlay = DirectoryPickerOverlay(
        { settingsViewState },
        ::handleSettingsKey
    )
    internal val trackOptionsOverlay = TrackOptionsOverlay({ state }, ::handleTrackOptionsKey)
    internal val commandBarSuggestionsOverlay = CommandBarSuggestionsOverlay { state }

    override fun configure(): TuiConfig = TuiConfig.builder().mouseCapture(true).build()

    override fun onStart() = onStartLifecycle()

    override fun onStop() = onStopLifecycle()

    override fun render(): Element = renderRoot()

    internal fun loadLocalTracks() = loadLocalTracksAction()

    internal fun deleteDownloadedTrack(trackId: String) = deleteDownloadedTrackAction(trackId)

    internal fun downloadTrack(track: Track, downloadType: DownloadType = DownloadType.PREFETCH) =
        downloadTrackAction(track, downloadType)

    internal fun loadHomeFeed(chipParams: String? = null, chipIndex: Int = 0) {
        val currentHome = (state.screen as? ScreenState.Home) ?: cachedHomeScreen
        if (currentHome.isLoadingFeed && chipParams == null && chipIndex == currentHome.selectedChipIndex) return
        updateScreen<ScreenState.Home> { it.copy(isLoadingFeed = true, feedError = null) }
        scope.launch {
            try {
                val feed = getHome(params = chipParams)
                val allSections = feed.sections.toMutableList()
                val chips = feed.chips
                var nextContinuation = feed.continuation

                var continuationCount = 0
                while (!nextContinuation.isNullOrBlank() && allSections.size < 12 && continuationCount < 3) {
                    try {
                        val nextFeed = getHome(continuation = nextContinuation)
                        if (nextFeed.sections.isEmpty()) break
                        allSections.addAll(nextFeed.sections)
                        nextContinuation = nextFeed.continuation
                        continuationCount++
                    } catch (_: Exception) {
                        break
                    }
                }

                if (chipParams == null && allSections.size < 5) {
                    try {
                        val explore = discoveryInteractors.getExplore()
                        allSections.addAll(explore)
                    } catch (_: Exception) {
                    }
                    try {
                        val charts = discoveryInteractors.getCharts()
                        allSections.addAll(charts)
                    } catch (_: Exception) {
                    }
                }

                val distinctSections = allSections.distinctBy { it.title }

                appRunner()?.runOnRenderThread {
                    updateScreen<ScreenState.Home> { current ->
                        val preservedChips = chips.ifEmpty { current.feedChips }
                        current.copy(
                            feedSections = distinctSections,
                            feedChips = preservedChips,
                            feedContinuation = nextContinuation,
                            isLoadingMoreSections = false,
                            selectedChipIndex = chipIndex,
                            isLoadingFeed = false,
                            feedError = null,
                            selectedSectionIndex = 0,
                            selectedItemIndex = 0
                        )
                    }
                    enrichActiveSectionTracks()
                }
            } catch (e: Exception) {
                appRunner()?.runOnRenderThread {
                    updateScreen<ScreenState.Home> {
                        it.copy(
                            isLoadingFeed = false,
                            feedError = e.message ?: "Failed to load feed"
                        )
                    }
                }
            }
        }
    }

    internal fun loadMoreHomeSections() {
        val s = state.screen as? ScreenState.Home ?: return
        val continuation = s.feedContinuation ?: return
        if (s.isLoadingMoreSections || s.isLoadingFeed) return

        updateScreen<ScreenState.Home> { it.copy(isLoadingMoreSections = true) }
        scope.launch {
            try {
                val nextFeed = getHome(continuation = continuation)
                appRunner()?.runOnRenderThread {
                    updateScreen<ScreenState.Home> { current ->
                        val newSections =
                            (current.feedSections + nextFeed.sections).distinctBy { it.title }
                        current.copy(
                            feedSections = newSections,
                            feedContinuation = nextFeed.continuation,
                            isLoadingMoreSections = false
                        )
                    }
                }
            } catch (_: Exception) {
                appRunner()?.runOnRenderThread {
                    updateScreen<ScreenState.Home> { it.copy(isLoadingMoreSections = false) }
                }
            }
        }
    }

    internal fun enrichActiveSectionTracks() {
        val s = state.screen as? ScreenState.Home ?: return
        val sectionIndex = s.selectedSectionIndex
        val currentSection = s.feedSections.getOrNull(sectionIndex) ?: return

        val songsToEnrich = currentSection.items.mapIndexedNotNull { index, item ->
            if (item is SearchResult.Song) {
                val track = item.track
                if (track.album.isBlank() || track.durationMs <= 0L) {
                    index to track
                } else null
            } else null
        }

        if (songsToEnrich.isEmpty()) return

        enrichSectionJob?.cancel()
        enrichSectionJob = scope.launch(Dispatchers.IO) {
            val semaphore = Semaphore(6)
            val immediateUpdates = mutableListOf<Pair<Int, Track>>()
            val pendingNetwork = mutableListOf<Pair<Int, Track>>()

            for ((itemIndex, track) in songsToEnrich) {
                val cached = trackMetadataCache[track.id]
                if (cached != null && (cached.first.isNotBlank() || cached.second > 0L)) {
                    val updated = track.copy(
                        album = track.album.ifBlank { cached.first },
                        durationMs = if (track.durationMs > 0L) track.durationMs else cached.second
                    )
                    immediateUpdates.add(itemIndex to updated)
                    continue
                }

                val fav = state.collections.favorites.find { it.id == track.id }
                if (fav != null && (fav.album.isNotBlank() || fav.durationMs > 0L)) {
                    val updated = track.copy(
                        album = track.album.ifBlank { fav.album },
                        durationMs = if (track.durationMs > 0L) track.durationMs else fav.durationMs
                    )
                    trackMetadataCache[track.id] = updated.album to updated.durationMs
                    immediateUpdates.add(itemIndex to updated)
                    continue
                }

                pendingNetwork.add(itemIndex to track)
            }

            if (immediateUpdates.isNotEmpty()) {
                appRunner()?.runOnRenderThread {
                    applyEnrichedTracksBatch(sectionIndex, immediateUpdates)
                }
            }

            if (pendingNetwork.isEmpty()) return@launch

            val networkBatch = java.util.concurrent.ConcurrentLinkedQueue<Pair<Int, Track>>()
            coroutineScope {
                pendingNetwork.forEach { (itemIndex, track) ->
                    launch {
                        semaphore.withPermit {
                            if (!isActive) return@withPermit
                            try {
                                val fetched = getTrack(track.id)
                                if (fetched != null && (fetched.album.isNotBlank() || fetched.durationMs > 0L)) {
                                    val updated = track.copy(
                                        album = track.album.ifBlank { fetched.album },
                                        durationMs = if (track.durationMs > 0L) track.durationMs else fetched.durationMs
                                    )
                                    trackMetadataCache[track.id] =
                                        updated.album to updated.durationMs
                                    networkBatch.add(itemIndex to updated)
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
            }

            if (networkBatch.isNotEmpty() && isActive) {
                appRunner()?.runOnRenderThread {
                    applyEnrichedTracksBatch(sectionIndex, networkBatch.toList())
                }
                saveDiskMetadataCache()
            }
        }
    }

    private fun applyEnrichedTracksBatch(sectionIndex: Int, updates: List<Pair<Int, Track>>) {
        if (updates.isEmpty()) return
        updateScreen<ScreenState.Home> { current ->
            if (current.selectedSectionIndex != sectionIndex) return@updateScreen current
            val sec = current.feedSections.getOrNull(sectionIndex) ?: return@updateScreen current
            val newItems = sec.items.toMutableList()
            for ((idx, track) in updates) {
                if (idx in newItems.indices && newItems[idx] is SearchResult.Song) {
                    newItems[idx] = SearchResult.Song(track)
                }
            }
            val newSec = sec.copy(items = newItems)
            val newSections = current.feedSections.toMutableList()
            newSections[sectionIndex] = newSec
            current.copy(feedSections = newSections)
        }
    }

    private fun loadDiskMetadataCache() {
        try {
            val file = java.io.File(configDir, "metadata_cache.json")
            if (!file.exists()) return
            val text = file.readText()
            val parsed = Json.decodeFromString<Map<String, List<String>>>(text)
            for ((key, pair) in parsed) {
                val album = pair.getOrNull(0).orEmpty()
                val dur = pair.getOrNull(1)?.toLongOrNull() ?: 0L
                trackMetadataCache[key] = album to dur
            }
        } catch (_: Exception) {
        }
    }

    private fun saveDiskMetadataCache() {
        scope.launch(Dispatchers.IO) {
            try {
                val dir = java.io.File(configDir)
                if (!dir.exists()) dir.mkdirs()
                val file = java.io.File(dir, "metadata_cache.json")
                val map = trackMetadataCache.mapValues {
                    listOf(
                        it.value.first,
                        it.value.second.toString()
                    )
                }
                file.writeText(Json.encodeToString(map))
            } catch (_: Exception) {
            }
        }
    }

    internal fun checkYouTubeAuth() {
        scope.launch {
            val status = youTubeAuthService.getStatus()
            appRunner()?.runOnRenderThread {
                state = state.copy(youtubeAccountName = status.accountName)
            }
            if (status.accountName != null && state.collections.remotePlaylists.isEmpty()) {
                syncYouTubeLibrary()
            }
        }
    }

    internal fun importYouTubeAuth() {
        if (settingsViewState.isImportingAuth) return
        settingsViewState = settingsViewState.copy(
            isImportingAuth = true,
            authStatusMessage = "Importing from browser..."
        )
        scope.launch {
            val result = youTubeAuthService.importFromBrowser()
            appRunner()?.runOnRenderThread {
                result.fold(
                    onSuccess = { name ->
                        state = state.copy(youtubeAccountName = name)
                        settingsViewState = settingsViewState.copy(
                            isImportingAuth = false,
                            authStatusMessage = "✓ $name"
                        )
                        loadHomeFeed()
                        syncYouTubeLibrary()
                    },
                    onFailure = { _ ->
                        settingsViewState = settingsViewState.copy(
                            isImportingAuth = false,
                            authStatusMessage = "No browser session found"
                        )
                    }
                )
            }
        }
    }

    internal fun logoutYouTubeAuth() {
        scope.launch {
            youTubeAuthService.logout()
            appRunner()?.runOnRenderThread {
                state = state.copy(
                    youtubeAccountName = null,
                    collections = state.collections.copy(remotePlaylists = emptyList())
                )
                settingsViewState = settingsViewState.copy(
                    authStatusMessage = "Logged out"
                )
                loadHomeFeed()
            }
        }
    }
}