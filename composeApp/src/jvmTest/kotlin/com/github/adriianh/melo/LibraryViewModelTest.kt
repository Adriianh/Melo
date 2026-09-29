package com.github.adriianh.melo

import com.github.adriianh.core.domain.cache.LibraryCache
import com.github.adriianh.core.domain.manager.DownloadManager
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.LibraryCacheData
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.usecase.library.AddTrackToPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.AddTracksToPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.CreatePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.DeletePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.GetAccountProfileUseCase
import com.github.adriianh.core.domain.usecase.library.GetLikedSongsUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistIdsForTrackUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetRemoteHistoryUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserAlbumsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserArtistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.RenamePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeTrackUseCase
import com.github.adriianh.core.domain.usecase.offline.DeleteDownloadedTrackUseCase
import com.github.adriianh.core.domain.usecase.offline.EnrichLocalTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.GetOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.ScanLocalTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.SyncOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.playback.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.adriianh.melo.ui.library.LibraryViewModel
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val getSettingsUseCase = mockk<GetSettingsUseCase>(relaxed = true)
    private val updateSettingsUseCase = mockk<UpdateSettingsUseCase>(relaxed = true)
    private val getAccountProfileUseCase = mockk<GetAccountProfileUseCase>(relaxed = true)
    private val getUserPlaylistsUseCase = mockk<GetUserPlaylistsUseCase>(relaxed = true)
    private val getPlaylistsUseCase = mockk<GetPlaylistsUseCase>(relaxed = true)
    private val createPlaylistUseCase = mockk<CreatePlaylistUseCase>(relaxed = true)
    private val renamePlaylistUseCase = mockk<RenamePlaylistUseCase>(relaxed = true)
    private val deletePlaylistUseCase = mockk<DeletePlaylistUseCase>(relaxed = true)
    private val addTrackToPlaylistUseCase = mockk<AddTrackToPlaylistUseCase>(relaxed = true)
    private val addTracksToPlaylistUseCase = mockk<AddTracksToPlaylistUseCase>(relaxed = true)
    private val getPlaylistIdsForTrackUseCase = mockk<GetPlaylistIdsForTrackUseCase>(relaxed = true)
    private val getLikedSongsUseCase = mockk<GetLikedSongsUseCase>(relaxed = true)
    private val getUserArtistsUseCase = mockk<GetUserArtistsUseCase>(relaxed = true)
    private val getUserAlbumsUseCase = mockk<GetUserAlbumsUseCase>(relaxed = true)
    private val getRemoteHistoryUseCase = mockk<GetRemoteHistoryUseCase>(relaxed = true)
    private val getRecentTracksUseCase = mockk<GetRecentTracksUseCase>(relaxed = true)
    private val toggleLikeTrackUseCase = mockk<ToggleLikeTrackUseCase>(relaxed = true)
    private val getOfflineTracksUseCase = mockk<GetOfflineTracksUseCase>(relaxed = true)
    private val scanLocalTracksUseCase = mockk<ScanLocalTracksUseCase>(relaxed = true)
    private val enrichLocalTracksUseCase = mockk<EnrichLocalTracksUseCase>(relaxed = true)
    private val deleteDownloadedTrackUseCase = mockk<DeleteDownloadedTrackUseCase>(relaxed = true)
    private val syncOfflineTracksUseCase = mockk<SyncOfflineTracksUseCase>(relaxed = true)
    private val downloadManager = mockk<DownloadManager>(relaxed = true)
    private val playbackManager = mockk<PlaybackManager>(relaxed = true)
    private val libraryCache = mockk<LibraryCache>(relaxed = true)

    private val settingsFlow = MutableStateFlow(Settings(sessionCookies = "auth_cookie"))

    private val sampleTrack =
        Track(
            id = "track_1",
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 200_000L,
            genres = listOf("Pop"),
            artworkUrl = null,
            sourceId = "s1",
        )

    private val samplePlaylist =
        SearchResult.Playlist(
            id = "pl_1",
            title = "Test Playlist",
            author = "Test Author",
            trackCount = 10,
            artworkUrl = null,
        )

    private val sampleAlbum =
        SearchResult.Album(
            id = "alb_1",
            title = "Test Album",
            author = "Test Artist",
            year = "2026",
            artworkUrl = null,
        )

    private val sampleArtist =
        SearchResult.Artist(
            id = "art_1",
            name = "Test Artist",
            artworkUrl = null,
        )

    private val sampleProfile =
        AccountProfile(
            name = "Test User",
            email = "user@test.com",
            avatarUrl = null,
        )

    private val sampleCacheData =
        LibraryCacheData(
            playlists = listOf(samplePlaylist),
            likedSongs = listOf(sampleTrack),
            albums = listOf(sampleAlbum),
            artists = listOf(sampleArtist),
            profile = sampleProfile,
            remoteHistory = listOf(HistoryEntry(track = sampleTrack, playedAt = 1000L)),
            lastSyncedAt = 100_000L,
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getPlaylistsUseCase() } returns MutableStateFlow(emptyList())
        every { getRecentTracksUseCase(any()) } returns MutableStateFlow(emptyList())
        every { getOfflineTracksUseCase() } returns MutableStateFlow(emptyList())
        every { downloadManager.activeDownloads } returns MutableStateFlow(emptyMap())
        every { getSettingsUseCase() } returns settingsFlow
        coEvery { getSettingsUseCase.getSnapshot() } returns settingsFlow.value
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): LibraryViewModel =
        LibraryViewModel(
            getSettingsUseCase = getSettingsUseCase,
            updateSettingsUseCase = updateSettingsUseCase,
            getAccountProfileUseCase = getAccountProfileUseCase,
            getUserPlaylistsUseCase = getUserPlaylistsUseCase,
            getPlaylistsUseCase = getPlaylistsUseCase,
            createPlaylistUseCase = createPlaylistUseCase,
            renamePlaylistUseCase = renamePlaylistUseCase,
            deletePlaylistUseCase = deletePlaylistUseCase,
            addTrackToPlaylistUseCase = addTrackToPlaylistUseCase,
            addTracksToPlaylistUseCase = addTracksToPlaylistUseCase,
            getPlaylistIdsForTrackUseCase = getPlaylistIdsForTrackUseCase,
            getLikedSongsUseCase = getLikedSongsUseCase,
            getUserArtistsUseCase = getUserArtistsUseCase,
            getUserAlbumsUseCase = getUserAlbumsUseCase,
            getRemoteHistoryUseCase = getRemoteHistoryUseCase,
            getRecentTracksUseCase = getRecentTracksUseCase,
            toggleLikeTrackUseCase = toggleLikeTrackUseCase,
            getOfflineTracksUseCase = getOfflineTracksUseCase,
            scanLocalTracksUseCase = scanLocalTracksUseCase,
            enrichLocalTracksUseCase = enrichLocalTracksUseCase,
            deleteDownloadedTrackUseCase = deleteDownloadedTrackUseCase,
            syncOfflineTracksUseCase = syncOfflineTracksUseCase,
            downloadManager = downloadManager,
            playbackManager = playbackManager,
            libraryCache = libraryCache,
            observeLibraryUpdatesUseCase = null,
            ioDispatcher = testDispatcher,
        )

    @Test
    fun `when cache exists, ViewModel restores library state on init`() =
        runTest {
            coEvery { libraryCache.get() } returns sampleCacheData

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.playlists.size)
            assertEquals("Test Playlist", state.playlists.first().title)
            assertEquals(1, state.likedSongs.size)
            assertEquals("Test Song", state.likedSongs.first().title)
            assertEquals(1, state.albums.size)
            assertEquals(1, state.artists.size)
            assertEquals("Test User", state.profile?.name)
            assertEquals(1, state.history.size)
        }

    @Test
    fun `when onScreenVisible is called with fresh cache, network revalidation is skipped`() =
        runTest {
            val freshTimestamp = Clock.System.now().toEpochMilliseconds() - 60_000L
            val freshCache = sampleCacheData.copy(lastSyncedAt = freshTimestamp)
            every { libraryCache.getSync() } returns freshCache
            coEvery { libraryCache.get() } returns freshCache

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onScreenVisible()
            advanceUntilIdle()

            coVerify(exactly = 0) { getUserPlaylistsUseCase() }
            coVerify(exactly = 0) { getLikedSongsUseCase() }
        }

    @Test
    fun `when onScreenVisible is called with stale cache, background revalidation is triggered`() =
        runTest {
            val staleTimestamp = Clock.System.now().toEpochMilliseconds() - 15 * 60_000L
            val staleCache = sampleCacheData.copy(lastSyncedAt = staleTimestamp)
            every { libraryCache.getSync() } returns staleCache
            coEvery { libraryCache.get() } returns staleCache
            coEvery { getUserPlaylistsUseCase() } returns Result.success(emptyList())
            coEvery { getLikedSongsUseCase() } returns Result.success(emptyList())
            coEvery { getUserArtistsUseCase() } returns Result.success(emptyList())
            coEvery { getUserAlbumsUseCase() } returns Result.success(emptyList())
            coEvery { getRemoteHistoryUseCase() } returns Result.success(emptyList())
            coEvery { getAccountProfileUseCase() } returns Result.success(sampleProfile)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onScreenVisible()
            advanceUntilIdle()

            coVerify(atLeast = 1) { getUserPlaylistsUseCase() }
            coVerify(atLeast = 1) { getLikedSongsUseCase() }
        }

    @Test
    fun `when sessionCookies are cleared, ViewModel clears cache and resets state`() =
        runTest {
            coEvery { libraryCache.get() } returns sampleCacheData

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.playlists.size)

            settingsFlow.value = Settings(sessionCookies = null)
            advanceUntilIdle()

            coVerify { libraryCache.clear() }
            val state = viewModel.uiState.value
            assertTrue(state.playlists.isEmpty())
            assertTrue(state.likedSongs.isEmpty())
            assertTrue(state.albums.isEmpty())
            assertTrue(state.artists.isEmpty())
            assertNull(state.profile)
        }

    @Test
    fun `when refreshAll completes, updated snapshot is saved to cache`() =
        runTest {
            coEvery { getUserPlaylistsUseCase() } returns Result.success(listOf(samplePlaylist))
            coEvery { getLikedSongsUseCase() } returns Result.success(listOf(sampleTrack))
            coEvery { getUserArtistsUseCase() } returns Result.success(emptyList())
            coEvery { getUserAlbumsUseCase() } returns Result.success(emptyList())
            coEvery { getRemoteHistoryUseCase() } returns Result.success(emptyList())
            coEvery { getAccountProfileUseCase() } returns Result.success(sampleProfile)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.refreshAll()
            advanceUntilIdle()

            coVerify { libraryCache.save(any()) }
        }
}
