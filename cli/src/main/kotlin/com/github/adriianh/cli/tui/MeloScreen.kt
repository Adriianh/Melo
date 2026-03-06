package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.component.buildPlayerBar
import com.github.adriianh.cli.tui.component.buildSearchBar
import com.github.adriianh.cli.tui.component.buildSidebar
import com.github.adriianh.cli.tui.component.PlaylistInputOverlay
import com.github.adriianh.cli.tui.component.PlaylistPickerOverlay
import com.github.adriianh.cli.tui.component.QueueOverlay
import com.github.adriianh.cli.tui.graphics.ClearGraphicsElement
import com.github.adriianh.cli.tui.handler.*
import com.github.adriianh.cli.tui.player.AudioPlayer
import com.github.adriianh.cli.tui.player.MediaSessionManager
import com.github.adriianh.cli.tui.screen.renderHomeScreen
import com.github.adriianh.cli.tui.screen.renderLibraryScreen
import com.github.adriianh.cli.tui.screen.renderNowPlayingScreen
import com.github.adriianh.cli.tui.screen.renderSearchScreen
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.cli.tui.util.TextAnimationUtil.marqueeText
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.usecase.*
import dev.tamboui.layout.Constraint
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.app.ToolkitApp
import dev.tamboui.toolkit.app.ToolkitRunner
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.tui.TuiConfig
import dev.tamboui.widgets.input.TextInputState
import kotlinx.coroutines.*
import java.time.Duration

class MeloScreen(
    // Search
    internal val searchTracks: SearchTracksUseCase,
    internal val loadMoreTracks: LoadMoreTracksUseCase,
    internal val getTrack: GetTrackUseCase,
    internal val getLyrics: GetLyricsUseCase,
    internal val getSyncedLyrics: GetSyncedLyricsUseCase,
    internal val getSimilarTracks: GetSimilarTracksUseCase,
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
    // Artwork
    internal val artworkRenderer: ArtworkRenderer
) : ToolkitApp() {

    internal var state = MeloState()
    internal val scope = CoroutineScope(Dispatchers.IO)
    internal var detailsJob: Job? = null
    internal var loadMoreJob: Job? = null
    internal var playlistTracksJob: Job? = null
    internal var lastQuery = ""
    internal var marqueeJob: ToolkitRunner.ScheduledAction? = null
    internal var marqueeTick = 0

    /** Exposes the protected runner() for internal extension functions. */
    internal fun appRunner() = runner()

    internal val mediaSession = MediaSessionManager(
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
                state = state.copy(isPlaying = false, progress = 0.0)
            }
        },
    )

    internal val audioPlayer: AudioPlayer = AudioPlayer(
        scope = scope,
        onProgress = { elapsedMs ->
            runner()?.runOnRenderThread {
                val duration = state.nowPlaying?.durationMs ?: 0L
                val progress = if (duration > 0) (elapsedMs.toDouble() / duration).coerceIn(0.0, 1.0) else 0.0
                state = state.copy(progress = progress, nowPlayingPositionMs = elapsedMs)
                mediaSession.updatePosition(elapsedMs)
            }
        },
        onFinish = {
            runner()?.runOnRenderThread {
                state = state.copy(isPlaying = false, isLoadingAudio = false, progress = 0.0)
                seekForward()
            }
        },
        onError = { err ->
            runner()?.runOnRenderThread {
                state = state.copy(isPlaying = false, isLoadingAudio = false, audioError = err.message)
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

    internal val sidebarList: ListElement<*> = list()
        .items(
            "${MeloTheme.ICON_HOME} Home",
            "${MeloTheme.ICON_SEARCH} Search",
            "${MeloTheme.ICON_LIBRARY} Your Library",
            "${MeloTheme.ICON_NOTE} Now Playing",
        )
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .selected(SidebarSection.HOME.ordinal)

    internal val lyricsArea = markupTextArea()
        .scrollbar()
        .wrapWord()
        .focusable()
        .id("lyrics-area")

    internal val similarArea: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_BULLET} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
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
                runner()?.runOnRenderThread { state = state.copy(favorites = tracks) }
            }
        }
        scope.launch {
            getRecentTracks(20).collect { entries ->
                runner()?.runOnRenderThread { state = state.copy(recentTracks = entries) }
            }
        }
        scope.launch {
            getPlaylists().collect { playlists ->
                runner()?.runOnRenderThread { state = state.copy(playlists = playlists) }
            }
        }
        marqueeJob = runner()?.scheduleRepeating({
            runner()?.runOnRenderThread {
                marqueeTick++
                if (marqueeTick > 10) {
                    val track = state.selectedTrack
                    val newOffset = state.marqueeOffset + 1
                    if (track != null) {
                        val separator = "   •   "
                        val full = track.title + separator
                        if (newOffset % full.length == 0) marqueeTick = 0
                    }
                    state = state.copy(marqueeOffset = newOffset)
                }
            }
        }, Duration.ofMillis(150))
    }

    override fun onStop() {
        marqueeJob?.cancel()
        playlistTracksJob?.cancel()
        audioPlayer.stop()
        mediaSession.destroy()
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
            .left(buildSidebar(sidebarList, ::handleSidebarKey), Constraint.length(22))
            .center(renderMainContent())

        val withQueue = if (state.isQueueVisible) stack(mainLayout, queueOverlay) else mainLayout

        return when (state.playlistInputMode) {
            PlaylistInputMode.CREATE,
            PlaylistInputMode.RENAME -> stack(withQueue, playlistInputOverlay)
            PlaylistInputMode.PICKER -> stack(withQueue, playlistPickerOverlay)
            PlaylistInputMode.NONE   -> withQueue
        }
    }

    private fun renderMainContent(): Element {
        if (state.needsGraphicsClear) {
            val pending = state.pendingSection
            val targetSection = pending ?: state.activeSection
            state = state.copy(
                needsGraphicsClear = false,
                activeSection = targetSection,
                pendingSection = null,
                artworkData = if (targetSection != SidebarSection.SEARCH) null else state.artworkData,
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
            }
            return stack(ClearGraphicsElement().fill(), targetContent)
        }

        return when (state.activeSection) {
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
        }
    }
}