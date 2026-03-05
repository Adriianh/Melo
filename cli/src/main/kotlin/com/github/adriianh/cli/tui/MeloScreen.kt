package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.component.buildPlayerBar
import com.github.adriianh.cli.tui.component.buildQueuePanel
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
                seekForward()
            }
        },
        onError = { err ->
            runner()?.runOnRenderThread {
                state = state.copy(isPlaying = false, isLoadingAudio = false, audioError = err.message)
            }
        },
    )

    private val searchInputState = TextInputState()

    private val resultList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()

    private val favoritesList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("library-list")

    private val sidebarList: ListElement<*> = list()
        .items(
            "${MeloTheme.ICON_HOME} Home",
            "${MeloTheme.ICON_SEARCH} Search",
            "${MeloTheme.ICON_LIBRARY} Your Library",
        )
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .selected(SidebarSection.SEARCH.ordinal)

    private val lyricsArea = markupTextArea()
        .scrollbar()
        .wrapWord()
        .focusable()
        .id("lyrics-area")

    private val similarArea: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_BULLET} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .scrollbar()
        .focusable()
        .id("similar-area")

    private val queueList: ListElement<*> = list()
        .highlightSymbol("${MeloTheme.ICON_ARROW} ")
        .highlightColor(MeloTheme.PRIMARY_COLOR)
        .autoScroll()
        .scrollbar()
        .focusable()
        .id("queue-list")

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
                .center(buildQueuePanel(state, queueList, ::handleQueueKey))
        } else {
            playerBarBuilder()
        }
        val bottomConstraint = if (state.isQueueVisible) Constraint.length(15) else Constraint.length(5)

        return dock()
            .top(buildSearchBar(searchInputState, ::performSearch, ::handleSearchBarKey), Constraint.length(3))
            .bottom(bottomSection, bottomConstraint)
            .left(buildSidebar(sidebarList, ::handleSidebarKey), Constraint.length(22))
            .center(renderMainContent())
    }

    private fun renderMainContent(): Element = when (state.activeSection) {
        SidebarSection.HOME    -> renderHomeScreen(state, onSelectTrack = ::playTrack, onKeyEvent = ::handleHomeKey)
        SidebarSection.SEARCH  -> renderSearchScreen(
            state, resultList, lyricsArea, similarArea,
            ::marqueeText, ::handleResultsKey, ::handleDetailKey
        )
        SidebarSection.LIBRARY -> renderLibraryScreen(state, favoritesList, ::handleLibraryKey)
    }

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
            event.code() == KeyCode.ENTER -> {
                if (!isFocused) return EventResult.UNHANDLED
                val selected = state.results.getOrNull(resultList.selected()) ?: return EventResult.UNHANDLED
                playTrack(selected)
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == 'f' -> {
                state.results.getOrNull(state.selectedIndex)?.let { toggleFavorite(it) }
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == 'q' -> {
                state.results.getOrNull(state.selectedIndex)?.let { addToQueue(it) }
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == 'l' -> {
                loadLyrics()
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
            event.code() == KeyCode.ENTER -> {
                if (!isFocused) return EventResult.UNHANDLED
                state.favorites.getOrNull(favoritesList.selected())?.let { playTrack(it) }
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == 'f' -> {
                if (!isFocused) return EventResult.UNHANDLED
                state.favorites.getOrNull(favoritesList.selected())?.let { removeFavoriteTrack(it) }
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == 'q' -> {
                if (!isFocused) return EventResult.UNHANDLED
                state.favorites.getOrNull(favoritesList.selected())?.let { addToQueue(it) }
                return EventResult.HANDLED
            }
        }
        return EventResult.UNHANDLED
    }

    private fun handlePlayerBarKey(event: KeyEvent): EventResult {
        if (event.code() != KeyCode.CHAR) return EventResult.UNHANDLED
        return when (event.character()) {
            ' '  -> { togglePlayPause(); EventResult.HANDLED }
            '+'  -> { adjustVolume(5);   EventResult.HANDLED }
            '-'  -> { adjustVolume(-5);  EventResult.HANDLED }
            's'  -> { toggleShuffle();   EventResult.HANDLED }
            'r'  -> { cycleRepeat();     EventResult.HANDLED }
            'q'  -> { toggleQueue();     EventResult.HANDLED }
            else -> EventResult.UNHANDLED
        }
    }

    private fun handleQueueKey(event: KeyEvent): EventResult {
        val isFocused = runner()?.focusManager()?.focusedId() == "queue-panel"
        when {
            event.matches(Actions.MOVE_DOWN) -> {
                if (!isFocused) return EventResult.UNHANDLED
                state = state.copy(queueCursor = minOf(state.queue.lastIndex, state.queueCursor + 1))
                return EventResult.HANDLED
            }
            event.matches(Actions.MOVE_UP) -> {
                if (!isFocused) return EventResult.UNHANDLED
                state = state.copy(queueCursor = maxOf(0, state.queueCursor - 1))
                return EventResult.HANDLED
            }
            event.code() == KeyCode.ENTER -> {
                if (!isFocused) return EventResult.UNHANDLED
                state.queue.getOrNull(state.queueCursor)?.let { playFromQueue(state.queueCursor) }
                return EventResult.HANDLED
            }
            event.code() == KeyCode.DELETE || (event.code() == KeyCode.CHAR && event.character() == 'd') -> {
                if (!isFocused) return EventResult.UNHANDLED
                removeFromQueue(state.queueCursor)
                return EventResult.HANDLED
            }
            event.code() == KeyCode.CHAR && event.character() == 'c' -> {
                clearQueue()
                return EventResult.HANDLED
            }
        }
        return EventResult.UNHANDLED
    }

    private fun playTrack(track: Track) {
        // Sync queue: if track is in queue use its index, otherwise insert at current position
        val existingIndex = state.queue.indexOfFirst { it.id == track.id }
        val (newQueue, newIndex) = if (existingIndex >= 0) {
            state.queue to existingIndex
        } else {
            val insertAt = (state.queueIndex + 1).coerceAtLeast(0)
            val q = state.queue.toMutableList().also { it.add(insertAt, track) }
            q to insertAt
        }

        state = state.copy(
            selectedTrack = track,
            nowPlaying = track,
            isPlaying = false,
            isLoadingAudio = true,
            audioError = null,
            progress = 0.0,
            marqueeOffset = 0,
            queue = newQueue,
            queueIndex = newIndex,
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

    private fun seekBackward() {
        if (state.isLoadingAudio) return
        // If more than 3s played, restart track; otherwise go to previous in queue
        val threshold = 3000L
        val elapsedMs = (state.progress * (state.nowPlaying?.durationMs ?: 0L)).toLong()
        if (elapsedMs > threshold || state.queueIndex <= 0) {
            state.nowPlaying?.let { playTrack(it) }
        } else {
            playFromQueue(state.queueIndex - 1)
        }
    }

    private fun seekForward() {
        if (state.isLoadingAudio) return
        val queue = state.queue
        if (queue.isEmpty()) return

        val nextIndex = when {
            state.repeatMode == RepeatMode.ONE -> state.queueIndex
            state.shuffleEnabled && queue.size > 1 -> {
                val candidates = queue.indices.filter { it != state.queueIndex }
                candidates.random()
            }
            state.repeatMode == RepeatMode.ALL -> (state.queueIndex + 1) % queue.size
            else -> {
                val next = state.queueIndex + 1
                if (next >= queue.size) {
                    loadSimilarAndPlay()
                    return
                }
                next
            }
        }
        playFromQueue(nextIndex)
    }

    private fun loadSimilarAndPlay() {
        val track = state.nowPlaying ?: return
        state = state.copy(isLoadingAudio = true, isRadioMode = true)
        scope.launch {
            try {
                val similar = getSimilarTracks(track.artist, track.title)
                if (similar.isEmpty()) {
                    runner()?.runOnRenderThread {
                        state = state.copy(isPlaying = false, isLoadingAudio = false, progress = 0.0, isRadioMode = false)
                    }
                    return@launch
                }
                val resolved = similar
                    .shuffled()
                    .take(10)
                    .map { st ->
                        async {
                            runCatching { searchTracks("${st.artist} ${st.title}").firstOrNull() }.getOrNull()
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
                    .distinctBy { it.id }

                if (resolved.isEmpty()) {
                    runner()?.runOnRenderThread {
                        state = state.copy(isPlaying = false, isLoadingAudio = false, progress = 0.0, isRadioMode = false)
                    }
                    return@launch
                }

                runner()?.runOnRenderThread {
                    state = state.copy(
                        queue = resolved,
                        queueIndex = -1,
                        queueCursor = 0,
                        isLoadingAudio = false,
                    )
                    playFromQueue(0)
                }
            } catch (e: Exception) {
                runner()?.runOnRenderThread {
                    state = state.copy(isPlaying = false, isLoadingAudio = false, audioError = e.message, isRadioMode = false)
                }
            }
        }
    }

    private fun playFromQueue(index: Int) {
        val track = state.queue.getOrNull(index) ?: return
        state = state.copy(queueIndex = index)
        playTrack(track)
    }

    private fun addToQueue(track: Track) {
        val newQueue = state.queue + track
        val newIndex = if (state.queueIndex < 0 && state.nowPlaying == null) 0 else state.queueIndex

        state = state.copy(queue = newQueue, queueIndex = newIndex, isRadioMode = false)

        if (state.nowPlaying == null && !state.isLoadingAudio) {
            playFromQueue(0)
        }
    }

    private fun removeFromQueue(index: Int) {
        if (index < 0 || index >= state.queue.size) return
        val removingPlaying = index == state.queueIndex
        val newQueue = state.queue.toMutableList().also { it.removeAt(index) }
        val newIndex = when {
            newQueue.isEmpty() -> -1
            index < state.queueIndex -> state.queueIndex - 1
            index == state.queueIndex -> minOf(index, newQueue.lastIndex)
            else -> state.queueIndex
        }
        val newCursor = minOf(state.queueCursor, (newQueue.size - 1).coerceAtLeast(0))
        state = state.copy(queue = newQueue, queueIndex = newIndex, queueCursor = newCursor)

        if (removingPlaying) {
            audioPlayer.stop()
            if (newQueue.isEmpty()) {
                state = state.copy(nowPlaying = null, isPlaying = false, progress = 0.0)
            } else {
                val nextIndex = if (newIndex >= 0 && newIndex < newQueue.size) newIndex else 0
                playFromQueue(nextIndex)
            }
        }
    }

    private fun clearQueue() {
        audioPlayer.stop()
        state = state.copy(
            queue = emptyList(),
            queueIndex = -1,
            queueCursor = 0,
            nowPlaying = null,
            isPlaying = false,
            progress = 0.0,
        )
    }

    private fun toggleQueue() {
        state = state.copy(isQueueVisible = !state.isQueueVisible)
    }

    private fun toggleShuffle() {
        state = state.copy(shuffleEnabled = !state.shuffleEnabled)
    }

    private fun cycleRepeat() {
        val next = when (state.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        state = state.copy(repeatMode = next)
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