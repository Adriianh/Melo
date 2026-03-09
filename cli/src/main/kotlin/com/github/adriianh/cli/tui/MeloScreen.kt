package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.component.*
import com.github.adriianh.cli.tui.graphics.ClearGraphicsElement
import com.github.adriianh.cli.tui.handler.*
import com.github.adriianh.cli.tui.player.AudioPlayer
import com.github.adriianh.cli.tui.player.MediaSessionManager
import com.github.adriianh.cli.tui.screen.*
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.cli.tui.util.TextAnimationUtil.marqueeText
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.provider.ArtworkProvider
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
    // Artwork
    internal val artworkRenderer: ArtworkRenderer,
    internal val artworkProvider: ArtworkProvider,
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
                state = state.copy(player = state.player.copy(isPlaying = false, isLoadingAudio = false, progress = 0.0))
                seekForward()
            }
        },
        onError = { err ->
            runner()?.runOnRenderThread {
                state = state.copy(player = state.player.copy(isPlaying = false, isLoadingAudio = false, audioError = err.message))
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

    private val playlistInputOverlay = PlaylistInputOverlay { state }
    private val playlistPickerOverlay = PlaylistPickerOverlay { state }
    private val queueOverlay = QueueOverlay({ state }, queueList, ::handleQueueKey)

    override fun configure(): TuiConfig = TuiConfig.builder().mouseCapture(true).build()

    override fun onStart() {
        mediaSession.init()
        scope.launch {
            getFavorites().collect { tracks ->
                runner()?.runOnRenderThread { state = state.copy(collections = state.collections.copy(favorites = tracks)) }
            }
        }
        scope.launch {
            getRecentTracks(20).collect { entries ->
                runner()?.runOnRenderThread { state = state.copy(collections = state.collections.copy(recentTracks = entries)) }
            }
        }
        scope.launch {
            getPlaylists().collect { playlists ->
                runner()?.runOnRenderThread { state = state.copy(collections = state.collections.copy(playlists = playlists)) }
            }
        }
        scope.launch { restoreLastSession() }
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
            .left(buildSidebar(sidebarNavList, sidebarUtilList, state.navigation.sidebarInUtil, ::handleSidebarKey), Constraint.length(22))
            .center(renderMainContent())

        val withQueue = if (state.player.isQueueVisible) stack(mainLayout, queueOverlay) else mainLayout

        return when (state.playlistInteraction.playlistInputMode) {
            PlaylistInputMode.CREATE,
            PlaylistInputMode.RENAME -> stack(withQueue, playlistInputOverlay)
            PlaylistInputMode.PICKER -> stack(withQueue, playlistPickerOverlay)
            PlaylistInputMode.NONE   -> withQueue
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
                // Should not happen with exhaustive SidebarSection
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
                SidebarSection.HOME    -> renderHomeScreen(
                    state, homeRecentList, homeFavoritesList,
                    onKeyEvent = ::handleHomeKey,
                )
                SidebarSection.SEARCH  -> renderSearchScreen(
                    state, resultList, lyricsArea, similarArea,
                    ::marqueeText, ::handleResultsKey, ::handleDetailKey,
                )
                SidebarSection.LIBRARY -> renderLibraryScreen(
                    state, favoritesList, playlistsList, playlistTracksList, ::handleLibraryKey,
                )
                SidebarSection.NOW_PLAYING -> renderNowPlayingScreen(state, ::marqueeText, ::handlePlayerBarKey)
                SidebarSection.STATS -> renderStatsScreen(state, ::handleStatsKey)
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
                state, favoritesList, playlistsList, playlistTracksList, ::handleLibraryKey,
            )
            is ScreenState.NowPlaying -> renderNowPlayingScreen(state, ::marqueeText, ::handlePlayerBarKey)
            is ScreenState.Stats -> renderStatsScreen(state, ::handleStatsKey)
        }
    }
}