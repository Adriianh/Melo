package com.github.adriianh.melo

import com.github.adriianh.core.domain.cache.HomeFeedCache
import com.github.adriianh.core.domain.model.HomeFeed
import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.offline.GetOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.ScanLocalTracksUseCase
import com.github.adriianh.core.domain.usecase.playback.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.search.GetChartsUseCase
import com.github.adriianh.core.domain.usecase.search.GetExploreUseCase
import com.github.adriianh.core.domain.usecase.search.GetHomeUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrendingUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.melo.ui.home.HomeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getHomeUseCase = mockk<GetHomeUseCase>(relaxed = true)
    private val getExploreUseCase = mockk<GetExploreUseCase>(relaxed = true)
    private val getChartsUseCase = mockk<GetChartsUseCase>(relaxed = true)
    private val getTrendingUseCase = mockk<GetTrendingUseCase>(relaxed = true)
    private val searchTracksUseCase = mockk<SearchTracksUseCase>(relaxed = true)
    private val getSettingsUseCase = mockk<GetSettingsUseCase>(relaxed = true)
    private val getOfflineTracksUseCase = mockk<GetOfflineTracksUseCase>(relaxed = true)
    private val scanLocalTracksUseCase = mockk<ScanLocalTracksUseCase>(relaxed = true)
    private val getRecentTracksUseCase = mockk<GetRecentTracksUseCase>(relaxed = true)
    private val homeFeedCache = mockk<HomeFeedCache>(relaxed = true)

    private val settingsFlow = MutableStateFlow(Settings())

    private val sampleTrack = Track(
        id = "track_1",
        title = "Sample Song",
        artist = "Sample Artist",
        album = "Sample Album",
        durationMs = 210000L,
        genres = listOf("Pop"),
        artworkUrl = "https://example.com/art.jpg",
        sourceId = "s1"
    )

    private val sampleSection = HomeSection(
        title = "Quick Picks",
        type = HomeSectionType.SONGS,
        items = listOf(SearchResult.Song(sampleTrack))
    )

    private val sampleCachedFeed = HomeFeed(
        chips = emptyList(),
        sections = listOf(sampleSection),
        continuation = null
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getSettingsUseCase() } returns settingsFlow
        coEvery { getSettingsUseCase.getSnapshot() } returns Settings()
        coEvery { getExploreUseCase() } returns emptyList()
        coEvery { getChartsUseCase() } returns emptyList()
        coEvery { getTrendingUseCase() } returns emptyList()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when cache exists, ViewModel shows cached feed after coroutines advance`() = runTest {
        coEvery { homeFeedCache.get() } returns sampleCachedFeed
        coEvery { getHomeUseCase(any(), any()) } returns sampleCachedFeed

        val viewModel = HomeViewModel(
            getHomeUseCase = getHomeUseCase,
            getExploreUseCase = getExploreUseCase,
            getChartsUseCase = getChartsUseCase,
            getTrendingUseCase = getTrendingUseCase,
            searchTracksUseCase = searchTracksUseCase,
            getSettingsUseCase = getSettingsUseCase,
            getOfflineTracksUseCase = getOfflineTracksUseCase,
            scanLocalTracksUseCase = scanLocalTracksUseCase,
            getRecentTracksUseCase = getRecentTracksUseCase,
            homeFeedCache = homeFeedCache,
            ioDispatcher = testDispatcher,
        )

        // Cache is now loaded asynchronously — initial state is still loading
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        // After coroutines advance, cache is applied
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.sections.size)
        assertEquals("Quick Picks", viewModel.uiState.value.sections.first().title)
    }

    @Test
    fun `when cache is empty, ViewModel shows loading and then displays fetched feed and saves to cache`() =
        runTest {
            coEvery { homeFeedCache.get() } returns null
            coEvery { getHomeUseCase(any(), any()) } returns sampleCachedFeed

            val viewModel = HomeViewModel(
                getHomeUseCase = getHomeUseCase,
                getExploreUseCase = getExploreUseCase,
                getChartsUseCase = getChartsUseCase,
                getTrendingUseCase = getTrendingUseCase,
                searchTracksUseCase = searchTracksUseCase,
                getSettingsUseCase = getSettingsUseCase,
                getOfflineTracksUseCase = getOfflineTracksUseCase,
                scanLocalTracksUseCase = scanLocalTracksUseCase,
                getRecentTracksUseCase = getRecentTracksUseCase,
                homeFeedCache = homeFeedCache,
                ioDispatcher = testDispatcher,
            )

            // Initially loading because no cache
            assertTrue(viewModel.uiState.value.isLoading)

            advanceUntilIdle()

            // After fetch completes
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(1, viewModel.uiState.value.sections.size)
            coVerify { homeFeedCache.save(any()) }
        }

    @Test
    fun `when fetch fails but cache was present, cached sections are preserved`() = runTest {
        coEvery { homeFeedCache.get() } returns sampleCachedFeed
        coEvery { getHomeUseCase(any(), any()) } throws RuntimeException("Network error")

        val viewModel = HomeViewModel(
            getHomeUseCase = getHomeUseCase,
            getExploreUseCase = getExploreUseCase,
            getChartsUseCase = getChartsUseCase,
            getTrendingUseCase = getTrendingUseCase,
            searchTracksUseCase = searchTracksUseCase,
            getSettingsUseCase = getSettingsUseCase,
            getOfflineTracksUseCase = getOfflineTracksUseCase,
            scanLocalTracksUseCase = scanLocalTracksUseCase,
            getRecentTracksUseCase = getRecentTracksUseCase,
            homeFeedCache = homeFeedCache,
            ioDispatcher = testDispatcher,
        )

        advanceUntilIdle()

        // Does not wipe cached data
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.sections.size)
        assertEquals("Quick Picks", viewModel.uiState.value.sections.first().title)
    }
}