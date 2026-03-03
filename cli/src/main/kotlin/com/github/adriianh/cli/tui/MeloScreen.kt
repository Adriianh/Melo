package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.GREEN
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.core.domain.model.SimilarTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.usecase.*
import dev.tamboui.image.Image
import dev.tamboui.image.ImageScaling
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Flex
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.app.ToolkitApp
import dev.tamboui.toolkit.app.ToolkitRunner
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
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
) : ToolkitApp() {

    private var state = MeloState()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var detailsJob: Job? = null
    private var loadMoreJob: Job? = null
    private var lastQuery = ""
    private var marqueeJob: ToolkitRunner.ScheduledAction? = null
    private var marqueeTick = 0  // counts ticks before scrolling starts

    // UI state holders
    private val searchInputState = TextInputState()
    private val resultList: ListElement<*> = list()
        .highlightSymbol("▸ ")
        .highlightColor(GREEN)
        .autoScroll()
        .scrollbar()

    private val sidebarList: ListElement<*> = list()
        .items("🏠 Home", "🔍 Search", "📚 Your Library")
        .highlightSymbol("▸ ")
        .highlightColor(GREEN)
        .selected(1)
        .focusable()
        .id("sidebar-list")
        .onKeyEvent(::handleSidebarKey)

    private val lyricsArea = markupTextArea()
        .scrollbar()
        .wrapWord()
        .focusable()
        .id("lyrics-area")


    private val similarArea: ListElement<*> = list()
        .highlightSymbol("• ")
        .highlightColor(GREEN)
        .scrollbar()
        .focusable()
        .id("similar-area")

    override fun configure(): TuiConfig {
        return TuiConfig.builder()
            .mouseCapture(true)
            .build()
    }

    override fun onStart() {
        marqueeJob = runner()?.scheduleRepeating({
            runner()?.runOnRenderThread {
                marqueeTick++
                // 10 ticks (~1.5s) delay before starting scroll
                if (marqueeTick > 10) {
                    val track = state.selectedTrack
                    val newOffset = state.marqueeOffset + 1
                    // Detect wrap-around: when the offset reaches the start of the loop again,
                    // reset marqueeTick to re-apply the pause before scrolling resumes
                    if (track != null) {
                        val separator = "   •   "
                        val full = track.title + separator
                        if (newOffset % full.length == 0) {
                            marqueeTick = 0
                        }
                    }
                    state = state.copy(marqueeOffset = newOffset)
                }
            }
        }, Duration.ofMillis(150))
    }

    override fun onStop() {
        marqueeJob?.cancel()
    }

    // ───────────────────────────────── Render ─────────────────────────────────

    override fun render(): Element {
        return dock()
            .top(renderSearchBar(), Constraint.length(3))
            .bottom(renderPlayerBar(), Constraint.length(3))
            .left(renderSidebar(), Constraint.length(22))
            .center(renderMainContent())
    }

    // ── Search Bar (Top) ──

    private fun renderSearchBar(): Element {
        return panel(
            row(
                text("♫ Melo").bold().fg(GREEN).length(8),
                textInput(searchInputState)
                    .placeholder("Search for songs, artists...")
                    .onSubmit(::performSearch)
                    .fill()
            )
        ).rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .id("search-bar")
            .onKeyEvent(::handleSearchBarKey)
    }

    // ── Sidebar (Left) ──

    private fun renderSidebar(): Element {
        return panel(
            column(
                sidebarList.fill()
            )
        ).title("Navigation")
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
    }

    // ── Main Content (Center) ──

    private fun renderMainContent(): Element {
        val content = when {
            state.isLoading -> panel(
                column(
                    spacer(),
                    text("  Searching...").dim().centered(),
                    spacer()
                )
            ).title("Results").rounded().borderColor(BORDER_DEFAULT)

            state.errorMessage != null -> panel(
                text(state.errorMessage!!).fg(MeloTheme.ACCENT_RED)
            ).title("Error").rounded().borderColor(MeloTheme.ACCENT_RED)

            state.results.isEmpty() -> panel(
                column(
                    spacer(),
                    text("  Search for music to get started").fg(TEXT_SECONDARY).centered(),
                    text("  Press Tab to focus the search bar").fg(TEXT_DIM).centered(),
                    spacer()
                )
            ).title("Melo").rounded().borderColor(BORDER_DEFAULT)

            else -> renderResultsArea()
        }

        return content
    }

    private fun renderResultsArea(): Element {
        val items = state.results.mapIndexed { index, track ->
            val duration = formatDuration(track.durationMs)
            val nowPlayingIndicator = if (track.id == state.nowPlaying?.id) "♫ " else "  "
            val isSelected = index == state.selectedIndex
            val titleText = if (isSelected) {
                marqueeText(track.title, state.marqueeOffset, 40)
            } else {
                track.title
            }
            row(
                text(nowPlayingIndicator).fg(GREEN).length(2),
                text("${index + 1}").dim().length(3),
                text(titleText).fg(TEXT_PRIMARY).apply { if (!isSelected) ellipsisMiddle() }.fill(),
                text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
                text(duration).fg(TEXT_DIM).length(6)
            )
        }

        resultList.elements(*items.toTypedArray())

        val resultsTitle = if (state.isLoadingMore) "Searching... ↓" else "Search Results"

        val header = row(
            text("").length(2),
            text("#").dim().length(3),
            text("Title").dim().fill(),
            text("Artist").dim().percent(25),
            text("Time").dim().length(6)
        ).margin(Margin.horizontal(1))

        val resultsPanel = panel(
            column(
                header,
                text("").length(1),
                resultList.fill()
            )
        )
            .title(resultsTitle)
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("results-panel")
            .onKeyEvent(::handleResultsKey)

        return if (state.selectedTrack != null) {
            dock()
                .center(resultsPanel)
                .right(renderDetailPanel(), Constraint.percentage(35))
        } else {
            dock().center(resultsPanel)
        }
    }

    // ── Detail Panel (Right) ──

    private fun renderDetailPanel(): Element {
        val track = state.selectedTrack ?: return spacer()

        val detailTabs = tabs("Info", "Lyrics", "Similar")
            .selected(state.detailTab.ordinal)
            .highlightColor(GREEN)
            .divider(" │ ")

        val tabContent = when (state.detailTab) {
            DetailTab.INFO -> renderTrackMetadata(track)
            DetailTab.LYRICS -> renderLyricsTab()
            DetailTab.SIMILAR -> renderSimilarTab()
        }

        val layeredContent = if (state.detailTab != DetailTab.INFO) {
            stack(
                ClearGraphicsElement().fill(),
                tabContent.fill()
            )
        } else {
            column(
                renderArtwork(),
                tabContent.fill()
            )
        }

        return panel(
            detailTabs.length(1),
            layeredContent.fill()
        ).title("Now Playing")
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("detail-panel")
            .onKeyEvent(::handleDetailKey)
    }

    private fun renderTrackMetadata(track: Track): StyledElement<*> {
        return column(
            text(marqueeText(track.title, state.marqueeOffset, 30)).bold().fg(TEXT_PRIMARY),
            text(marqueeText(track.artist, state.marqueeOffset, 30)).fg(TEXT_SECONDARY),
            text(""),
            if (track.sourceId != null) text("✓ Available for streaming").dim().fg(GREEN)
            else text("✗ Not available for streaming").dim()
        ).flex(Flex.START)
    }

    private fun renderArtwork(): StyledElement<*> {
        return if (state.artworkData != null) {
            widget(Image.builder()
                .data(state.artworkData)
                .scaling(ImageScaling.FIT)
                .block(dev.tamboui.widgets.block.Block.builder()
                    .borders(dev.tamboui.widgets.block.Borders.ALL)
                    .borderType(dev.tamboui.widgets.block.BorderType.ROUNDED)
                    .build())
                .build()).length(18)
        } else {
            stack(
                ClearGraphicsElement().fill(),
                panel(text(" [ No Artwork ] ").dim().centered()).rounded().fit().length(5)
            )
        }
    }

    private fun renderLyricsTab(): StyledElement<*> {
        return when {
            state.isLoadingLyrics -> column(
                spacer(),
                text("  Loading lyrics...").dim().centered(),
                spacer()
            )
            state.lyrics != null -> lyricsArea.markup(state.lyrics!!).fill()
            else -> column(
                spacer(),
                text("  Press Enter to load lyrics").fg(TEXT_SECONDARY).centered(),
                spacer()
            )
        }
    }

    private fun renderSimilarTab(): StyledElement<*> {
        return when {
            state.similarTracks.isEmpty() -> column(
                spacer(),
                text("  No similar tracks found").fg(TEXT_SECONDARY).centered(),
                spacer()
            )
            else -> {
                val items = state.similarTracks.map { similar ->
                    val matchPercent = (similar.match * 100).toInt()
                    row(
                        text("• ").fg(GREEN).length(2),
                        text(similar.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
                        text(similar.artist).fg(TEXT_SECONDARY).ellipsis().percent(30),
                        text("($matchPercent%)").dim().length(6)
                    )
                }
                similarArea.elements(*items.toTypedArray())
                similarArea.fill()
            }
        }
    }

    // ── Player Bar (Bottom) ──

    private fun renderPlayerBar(): Element {
        val nowPlaying = state.nowPlaying

        val trackInfo = if (nowPlaying != null) {
            row(
                text(if (state.isPlaying) "▶" else "⏸").fg(GREEN).length(2),
                text(nowPlaying.title).bold().fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
                text(" — ").fg(TEXT_DIM).length(3),
                text(nowPlaying.artist).fg(TEXT_SECONDARY).ellipsis().fill()
            )
        } else {
            text("  No track selected").fg(TEXT_DIM)
        }

        val progressBar = if (nowPlaying != null) {
            row(
                text(formatProgress(state.progress, nowPlaying.durationMs)).fg(TEXT_DIM).length(13),
                lineGauge((state.progress * 100).toInt())
                    .filledColor(GREEN)
                    .unfilledColor(TEXT_DIM)
                    .fill()
            )
        } else {
            lineGauge(0)
                .filledColor(TEXT_DIM)
                .unfilledColor(TEXT_DIM)
        }

        val volumeBar = row(
            text(if (state.volume > 50) "🔊" else if (state.volume > 0) "🔉" else "🔇")
                .length(2),
            lineGauge(state.volume)
                .filledColor(TEXT_PRIMARY)
                .unfilledColor(TEXT_DIM)
                .length(8)
        )

        return panel(
            row(
                trackInfo.percent(35),
                progressBar.fill(),
                volumeBar.length(12)
            )
        ).rounded()
            .borderColor(BORDER_DEFAULT)
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

    private fun handleSidebarKey(event: KeyEvent): EventResult {
        when {
            event.matches(Actions.SELECT) -> {
                val section = SidebarSection.entries.getOrNull(sidebarList.selected())
                if (section != null) {
                    state = state.copy(activeSection = section)
                }
                return EventResult.HANDLED
            }
        }
        return EventResult.UNHANDLED
    }

    private fun handleResultsKey(event: KeyEvent): EventResult {
        if (state.results.isEmpty()) return EventResult.UNHANDLED

        when {
            event.matches(Actions.SELECT) -> {
                val selected = state.results.getOrNull(resultList.selected())
                if (selected != null) {
                    state = state.copy(
                        selectedTrack = selected,
                        nowPlaying = selected,
                        isPlaying = true,
                        progress = 0.0,
                        marqueeOffset = 0
                    )
                    marqueeTick = 0
                    loadTrackDetails(selected.id, selected)
                }
                return EventResult.HANDLED
            }

            event.matches(Actions.MOVE_DOWN) -> {
                val newIndex = minOf(state.results.lastIndex, state.selectedIndex + 1)
                resultList.selected(newIndex)

                val track = state.results.getOrNull(newIndex)
                if (track != null) {
                    state = state.copy(selectedIndex = newIndex, selectedTrack = track, marqueeOffset = 0)
                    marqueeTick = 0
                    debouncedLoadDetails(track)
                }

                if (newIndex >= state.results.size - 5 && !state.isLoadingMore && state.hasMore) {
                    loadMore()
                }
                return EventResult.HANDLED
            }

            event.matches(Actions.MOVE_UP) -> {
                val newIndex = maxOf(0, state.selectedIndex - 1)
                resultList.selected(newIndex)

                val track = state.results.getOrNull(newIndex)
                if (track != null) {
                    state = state.copy(selectedIndex = newIndex, selectedTrack = track, marqueeOffset = 0)
                    marqueeTick = 0
                    debouncedLoadDetails(track)
                }
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

    // ─────────────────────────────── Actions ─────────────────────────────────

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
            hasMore = true
        )

        scope.launch {
            try {
                val results = searchTracks(query)
                val firstTrack = results.firstOrNull()
                state = state.copy(
                    results = results,
                    isLoading = false,
                    selectedIndex = 0,
                    selectedTrack = firstTrack,
                    hasMore = loadMoreTracks.hasMore(results.size)
                )
                resultList.selected(0)
                focusResults()
                if (firstTrack != null) {
                    loadTrackDetails(firstTrack.id)
                }
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Search failed: ${e.message}"
                )
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
                if (isActive) {
                    state = state.copy(
                        results = state.results + more,
                        isLoadingMore = false,
                        hasMore = loadMoreTracks.hasMore(offset + more.size)
                    )
                }
            } catch (_: Exception) {
                state = state.copy(isLoadingMore = false)
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
        state = state.copy(
            lyrics = null,
            isLoadingLyrics = false,
            similarTracks = emptyList(),
            artworkData = null
        )

        detailsJob = scope.launch {
            val fullTrackDeferred = async { getTrack(trackId) }
            val similarDeferred = async {
                val artist = knownTrack?.artist ?: fullTrackDeferred.await()?.artist
                    ?: return@async emptyList<SimilarTrack>()
                val title = knownTrack?.title ?: fullTrackDeferred.await()?.title
                    ?: return@async emptyList<SimilarTrack>()
                getSimilarTracks(artist, title)
            }

            val fullTrack = fullTrackDeferred.await() ?: knownTrack ?: return@launch
            val artworkData = fullTrack.artworkUrl?.let { ArtworkRenderer.load(it) }

            if (isActive) {
                state = state.copy(
                    selectedTrack = fullTrack,
                    artworkData = artworkData
                )
                val similar = similarDeferred.await()
                if (isActive) state = state.copy(similarTracks = similar)
            }
        }
    }

    private fun loadLyrics() {
        val track = state.selectedTrack ?: return
        state = state.copy(isLoadingLyrics = true, lyrics = null)
        scope.launch {
            val lyrics = getLyrics(track.artist, track.title)
            state = state.copy(
                lyrics = lyrics ?: "Lyrics not found",
                isLoadingLyrics = false
            )
        }
    }

    private fun focusResults() {
        runner()?.focusManager()?.setFocus("results-panel")
    }

    // ─────────────────────────────── Utilities ────────────────────────────────

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    private fun formatProgress(progress: Double, durationMs: Long): String {
        val currentMs = (progress * durationMs).toLong()
        return "${formatDuration(currentMs)} / ${formatDuration(durationMs)}"
    }

    /**
     * Returns a scrolling (marquee) version of [text] based on [offset].
     * If the text fits within [maxWidth] no scrolling is applied.
     * The text loops with a gap separator: "Long title...   Long title..."
     */
    private fun marqueeText(text: String, offset: Int, maxWidth: Int): String {
        if (text.length <= maxWidth) return text
        val separator = "   •   "
        val full = text + separator
        val loop = full.repeat(2)  // enough to always slice a window
        val start = offset % full.length
        return loop.substring(start, start + maxWidth)
    }
}