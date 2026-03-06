package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.component.*
import com.github.adriianh.cli.tui.handler.*
import com.github.adriianh.cli.tui.player.AudioPlayer
import com.github.adriianh.cli.tui.player.MediaSessionManager
import com.github.adriianh.cli.tui.screen.*
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.usecase.*
import dev.tamboui.toolkit.Constraint
import dev.tamboui.toolkit.Element
import dev.tamboui.toolkit.component.list.TuiList
import dev.tamboui.toolkit.component.list.TuiListState
import dev.tamboui.toolkit.component.text.TuiTextArea
import dev.tamboui.toolkit.component.text.TuiTextAreaState
import dev.tamboui.toolkit.input.overlay.TuiInputOverlay
import dev.tamboui.toolkit.input.overlay.TuiInputOverlayState
import dev.tamboui.tui.TuiConfig
import dev.tamboui.tui.TuiScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
) : TuiScreen() {

    internal var state: MeloState = MeloState()
        set(value) {
            field = value
            invalidate()
        }

    internal val scope = CoroutineScope(Dispatchers.IO)

    internal var detailsJob   = kotlinx.coroutines.Job()
    internal var loadMoreJob  = kotlinx.coroutines.Job()
    internal var playlistTracksJob: kotlinx.coroutines.Job? = null
    internal var lastQuery    = ""
    internal var marqueeJob: Any? = null
    internal var marqueeTick  = 0

    /** Exposes the protected runner() for internal extension functions. */
    internal fun appRunner() = runner()

    internal val mediaSession = MediaSessionManager()
    internal val audioPlayer  = AudioPlayer(
        onProgress = { positionMs ->
            runner()?.runOnRenderThread {
                state = state.copy(nowPlayingPositionMs = positionMs)
            }
        },
        onFinish   = {
            runner()?.runOnRenderThread { handleTrackFinished() }
        },
        onError    = { _ ->
            runner()?.runOnRenderThread {
                state = state.copy(isLoadingAudio = false, audioError = "Playback error")
            }
        },
        isPlaying  = { state.isPlaying },
    )

    internal val searchInputState = TuiTextAreaState()

    internal val homeRecentList   = TuiListState()
    internal val homeFavoritesList = TuiListState()
    internal val resultList       = TuiListState()
    internal val favoritesList    = TuiListState()
    internal val playlistsList    = TuiListState()
    internal val playlistTracksList = TuiListState()
    internal val sidebarList      = TuiListState()
    internal val lyricsArea       = TuiTextAreaState()
    internal val similarArea      = TuiTextAreaState()
    internal val queueList        = TuiListState()

    internal val playlistInputOverlay  = TuiInputOverlayState()
    internal val playlistPickerOverlay = TuiInputOverlayState()

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
        // Slowed from 150ms to 300ms: halves MeloState.copy() frequency,
        // significantly reducing short-lived object allocation and GC pressure.
        }, Duration.ofMillis(300))
    }

    override fun onStop() {
        marqueeJob?.cancel()
        playlistTracksJob?.cancel()
        audioPlayer.stop()
        mediaSession.destroy()
        scope.cancel()
    }

    override fun render(): Element {
        val playerBarBuilder = {
            buildPlayerBar(
                state, ::formatDuration, ::handlePlayerBarKey,
                ::togglePlayPause, ::adjustVolume, ::seekForward, ::seekBackward,
                ::toggleShuffle, ::cycleRepeat, ::toggleQueue,
            )
        }
        val bottomSection = if (state.isQueueVisible) {
            dock()
                .bottom(playerBarBuilder(), Constraint.length(4))
                .center(stack(ClearGraphicsElement(), buildQueuePanel(state, queueList, ::handleQueueKey)))
        } else {
            playerBarBuilder()
        }
        val bottomConstraint = if (state.isQueueVisible) Constraint.length(15) else Constraint.length(4)

        val mainLayout = dock()
            .top(buildSearchBar(searchInputState, ::performSearch, ::handleSearchBarKey), Constraint.length(3))
            .bottom(bottomSection, bottomConstraint)
            .left(buildSidebar(sidebarList, ::handleSidebarKey), Constraint.length(22))
            .center(renderMainContent())

        return when (state.playlistInputMode) {
            PlaylistInputMode.CREATE,
            PlaylistInputMode.RENAME -> stack(mainLayout, playlistInputOverlay)
            PlaylistInputMode.PICKER -> stack(mainLayout, playlistPickerOverlay)
            PlaylistInputMode.NONE   -> mainLayout
        }
    }
}
