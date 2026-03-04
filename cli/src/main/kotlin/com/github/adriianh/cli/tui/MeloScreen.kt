package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.component.buildPlayerBar
import com.github.adriianh.cli.tui.component.buildSearchBar
import com.github.adriianh.cli.tui.component.buildSidebar
import com.github.adriianh.cli.tui.screen.renderHomeScreen
import com.github.adriianh.cli.tui.screen.renderLibraryScreen
import com.github.adriianh.cli.tui.screen.renderSearchScreen
import com.github.adriianh.cli.tui.player.AudioPlayer
import com.github.adriianh.cli.tui.util.TextAnimationUtil.marqueeText
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.SimilarTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.usecase.*
import dev.tamboui.layout.Constraint
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.app.ToolkitApp
import dev.tamboui.toolkit.app.ToolkitRunner
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.TuiConfig
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.input.TextInputState
import kotlinx.coroutines.*
import java.time.Duration

class MeloScreen(
    private val searchTracks: SearchTracksUseCase,
    private val loadMoreTracks: LoadMoreTracksUseCase,
    private val getTrack: GetTrackUseCase,
    private val getLyrics: GetLyricsUseCase,
    private val getSimilarTracks: GetSimilarTracksUseCase,
    private val getFavorites: GetFavoritesUseCase,
    private val addFavorite: AddFavoriteUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val getRecentTracks: GetRecentTracksUseCase,
    private val recordPlay: RecordPlayUseCase,
    private val getStream: GetStreamUseCase,
) : ToolkitApp() {

    private var state = MeloState()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var detailsJob: Job? = null
    private var loadMoreJob: Job? = null
    private var lastQuery = ""
    private var marqueeJob: ToolkitRunner.ScheduledAction? = null
    private var marqueeTick = 0

    private val audioPlayer = AudioPlayer(
        scope = scope,
        onProgress = { elapsedMs ->
            runner()?.runOnRenderThread {
                val duration = state.nowPlaying?.durationMs ?: 0L
                val progress = if (duration > 0) (elapsedMs.toDouble() / duration).coerceIn(0.0, 1.0) else 0.0
                state = state.copy(progress = progress)
            }
        },
        onFinish = {
            runner()?.runOnRenderThread {
                state = state.copy(isPlaying = false, progress = 0.0)
            }
        },
        onError = { err ->
            runner()?.runOnRenderThread {
                state = state.copy(isPlaying = false, isLoadingAudio = false, audioError = err.message)
            }
        },
    )

    // ── Widget instances ──

    private val searchInputState = TextInputState()

    private val resultList: ListElement<*> = list()
        .highlightSymbol("▸ ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()

    private val favoritesList: ListElement<*> = list()
        .highlightSymbol("▸ ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("library-list")

    private val sidebarList: ListElement<*> = list()
        .items("🏠 Home", "🔍 Search", "📚 Your Library")
        .highlightSymbol("▸ ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .selected(SidebarSection.SEARCH.ordinal)

    private val lyricsArea = markupTextArea()
        .scrollbar()
        .wrapWord()
        .focusable()
        .id("lyrics-area")

    private val similarArea: ListElement<*> = list()
        .highlightSymbol("• ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .scrollbar()
        .focusable()
        .id("similar-area")

    // ─────────────────────────────── Lifecycle ────────────────────────────────

    override fun configure(): TuiConfig = TuiConfig.builder().mouseCapture(true).build()

    override fun onStart() {
        scope.launch {
            getFavorites().collect { tracks ->
                runner()?.runOnRenderThread {
                    state = state.copy(favorites = tracks)
                }
            }
        }
        scope.launch {
            getRecentTracks(20).collect { entries ->
                runner()?.runOnRenderThread {
                    state = state.copy(recentTracks = entries)
                }
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
        audioPlayer.stop()
        scope.cancel()
    }

    // ─────────────────────────────── Render ───────────────────────────────────

    override fun render(): Element = dock()
        .top(buildSearchBar(searchInputState, ::performSearch, ::handleSearchBarKey), Constraint.length(3))
        .bottom(buildPlayerBar(state, ::formatDuration, ::handlePlayerBarKey), Constraint.length(3))
        .left(buildSidebar(sidebarList, ::handleSidebarKey), Constraint.length(22))
        .center(renderMainContent())

    private fun renderMainContent(): Element = when (state.activeSection) {
        SidebarSection.HOME    -> renderHomeScreen(state, onSelectTrack = ::playTrack, onKeyEvent = ::handleHomeKey)
        SidebarSection.SEARCH  -> renderSearchScreen(
            state, resultList, lyricsArea, similarArea,
            ::marqueeText, ::handleResultsKey, ::handleDetailKey
        )
        SidebarSection.LIBRARY -> renderLibraryScreen(state, favoritesList, ::handleLibraryKey)
    }

    // ─────────────────────────────── Event Handlers ───────────────────────────

    private fun handleSearchBarKey(event: KeyEvent): EventResult {
        if (state.results.isNotEmpty()) {
            if (event.matches(Actions.MOVE_DOWN) || event.matches(Actions.MOVE_UP)) {
                return handleResultsKey(event)
            }
        }
        return EventResult.UNHANDLED
    }

    private fun handleHomeKey(event: KeyEvent): EventResult {
        val isFocused = runner()?.focusManager()?.focusedId() == "home-panel"
        if (!isFocused) return EventResult.UNHANDLED
        if (event.matches(Actions.SELECT)) return applySidebarSelection()
        return EventResult.UNHANDLED
    }

    private fun handleSidebarKey(event: KeyEvent): EventResult {
        val isFocused = runner()?.focusManager()?.focusedId() == "sidebar-panel"
        if (!isFocused) return EventResult.UNHANDLED
        when {
            event.matches(Actions.MOVE_UP) -> {
                sidebarList.selected(maxOf(0, sidebarList.selected() - 1))
                return EventResult.HANDLED
            }
            event.matches(Actions.MOVE_DOWN) -> {
                sidebarList.selected(minOf(SidebarSection.entries.lastIndex, sidebarList.selected() + 1))
                return EventResult.HANDLED
            }
            event.matches(Actions.SELECT) -> {
                applySidebarSelection()
                return EventResult.HANDLED
            }
        }
        return EventResult.UNHANDLED
    }

    private fun applySidebarSelection(): EventResult {
        val section = SidebarSection.entries.getOrNull(sidebarList.selected())
        if (section != null) state = state.copy(activeSection = section)
        return EventResult.HANDLED
    }

    private fun handleResultsKey(event: KeyEvent): EventResult {
        if (state.results.isEmpty()) return EventResult.UNHANDLED
        val focused = runner()?.focusManager()?.focusedId()
        val isFocused = focused == "results-panel"
        when {
            event.matches(Actions.SELECT) -> {
                if (!isFocused) return EventResult.UNHANDLED
                val selected = state.results.getOrNull(resultList.selected()) ?: return EventResult.UNHANDLED
                playTrack(selected)
                return EventResult.HANDLED
            }
            event.matches(Actions.MOVE_DOWN) -> {
                if (!isFocused) return EventResult.UNHANDLED
                val newIndex = minOf(state.results.lastIndex, state.selectedIndex + 1)
                resultList.selected(newIndex)
                state.results.getOrNull(newIndex)?.let { track ->
                    state = state.copy(selectedIndex = newIndex, selectedTrack = track, marqueeOffset = 0)
                    marqueeTick = 0
                    debouncedLoadDetails(track)
                }
                if (newIndex >= state.results.size - 5 && !state.isLoadingMore && state.hasMore) loadMore()
                return EventResult.HANDLED
            }
            event.matches(Actions.MOVE_UP) -> {
                if (!isFocused) return EventResult.UNHANDLED
                val newIndex = maxOf(0, state.selectedIndex - 1)
                resultList.selected(newIndex)
                state.results.getOrNull(newIndex)?.let { track ->
                    state = state.copy(selectedIndex = newIndex, selectedTrack = track, marqueeOffset = 0)
                    marqueeTick = 0
                    debouncedLoadDetails(track)
                }
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == 'f' -> {
                state.results.getOrNull(state.selectedIndex)?.let { toggleFavorite(it) }
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == 'l' -> {
                loadLyrics()
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == ' ' -> {
                togglePlayPause()
                return EventResult.HANDLED
            }
        }
        return EventResult.UNHANDLED
    }

    private fun handleDetailKey(event: KeyEvent): EventResult {
        when {
            event.code() == KeyCode.CHAR && event.character() == '1' -> {
                state = state.copy(detailTab = DetailTab.INFO)
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == '2' -> {
                state = state.copy(detailTab = DetailTab.LYRICS)
                if (state.lyrics == null && !state.isLoadingLyrics) loadLyrics()
                runner()?.focusManager()?.setFocus("lyrics-area")
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == '3' -> {
                state = state.copy(detailTab = DetailTab.SIMILAR)
                runner()?.focusManager()?.setFocus("similar-area")
                return EventResult.HANDLED
            }
            event.matches(Actions.SELECT) && state.detailTab == DetailTab.LYRICS -> {
                if (state.lyrics == null) loadLyrics()
                return EventResult.HANDLED
            }
        }
        return EventResult.UNHANDLED
    }

    private fun handleLibraryKey(event: KeyEvent): EventResult {
        if (state.favorites.isEmpty()) return EventResult.UNHANDLED
        val isFocused = runner()?.focusManager()?.focusedId() == "library-panel"
        when {
            event.matches(Actions.SELECT) -> {
                if (!isFocused) return EventResult.UNHANDLED
                state.favorites.getOrNull(favoritesList.selected())?.let { playTrack(it) }
                return EventResult.HANDLED
            }
            event.matches(Actions.MOVE_DOWN) -> {
                if (!isFocused) return EventResult.UNHANDLED
                favoritesList.selected(minOf(state.favorites.lastIndex, favoritesList.selected() + 1))
                return EventResult.HANDLED
            }
            event.matches(Actions.MOVE_UP) -> {
                if (!isFocused) return EventResult.UNHANDLED
                favoritesList.selected(maxOf(0, favoritesList.selected() - 1))
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == 'f' -> {
                if (!isFocused) return EventResult.UNHANDLED
                state.favorites.getOrNull(favoritesList.selected())?.let { removeFavoriteTrack(it) }
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == ' ' -> {
                togglePlayPause()
                return EventResult.HANDLED
            }
        }
        return EventResult.UNHANDLED
    }

    /**
     * Global player controls — handled regardless of which panel has focus.
     * Space  → play / pause
     * +      → volume up 5
     * -      → volume down 5
     */
    private fun handlePlayerBarKey(event: KeyEvent): EventResult {
        when (KeyCode.CHAR) {
            event.code() if event.character() == ' ' -> {
                togglePlayPause()
                return EventResult.HANDLED
            }
            event.code() if event.character() == '+' -> {
                adjustVolume(5)
                return EventResult.HANDLED
            }
            event.code() if event.character() == '-' -> {
                adjustVolume(-5)
                return EventResult.HANDLED
            }
            else -> {}
        }
        return EventResult.UNHANDLED
    }

    // ─────────────────────────────── Actions ──────────────────────────────────

    private fun playTrack(track: Track) {
        state = state.copy(
            selectedTrack = track,
            nowPlaying = track,
            isPlaying = false,
            isLoadingAudio = true,
            audioError = null,
            progress = 0.0,
            marqueeOffset = 0,
        )
        marqueeTick = 0
        audioPlayer.stop()
        loadTrackDetails(track.id, track)
        scope.launch {
            recordPlay(track)
            val url = getStream(track)
            runner()?.runOnRenderThread {
                if (url == null) {
                    state = state.copy(isLoadingAudio = false, audioError = "Stream not available")
                    return@runOnRenderThread
                }
                state = state.copy(isPlaying = true, isLoadingAudio = false)
                audioPlayer.play(url)
            }
        }
        checkIsFavorite(track.id)
    }

    private fun togglePlayPause() {
        if (state.nowPlaying == null || state.isLoadingAudio) return
        if (state.isPlaying) {
            audioPlayer.pause()
            state = state.copy(isPlaying = false)
        } else {
            audioPlayer.resume()
            state = state.copy(isPlaying = true)
        }
    }

    private fun adjustVolume(delta: Int) {
        val newVol = (state.volume + delta).coerceIn(0, 100)
        state = state.copy(volume = newVol)
        audioPlayer.setVolume(newVol)
    }

    private fun toggleFavorite(track: Track) {
        scope.launch {
            if (isFavoriteUseCase(track.id)) removeFavorite(track.id)
            else addFavorite(track)
            val isFav = isFavoriteUseCase(track.id)
            runner()?.runOnRenderThread {
                state = state.copy(isFavorite = isFav)
            }
        }
    }

    private fun removeFavoriteTrack(track: Track) {
        scope.launch { removeFavorite(track.id) }
    }

    private fun checkIsFavorite(trackId: String) {
        scope.launch {
            val isFav = isFavoriteUseCase(trackId)
            runner()?.runOnRenderThread {
                state = state.copy(isFavorite = isFav)
            }
        }
    }

    private fun performSearch() {
        val query = searchInputState.text()
        if (query.isBlank()) return
        lastQuery = query
        loadMoreJob?.cancel()
        loadMoreJob = null
        state = state.copy(
            isLoading = true,
            errorMessage = null,
            selectedTrack = null,
            hasMore = true,
            activeSection = SidebarSection.SEARCH,
        )
        sidebarList.selected(SidebarSection.SEARCH.ordinal)

        scope.launch {
            try {
                val results = searchTracks(query)
                val firstTrack = results.firstOrNull()
                runner()?.runOnRenderThread {
                    state = state.copy(
                        results = results,
                        isLoading = false,
                        selectedIndex = 0,
                        selectedTrack = firstTrack,
                        hasMore = loadMoreTracks.hasMore(results.size),
                    )
                    resultList.selected(0)
                    focusResults()
                }
                if (firstTrack != null) loadTrackDetails(firstTrack.id)
            } catch (e: Exception) {
                runner()?.runOnRenderThread {
                    state = state.copy(isLoading = false, errorMessage = "Search failed: ${e.message}")
                }
            }
        }
    }

    private fun loadMore() {
        if (lastQuery.isBlank()) return
        loadMoreJob?.cancel()
        val offset = state.results.size
        state = state.copy(isLoadingMore = true)
        loadMoreJob = scope.launch {
            try {
                val more = loadMoreTracks(lastQuery, offset)
                if (isActive) runner()?.runOnRenderThread {
                    state = state.copy(
                        results = state.results + more,
                        isLoadingMore = false,
                        hasMore = loadMoreTracks.hasMore(offset + more.size),
                    )
                }
            } catch (_: Exception) {
                runner()?.runOnRenderThread {
                    state = state.copy(isLoadingMore = false)
                }
            }
        }
    }

    private fun debouncedLoadDetails(track: Track) {
        detailsJob?.cancel()
        detailsJob = scope.launch {
            delay(150)
            if (isActive) loadTrackDetails(track.id, track)
        }
    }

    private fun loadTrackDetails(trackId: String, knownTrack: Track? = null) {
        detailsJob?.cancel()
        state = state.copy(lyrics = null, isLoadingLyrics = false, similarTracks = emptyList(), artworkData = null)
        detailsJob = scope.launch {
            val fullTrackDeferred = async { getTrack(trackId) }
            val similarDeferred = async {
                val artist = knownTrack?.artist ?: fullTrackDeferred.await()?.artist ?: return@async emptyList<SimilarTrack>()
                val title  = knownTrack?.title  ?: fullTrackDeferred.await()?.title  ?: return@async emptyList<SimilarTrack>()
                getSimilarTracks(artist, title)
            }
            val fullTrack = fullTrackDeferred.await() ?: knownTrack ?: return@launch
            val artworkData = fullTrack.artworkUrl?.let { ArtworkRenderer.load(it) }
            if (isActive) {
                runner()?.runOnRenderThread {
                    state = state.copy(selectedTrack = fullTrack, artworkData = artworkData)
                }
                val similar = similarDeferred.await()
                if (isActive) runner()?.runOnRenderThread {
                    state = state.copy(similarTracks = similar)
                }
            }
        }
    }

    private fun loadLyrics() {
        val track = state.selectedTrack ?: return
        state = state.copy(isLoadingLyrics = true, lyrics = null)
        scope.launch {
            val lyrics = getLyrics(track.artist, track.title)
            runner()?.runOnRenderThread {
                state = state.copy(lyrics = lyrics ?: "Lyrics not found", isLoadingLyrics = false)
            }
        }
    }

    private fun focusResults() {
        runner()?.focusManager()?.setFocus("results-panel")
    }
}