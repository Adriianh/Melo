package com.github.adriianh.data.player

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.player.PlaybackState
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackManagerTest {

    private val meloPlayerState = MutableStateFlow(PlaybackState())
    private val meloPlayer = mockk<MeloPlayer>(relaxed = true) {
        every { state } returns meloPlayerState
    }
    private val getStreamUseCase = mockk<GetStreamUseCase>()

    private fun managerScope() = TestScope(StandardTestDispatcher())

    private fun createManager(scope: TestScope = managerScope()) =
        PlaybackManagerImpl(meloPlayer, getStreamUseCase, scope)

    private fun fakeTrack(id: String, title: String = "Track $id") = Track(
        id = id, title = title, artist = "Artist",
        album = "Album", durationMs = 180_000,
        genres = emptyList(), artworkUrl = null, sourceId = null
    )

    @Test
    fun `playTrack sets queue with single track`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.playTrack(fakeTrack("1"))
        scope.advanceUntilIdle()

        assertEquals(1, manager.queueState.value.tracks.size)
        assertEquals("1", manager.queueState.value.currentTrack?.id)
    }

    @Test
    fun `playTrack resolves stream URL and loads player`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.playTrack(fakeTrack("1"))
        scope.advanceUntilIdle()

        coVerify { getStreamUseCase(any()) }
        verify { meloPlayer.load("http://stream.url", any()) }
    }

    @Test
    fun `addToQueue appends track to queue`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.addToQueue(fakeTrack("1"))
        scope.advanceUntilIdle()
        manager.addToQueue(fakeTrack("2"))
        scope.advanceUntilIdle()

        assertEquals(2, manager.queueState.value.tracks.size)
        assertEquals("2", manager.queueState.value.tracks[1].id)
    }

    @Test
    fun `addToQueue auto-starts when queue was empty`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.addToQueue(fakeTrack("1"))
        scope.advanceUntilIdle()

        assertEquals(0, manager.queueState.value.currentIndex)
        verify { meloPlayer.load(any(), any()) }
    }

    @Test
    fun `playNext advances to next track`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2"), fakeTrack("3")))
        scope.advanceUntilIdle()
        manager.playNext()
        scope.advanceUntilIdle()

        assertEquals(1, manager.queueState.value.currentIndex)
        assertEquals("2", manager.queueState.value.currentTrack?.id)
    }

    @Test
    fun `playNext does nothing at end of queue in NONE mode`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()
        manager.playNext() // → index 1
        scope.advanceUntilIdle()
        manager.playNext() // at end, should do nothing
        scope.advanceUntilIdle()

        assertEquals(1, manager.queueState.value.currentIndex)
    }

    @Test
    fun `playNext wraps around in ALL repeat mode`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()
        manager.toggleRepeat() // NONE → ALL
        manager.playNext()     // index 0 → 1
        scope.advanceUntilIdle()
        manager.playNext()     // index 1 → 0 (wrap)
        scope.advanceUntilIdle()

        assertEquals(0, manager.queueState.value.currentIndex)
    }

    @Test
    fun `playNext stays on same track in ONE repeat mode`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()
        manager.toggleRepeat() // NONE → ALL
        manager.toggleRepeat() // ALL → ONE
        manager.playNext()     // stays at 0
        scope.advanceUntilIdle()

        assertEquals(0, manager.queueState.value.currentIndex)
    }

    @Test
    fun `playPrevious seeks to start if progress greater than 3s`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()

        meloPlayerState.value = PlaybackState(
            currentTrack = fakeTrack("1"),
            isPlaying = true,
            progressMs = 5_000,
            durationMs = 180_000
        )

        manager.playPrevious()
        verify { meloPlayer.seekTo(0) }
    }

    @Test
    fun `playPrevious goes to previous track if progress less than 3s`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2"), fakeTrack("3")))
        scope.advanceUntilIdle()
        manager.playNext() // → index 1
        scope.advanceUntilIdle()

        meloPlayerState.value = PlaybackState(
            currentTrack = fakeTrack("2"),
            isPlaying = true,
            progressMs = 1_000,
            durationMs = 180_000
        )

        manager.playPrevious()
        scope.advanceUntilIdle()

        assertEquals(0, manager.queueState.value.currentIndex)
    }

    @Test
    fun `togglePlayPause pauses when playing`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1")))
        scope.advanceUntilIdle()

        meloPlayerState.value = PlaybackState(
            currentTrack = fakeTrack("1"), isPlaying = true
        )

        manager.togglePlayPause()
        verify { meloPlayer.pause() }
    }

    @Test
    fun `togglePlayPause plays when paused`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1")))
        scope.advanceUntilIdle()

        meloPlayerState.value = PlaybackState(
            currentTrack = fakeTrack("1"), isPlaying = false
        )

        manager.togglePlayPause()
        verify { meloPlayer.play() }
    }

    @Test
    fun `toggleRepeat cycles NONE to ALL`() = runTest {
        val manager = createManager()

        manager.toggleRepeat()

        assertEquals(RepeatMode.ALL, manager.queueState.value.repeatMode)
    }

    @Test
    fun `toggleRepeat cycles ALL to ONE`() = runTest {
        val manager = createManager()

        manager.toggleRepeat()
        manager.toggleRepeat()

        assertEquals(RepeatMode.ONE, manager.queueState.value.repeatMode)
    }

    @Test
    fun `toggleRepeat cycles ONE to NONE`() = runTest {
        val manager = createManager()

        manager.toggleRepeat()
        manager.toggleRepeat()
        manager.toggleRepeat()

        assertEquals(RepeatMode.NONE, manager.queueState.value.repeatMode)
    }

    @Test
    fun `toggleShuffle enables shuffle and keeps current track first`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2"), fakeTrack("3")))
        scope.advanceUntilIdle()
        manager.playNext() // → index 1
        scope.advanceUntilIdle()

        assertEquals("2", manager.queueState.value.currentTrack?.id)

        manager.toggleShuffle()

        assertTrue(manager.queueState.value.shuffleEnabled)
        assertEquals("2", manager.queueState.value.currentTrack?.id)
        assertEquals(0, manager.queueState.value.currentIndex)
    }

    @Test
    fun `toggleShuffle disables shuffle`() = runTest {
        val manager = createManager()

        manager.toggleShuffle()
        assertTrue(manager.queueState.value.shuffleEnabled)

        manager.toggleShuffle()
        assertFalse(manager.queueState.value.shuffleEnabled)
    }

    @Test
    fun `auto-advances when track finishes naturally`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()

        meloPlayerState.value = PlaybackState(
            currentTrack = fakeTrack("1"),
            isPlaying = false,
            isBuffering = false,
            isFinished = true,
            progressMs = 180_000,
            durationMs = 180_000
        )

        scope.advanceUntilIdle()

        assertEquals(1, manager.queueState.value.currentIndex)
    }

    @Test
    fun `stops when last track finishes`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1")))
        scope.advanceUntilIdle()

        meloPlayerState.value = PlaybackState(
            currentTrack = fakeTrack("1"),
            isPlaying = false,
            isBuffering = false,
            isFinished = true,
            progressMs = 180_000,
            durationMs = 180_000
        )

        scope.advanceUntilIdle()

        verify { meloPlayer.stop() }
    }

    @Test
    fun `does not auto-advance when error is present`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()

        meloPlayerState.value = PlaybackState(
            currentTrack = fakeTrack("1"),
            isPlaying = false,
            isBuffering = false,
            progressMs = 179_600,
            durationMs = 180_000,
            error = "Network error"
        )

        scope.advanceUntilIdle()

        assertEquals(0, manager.queueState.value.currentIndex)
    }

    @Test
    fun `prefetch resolves URL for next track`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()

        coVerify(atLeast = 2) { getStreamUseCase(any()) }
    }

    @Test
    fun `release cancels prefetch and releases player`() = runTest {
        val manager = createManager()

        manager.release()

        verify { meloPlayer.release() }
    }

    @Test
    fun `playTrackInQueue jumps to track if already in queue`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2"), fakeTrack("3")))
        scope.advanceUntilIdle()
        manager.playTrackInQueue(fakeTrack("3"))
        scope.advanceUntilIdle()

        assertEquals(2, manager.queueState.value.currentIndex)
    }

    @Test
    fun `playTrackInQueue appends and plays if track not in queue`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()
        manager.playTrackInQueue(fakeTrack("99"))
        scope.advanceUntilIdle()

        assertEquals(3, manager.queueState.value.tracks.size)
        assertEquals("99", manager.queueState.value.tracks[2].id)
    }

    @Test
    fun `auto-caching invokes downloadManager cacheTrack`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val downloadManager = mockk<com.github.adriianh.core.domain.manager.DownloadManager>(relaxed = true)
        coEvery { downloadManager.cacheTrack(any()) } returns true

        val scope = managerScope()
        val manager = PlaybackManagerImpl(
            meloPlayer = meloPlayer,
            getStreamUseCase = getStreamUseCase,
            scope = scope,
            downloadManager = downloadManager
        )

        manager.playTrack(fakeTrack("1"))
        scope.advanceUntilIdle()

        coVerify { downloadManager.cacheTrack(match { it.id == "1" }) }
    }

    @Test
    fun `smart fallback advances to next offline track when online stream fails`() = runTest {
        val offlineRepo = mockk<com.github.adriianh.core.domain.repository.OfflineRepository>(relaxed = true)
        coEvery { getStreamUseCase(match { it.id == "1" }) } returns null
        coEvery { getStreamUseCase(match { it.id == "2" }) } returns "file:///local/2.mp3"
        coEvery { offlineRepo.getOfflineTrack("2") } returns com.github.adriianh.core.domain.model.OfflineTrack(
            track = fakeTrack("2"),
            localFilePath = "/local/2.mp3",
            downloadStatus = com.github.adriianh.core.domain.model.DownloadStatus.COMPLETED
        )

        val scope = managerScope()
        val manager = PlaybackManagerImpl(
            meloPlayer = meloPlayer,
            getStreamUseCase = getStreamUseCase,
            scope = scope,
            offlineRepository = offlineRepo
        )

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()

        assertEquals(1, manager.queueState.value.currentIndex)
        assertEquals("2", manager.queueState.value.currentTrack?.id)
    }
}
