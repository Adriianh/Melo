package com.github.adriianh.melo

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.network.NetworkMonitor
import com.github.adriianh.core.domain.usecase.library.GetRemoteHistoryUseCase
import com.github.adriianh.core.domain.usecase.offline.GetOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.ScanLocalTracksUseCase
import com.github.adriianh.core.domain.usecase.playback.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.search.BrowseCategoryUseCase
import com.github.adriianh.core.domain.usecase.search.DeleteSearchQueryUseCase
import com.github.adriianh.core.domain.usecase.search.GetChartsUseCase
import com.github.adriianh.core.domain.usecase.search.GetExploreUseCase
import com.github.adriianh.core.domain.usecase.search.GetMoodAndGenresUseCase
import com.github.adriianh.core.domain.usecase.search.GetSearchHistoryUseCase
import com.github.adriianh.core.domain.usecase.search.GetSearchSuggestionsUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrendingUseCase
import com.github.adriianh.core.domain.usecase.search.SaveSearchQueryUseCase
import com.github.adriianh.core.domain.usecase.search.SearchAlbumsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchArtistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchSummaryUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.adriianh.core.domain.usecase.search.SearchVideosUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.melo.ui.search.SearchViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val searchTracksUseCase = mockk<SearchTracksUseCase>(relaxed = true)
    private val searchAlbumsUseCase = mockk<SearchAlbumsUseCase>(relaxed = true)
    private val searchArtistsUseCase = mockk<SearchArtistsUseCase>(relaxed = true)
    private val searchPlaylistsUseCase = mockk<SearchPlaylistsUseCase>(relaxed = true)
    private val searchVideosUseCase = mockk<SearchVideosUseCase>(relaxed = true)
    private val searchSummaryUseCase = mockk<SearchSummaryUseCase>(relaxed = true)
    private val getExploreUseCase = mockk<GetExploreUseCase>(relaxed = true)
    private val getChartsUseCase = mockk<GetChartsUseCase>(relaxed = true)
    private val getTrendingUseCase = mockk<GetTrendingUseCase>(relaxed = true)
    private val getMoodAndGenresUseCase = mockk<GetMoodAndGenresUseCase>(relaxed = true)
    private val getSearchHistoryUseCase = mockk<GetSearchHistoryUseCase>(relaxed = true)
    private val saveSearchQueryUseCase = mockk<SaveSearchQueryUseCase>(relaxed = true)
    private val deleteSearchQueryUseCase = mockk<DeleteSearchQueryUseCase>(relaxed = true)
    private val getSearchSuggestionsUseCase = mockk<GetSearchSuggestionsUseCase>(relaxed = true)
    private val getRecentTracksUseCase = mockk<GetRecentTracksUseCase>(relaxed = true)
    private val getRemoteHistoryUseCase = mockk<GetRemoteHistoryUseCase>(relaxed = true)
    private val browseCategoryUseCase = mockk<BrowseCategoryUseCase>(relaxed = true)
    private val getSettingsUseCase = mockk<GetSettingsUseCase>(relaxed = true)
    private val getOfflineTracksUseCase = mockk<GetOfflineTracksUseCase>(relaxed = true)
    private val scanLocalTracksUseCase = mockk<ScanLocalTracksUseCase>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>(relaxed = true)

    private val settingsFlow = MutableStateFlow(Settings())
    private val isOnlineFlow = MutableStateFlow(true)

    private val sampleTrack =
        Track(
            id = "t1",
            title = "Viva La Vida",
            artist = "Coldplay",
            album = "Viva La Vida",
            durationMs = 240_000,
            genres = emptyList(),
            artworkUrl = "http://art1.jpg",
            sourceId = null,
        )

    private val localTrack =
        Track(
            id = "t2",
            title = "Yellow",
            artist = "Coldplay",
            album = "Parachutes",
            durationMs = 260_000,
            genres = emptyList(),
            artworkUrl = "http://art2.jpg",
            sourceId = null,
        )

    private val offlineTrackItem =
        OfflineTrack(
            track = sampleTrack,
            localFilePath = "/path/to/t1.mp3",
            downloadStatus = DownloadStatus.COMPLETED,
            downloadType = DownloadType.MANUAL,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getSettingsUseCase() } returns settingsFlow
        coEvery { getSettingsUseCase.getSnapshot() } answers { settingsFlow.value }
        every { networkMonitor.isOnline } returns isOnlineFlow
        every { getSearchHistoryUseCase.invoke(any(), any()) } returns flowOf(emptyList())
        every { getOfflineTracksUseCase() } returns flowOf(listOf(offlineTrackItem))
        coEvery { scanLocalTracksUseCase() } returns listOf(localTrack)
        every { getRecentTracksUseCase.invoke(any()) } returns flowOf(emptyList())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SearchViewModel =
        SearchViewModel(
            searchTracksUseCase = searchTracksUseCase,
            searchAlbumsUseCase = searchAlbumsUseCase,
            searchArtistsUseCase = searchArtistsUseCase,
            searchPlaylistsUseCase = searchPlaylistsUseCase,
            searchVideosUseCase = searchVideosUseCase,
            searchSummaryUseCase = searchSummaryUseCase,
            getExploreUseCase = getExploreUseCase,
            getChartsUseCase = getChartsUseCase,
            getTrendingUseCase = getTrendingUseCase,
            getMoodAndGenresUseCase = getMoodAndGenresUseCase,
            getSearchHistoryUseCase = getSearchHistoryUseCase,
            saveSearchQueryUseCase = saveSearchQueryUseCase,
            deleteSearchQueryUseCase = deleteSearchQueryUseCase,
            getSearchSuggestionsUseCase = getSearchSuggestionsUseCase,
            getRecentTracksUseCase = getRecentTracksUseCase,
            getRemoteHistoryUseCase = getRemoteHistoryUseCase,
            browseCategoryUseCase = browseCategoryUseCase,
            getSettingsUseCase = getSettingsUseCase,
            getOfflineTracksUseCase = getOfflineTracksUseCase,
            scanLocalTracksUseCase = scanLocalTracksUseCase,
            networkMonitor = networkMonitor,
        )

    @Test
    fun `when starting in offline mode, loads offline explore and does not query online explore`() =
        runTest {
            settingsFlow.value = Settings(offlineMode = true)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isOffline)
            assertTrue(state.isOfflineSearch)
            assertTrue(state.moodAndGenres.isEmpty())
            assertFalse(state.isLoadingMoodAndGenres)
            assertEquals(2, state.exploreSections.size)
            assertEquals("Descargas", state.exploreSections[0].title)
            assertEquals("Archivos locales", state.exploreSections[1].title)

            coVerify(exactly = 0) { getExploreUseCase() }
            coVerify(exactly = 0) { getChartsUseCase() }
            coVerify(exactly = 0) { getTrendingUseCase() }
            coVerify(exactly = 0) { getMoodAndGenresUseCase() }
        }

    @Test
    fun `when offline, executeSearch searches only local and downloaded tracks`() =
        runTest {
            settingsFlow.value = Settings(offlineMode = true)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.executeSearch("Viva")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isOfflineSearch)
            assertEquals(1, state.songResults.size)
            assertEquals("Viva La Vida", state.songResults[0].title)
            assertEquals(1, state.albumResults.size)
            assertEquals("Viva La Vida", state.albumResults[0].title)
            assertEquals(0, state.playlistResults.size)
            assertEquals(0, state.videoResults.size)

            coVerify(exactly = 0) { searchTracksUseCase(any()) }
            coVerify(exactly = 0) { searchSummaryUseCase(any()) }
        }

    @Test
    fun `when offline, onQueryChange does not request search suggestions`() =
        runTest {
            settingsFlow.value = Settings(offlineMode = true)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onQueryChange("Cold")
            advanceTimeBy(300)
            advanceUntilIdle()

            val suggestions = viewModel.uiState.value.suggestions
            assertTrue(suggestions.isEmpty())
            coVerify(exactly = 0) { getSearchSuggestionsUseCase(any()) }
        }

    @Test
    fun `when offline, browseCategory is ignored`() =
        runTest {
            settingsFlow.value = Settings(offlineMode = true)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.browseCategory("browse_1", null)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isBrowsingCategory)
            coVerify(exactly = 0) { browseCategoryUseCase(any(), any()) }
        }

    @Test
    fun `when toggling offline mode to active, switches to offline search immediately`() =
        runTest {
            settingsFlow.value = Settings(offlineMode = false)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.executeSearch("Coldplay")
            advanceUntilIdle()

            settingsFlow.value = Settings(offlineMode = true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isOffline)
            assertTrue(state.isOfflineSearch)
            assertEquals(2, state.songResults.size)
            assertEquals(1, state.artistResults.size)
            assertEquals("Coldplay", state.artistResults[0].name)
        }

    @Test
    fun `when offline, recent history filters out tracks not available offline`() =
        runTest {
            val nonOfflineTrack =
                Track(
                    id = "t_online_only",
                    title = "Online Only",
                    artist = "Artist",
                    album = "Album",
                    durationMs = 180_000,
                    genres = emptyList(),
                    artworkUrl = null,
                    sourceId = null,
                )
            every { getRecentTracksUseCase.invoke(any()) } returns
                flowOf(
                    listOf(
                        HistoryEntry(track = sampleTrack, playedAt = 1000L),
                        HistoryEntry(track = nonOfflineTrack, playedAt = 2000L),
                    ),
                )
            settingsFlow.value = Settings(offlineMode = true)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.recentHistory.size)
            assertEquals("t1", state.recentHistory[0].id)
            coVerify(exactly = 0) { getRemoteHistoryUseCase() }
        }
}
