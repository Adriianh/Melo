package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.component.DirectoryPickerOverlay
import com.github.adriianh.cli.tui.component.PlaylistInputOverlay
import com.github.adriianh.cli.tui.component.PlaylistPickerOverlay
import com.github.adriianh.cli.tui.component.QueueOverlay
import com.github.adriianh.cli.tui.component.SettingsOverlay
import com.github.adriianh.cli.tui.component.SettingsViewState
import com.github.adriianh.cli.tui.component.TrackOptionsOverlay
import com.github.adriianh.cli.tui.component.screen.deleteDownloadedTrackAction
import com.github.adriianh.cli.tui.component.screen.downloadTrackAction
import com.github.adriianh.cli.tui.component.screen.handleAudioError
import com.github.adriianh.cli.tui.component.screen.handleAudioFinish
import com.github.adriianh.cli.tui.component.screen.handleAudioProgress
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionNext
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionPlayPause
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionPrevious
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionStop
import com.github.adriianh.cli.tui.component.screen.loadLocalTracksAction
import com.github.adriianh.cli.tui.component.screen.onStartLifecycle
import com.github.adriianh.cli.tui.component.screen.onStopLifecycle
import com.github.adriianh.cli.tui.component.screen.renderRoot
import com.github.adriianh.cli.tui.handler.playback.handleQueueKey
import com.github.adriianh.cli.tui.handler.playback.handleTrackOptionsKey
import com.github.adriianh.cli.tui.handler.settings.handleSettingsKey
import com.github.adriianh.cli.tui.player.AudioPlayer
import com.github.adriianh.cli.tui.player.MediaSessionManager
import com.github.adriianh.cli.tui.service.DiscordRpcManager
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.core.domain.interactor.LibraryInteractors
import com.github.adriianh.core.domain.interactor.OfflineInteractors
import com.github.adriianh.core.domain.interactor.PlaybackInteractors
import com.github.adriianh.core.domain.interactor.SearchInteractors
import com.github.adriianh.core.domain.interactor.SessionInteractors
import com.github.adriianh.core.domain.interactor.SettingsInteractors
import com.github.adriianh.core.domain.interactor.StatsInteractors
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.provider.MetadataProvider
import com.github.adriianh.core.domain.repository.OfflineRepository
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Semaphore

class MeloScreen(
    // Shared infrastructure
    internal val httpClient: HttpClient,
    internal val pipedApiClient: PipedApiClient,
    // Interactors
    internal val searchInteractors: SearchInteractors,
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
    dispatcher: CoroutineDispatcher
) : ToolkitApp() {

    // Bridging properties to keep existing code working during refactor
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
    internal var playRecorded = false
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
        onPlayPause = ::handleMediaSessionPlayPause,
        onNext = ::handleMediaSessionNext,
        onPrevious = ::handleMediaSessionPrevious,
        onStop = ::handleMediaSessionStop,
    )

    internal var resolveStreamJob: Job? = null

    internal val audioPlayer: AudioPlayer = AudioPlayer(
        scope = scope,
        onProgress = ::handleAudioProgress,
        onFinish = ::handleAudioFinish,
        onError = ::handleAudioError,
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

    internal val playlistInputOverlay = PlaylistInputOverlay { state }
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

    override fun configure(): TuiConfig = TuiConfig.builder().mouseCapture(true).build()

    override fun onStart() = onStartLifecycle()

    override fun onStop() = onStopLifecycle()

    override fun render(): Element = renderRoot()

    internal fun loadLocalTracks() = loadLocalTracksAction()

    internal fun deleteDownloadedTrack(trackId: String) = deleteDownloadedTrackAction(trackId)

    internal fun downloadTrack(track: Track, downloadType: DownloadType = DownloadType.PREFETCH) =
        downloadTrackAction(track, downloadType)
}