package com.github.adriianh.cli.tui

import com.github.adriianh.cli.service.YouTubeAuthService
import com.github.adriianh.cli.tui.component.SettingsViewState
import com.github.adriianh.cli.tui.component.screen.deleteDownloadedTrackAction
import com.github.adriianh.cli.tui.component.screen.downloadTrackAction
import com.github.adriianh.cli.tui.component.screen.loadLocalTracksAction
import com.github.adriianh.cli.tui.component.screen.onStartLifecycle
import com.github.adriianh.cli.tui.component.screen.onStopLifecycle
import com.github.adriianh.cli.tui.component.screen.renderRoot
import com.github.adriianh.cli.tui.player.AudioPlayer
import com.github.adriianh.cli.tui.service.DiscordRpcManager
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.cli.tui.util.ToastKind
import com.github.adriianh.cli.tui.util.pushToast
import com.github.adriianh.core.domain.interactor.DiscoveryInteractors
import com.github.adriianh.core.domain.interactor.LibraryInteractors
import com.github.adriianh.core.domain.interactor.OfflineInteractors
import com.github.adriianh.core.domain.interactor.PlaybackInteractors
import com.github.adriianh.core.domain.interactor.SearchInteractors
import com.github.adriianh.core.domain.interactor.SessionInteractors
import com.github.adriianh.core.domain.interactor.SettingsInteractors
import com.github.adriianh.core.domain.interactor.StatsInteractors
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.provider.MetadataProvider
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.data.remote.piped.PipedApiClient
import dev.tamboui.toolkit.app.ToolkitApp
import dev.tamboui.toolkit.app.ToolkitRunner
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.tui.TuiConfig
import dev.tamboui.widgets.input.TextInputState
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Semaphore

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
    internal val translateLyrics get() = searchInteractors.translateLyrics
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
    internal val getRemoteHistory get() = libraryInteractors.getRemoteHistory
    internal val reorderPlaylistTracks get() = libraryInteractors.reorderPlaylistTracks

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
    internal var toastJob: ToolkitRunner.ScheduledAction? = null
    internal var equalizerJob: ToolkitRunner.ScheduledAction? = null
    /** Last known download status per track id, used to detect completion transitions. */
    internal val lastDownloadStatusById = mutableMapOf<String, DownloadStatus>()
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

    internal val mediaSession = buildMediaSession()
    internal val audioPlayer: AudioPlayer = buildAudioPlayer()
    internal val playbackManager = buildPlaybackManager(dispatcher)

    internal val searchInputState = TextInputState()

    @Volatile
    internal var lastObservedSearchQuery = ""

    init {
        observeSearchInput()
    }

    internal val homeRecentList: ListElement<*> = buildHomeRecentList()
    internal val homeFavoritesList: ListElement<*> = buildHomeFavoritesList()
    internal val resultList: ListElement<*> = buildResultList()
    internal val favoritesList: ListElement<*> = buildFavoritesList()
    internal val playlistsList: ListElement<*> = buildPlaylistsList()
    internal val playlistTracksList: ListElement<*> = buildPlaylistTracksList()
    internal val entityTracksList: ListElement<*> = buildEntityTracksList()
    internal val artistDashboardList: ListElement<*> = buildArtistDashboardList()
    internal val localLibraryList: ListElement<*> = buildLocalLibraryList()
    internal val homeFeedSectionList: ListElement<*> = buildHomeFeedSectionList()
    internal val homeFeedItemList: ListElement<*> = buildHomeFeedItemList()
    internal val sidebarNavList: ListElement<*> = buildSidebarNavList()
    internal val sidebarUtilList: ListElement<*> = buildSidebarUtilList()
    internal val lyricsArea = buildLyricsArea()
    internal val entityDescriptionArea = buildEntityDescriptionArea()
    internal val similarArea: ListElement<*> = buildSimilarArea()
    internal val queueList: ListElement<*> = buildQueueList()
    internal val offlineList: ListElement<*> = buildOfflineList()
    internal val settingsSectionList: ListElement<*> = buildSettingsSectionList()
    internal val settingsList: ListElement<*> = buildSettingsList()

    internal val playlistInputOverlay = buildPlaylistInputOverlay()
    internal val searchSuggestionsOverlay = buildSearchSuggestionsOverlay()
    internal val playlistPickerOverlay = buildPlaylistPickerOverlay()
    internal val queueOverlay = buildQueueOverlay()
    internal val settingsOverlay = buildSettingsOverlay()
    internal val directoryPickerOverlay = buildDirectoryPickerOverlay()
    internal val trackOptionsOverlay = buildTrackOptionsOverlay()
    internal val commandBarSuggestionsOverlay = buildCommandBarSuggestionsOverlay()
    internal val languagePickerOverlay = buildLanguagePickerOverlay()
    internal val toastOverlay = buildToastOverlay()

    override fun configure(): TuiConfig = TuiConfig.builder().mouseCapture(true).build()

    override fun onStart() = onStartLifecycle()

    override fun onStop() = onStopLifecycle()

    override fun render(): Element = renderRoot()

    internal fun loadLocalTracks() = loadLocalTracksAction()

    internal fun deleteDownloadedTrack(trackId: String) = deleteDownloadedTrackAction(trackId)

    internal fun downloadTrack(track: Track, downloadType: DownloadType = DownloadType.PREFETCH) =
        downloadTrackAction(track, downloadType)

    private var toastIdCounter = 0L

    /**
     * Enqueues a transient confirmation toast. Safe to call from key handlers
     * (render thread); coroutine call sites must wrap it in `runOnRenderThread`
     * like the rest of their state updates.
     */
    internal fun showToast(message: String, kind: ToastKind = ToastKind.INFO) {
        state = state.copy(
            toasts = pushToast(
                toasts = state.toasts,
                message = message,
                kind = kind,
                nowMs = System.currentTimeMillis(),
                id = ++toastIdCounter,
            )
        )
    }
}