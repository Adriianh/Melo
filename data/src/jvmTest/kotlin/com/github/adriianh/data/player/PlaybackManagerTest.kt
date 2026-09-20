package com.github.adriianh.data.player

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.player.PlaybackEvent
import com.github.adriianh.core.domain.player.PlaybackState
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.core.domain.provider.AgeRestrictedException
import com.github.adriianh.core.domain.repository.StreamCacheRepository
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
    fun `addToQueue inserts tracks as play-next after current`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2"), fakeTrack("3")))
        scope.advanceUntilIdle()
        manager.addToQueue(fakeTrack("4"))
        scope.advanceUntilIdle()
        manager.addToQueue(fakeTrack("5"))
        scope.advanceUntilIdle()

        assertEquals(listOf("1", "4", "5", "2", "3"), manager.queueState.value.tracks.map { it.id })
        assertEquals(2, manager.queueState.value.userQueueCount)
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
        assertEquals("99", manager.queueState.value.tracks[1].id)
    }

    @Test
    fun `auto-caching invokes downloadManager cacheTrack`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val downloadManager =
            mockk<com.github.adriianh.core.domain.manager.DownloadManager>(relaxed = true)
        every { downloadManager.activeDownloads } returns MutableStateFlow(emptyMap())
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
        val offlineRepo =
            mockk<com.github.adriianh.core.domain.repository.OfflineRepository>(relaxed = true)
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

    private fun cacheRepo() =
        mockk<StreamCacheRepository>(relaxed = true)

    @Test
    fun `uses cached stream URL and skips network resolution`() = runTest {
        val repo = cacheRepo()
        coEvery { repo.getCachedUrl("1", any()) } returns "http://cached.url"
        val scope = managerScope()
        val manager = PlaybackManagerImpl(
            meloPlayer = meloPlayer,
            getStreamUseCase = getStreamUseCase,
            scope = scope,
            streamCacheRepository = repo
        )

        manager.playTrack(fakeTrack("1"))
        scope.advanceUntilIdle()

        coVerify(exactly = 0) { getStreamUseCase(any()) }
        verify { meloPlayer.load("http://cached.url", any()) }
    }

    @Test
    fun `persists resolved stream URL to stream cache`() = runTest {
        val repo = cacheRepo()
        coEvery { repo.getCachedUrl(any(), any()) } returns null
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = PlaybackManagerImpl(
            meloPlayer = meloPlayer,
            getStreamUseCase = getStreamUseCase,
            scope = scope,
            streamCacheRepository = repo
        )

        manager.playTrack(fakeTrack("1"))
        scope.advanceUntilIdle()

        coVerify { repo.cacheUrl("1", "http://stream.url") }
    }

    @Test
    fun `does not persist file URLs to stream cache`() = runTest {
        val repo = cacheRepo()
        coEvery { repo.getCachedUrl(any(), any()) } returns null
        coEvery { getStreamUseCase(any()) } returns "file:///local/1.mp3"
        val scope = managerScope()
        val manager = PlaybackManagerImpl(
            meloPlayer = meloPlayer,
            getStreamUseCase = getStreamUseCase,
            scope = scope,
            streamCacheRepository = repo
        )

        manager.playTrack(fakeTrack("1"))
        scope.advanceUntilIdle()

        coVerify(exactly = 0) { repo.cacheUrl(any(), any()) }
        verify { meloPlayer.load("file:///local/1.mp3", any()) }
    }

    @Test
    fun `prefetch persists resolved URL to stream cache`() = runTest {
        val repo = cacheRepo()
        coEvery { repo.getCachedUrl(any(), any()) } returns null
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = PlaybackManagerImpl(
            meloPlayer = meloPlayer,
            getStreamUseCase = getStreamUseCase,
            scope = scope,
            streamCacheRepository = repo
        )

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()

        coVerify { repo.cacheUrl("2", "http://stream.url") }
    }

    @Test
    fun `invalidates cached URL when playback error occurs`() = runTest {
        val repo = cacheRepo()
        coEvery { repo.getCachedUrl(any(), any()) } returns null
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = PlaybackManagerImpl(
            meloPlayer = meloPlayer,
            getStreamUseCase = getStreamUseCase,
            scope = scope,
            streamCacheRepository = repo
        )

        manager.setQueue(listOf(fakeTrack("1")))
        scope.advanceUntilIdle()
        coVerify { repo.cacheUrl("1", "http://stream.url") }

        meloPlayerState.value = PlaybackState(
            currentTrack = fakeTrack("1"),
            isPlaying = false,
            isBuffering = false,
            progressMs = 0L,
            durationMs = 180_000,
            error = "Source error"
        )
        scope.advanceUntilIdle()

        coVerify { repo.invalidate("1") }
    }

    private fun collectEvents(
        scope: TestScope,
        manager: PlaybackManagerImpl
    ): Pair<MutableList<PlaybackEvent>, Job> {
        val events = mutableListOf<PlaybackEvent>()
        val job = scope.launch { manager.events.collect { events.add(it) } }
        return events to job
    }

    @Test
    fun `emits TrackStarted event when a track starts loading`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)
        val (events, collector) = collectEvents(scope, manager)

        manager.playTrack(fakeTrack("1"))
        scope.advanceUntilIdle()

        val started = events.filterIsInstance<PlaybackEvent.TrackStarted>()
        assertEquals(1, started.size)
        assertEquals("1", started.first().track.id)
        assertFalse(events.any { it is PlaybackEvent.Error })
        collector.cancel()
    }

    @Test
    fun `retries stream resolution 3 times then skips to next track`() = runTest {
        coEvery { getStreamUseCase(match { it.id == "1" }) } returns null
        coEvery { getStreamUseCase(match { it.id == "2" }) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)
        val (events, collector) = collectEvents(scope, manager)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()

        coVerify(exactly = 3) { getStreamUseCase(match { it.id == "1" }) }
        assertEquals(1, manager.queueState.value.currentIndex)
        assertEquals("2", manager.queueState.value.currentTrack?.id)
        assertTrue(
            events.any {
                it is PlaybackEvent.Error && it.message.contains("Stream not available")
            }
        )
        assertTrue(events.any { it is PlaybackEvent.TrackStarted && it.track.id == "2" })
        collector.cancel()
    }

    @Test
    fun `skips current track when it is the only one and resolution fails`() = runTest {
        coEvery { getStreamUseCase(any()) } returns null
        val scope = managerScope()
        val manager = createManager(scope)
        val (events, collector) = collectEvents(scope, manager)

        manager.setQueue(listOf(fakeTrack("1")))
        scope.advanceUntilIdle()

        coVerify(atLeast = 3) { getStreamUseCase(match { it.id == "1" }) }
        assertTrue(events.any { it is PlaybackEvent.Error })
        verify { meloPlayer.stop() }
        collector.cancel()
    }

    @Test
    fun `age-restricted track skips to next without retrying and emits error`() = runTest {
        coEvery { getStreamUseCase(match { it.id == "1" }) } throws AgeRestrictedException("1")
        coEvery { getStreamUseCase(match { it.id == "2" }) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)
        val (events, collector) = collectEvents(scope, manager)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()

        coVerify(exactly = 1) { getStreamUseCase(match { it.id == "1" }) }
        assertEquals(1, manager.queueState.value.currentIndex)
        assertEquals("2", manager.queueState.value.currentTrack?.id)
        assertTrue(
            events.any {
                it is PlaybackEvent.Error && it.message.contains("age-restricted")
            }
        )
        assertTrue(events.any { it is PlaybackEvent.TrackStarted && it.track.id == "2" })
        collector.cancel()
    }

    @Test
    fun `prefetches radio continuation when approaching end of queue`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val getRadio = mockk<GetRadioUseCase>()
        coEvery { getRadio.invoke(any()) } returns listOf(fakeTrack("radio-1"))

        val scope = managerScope()
        val manager = PlaybackManagerImpl(
            meloPlayer = meloPlayer,
            getStreamUseCase = getStreamUseCase,
            scope = scope,
            getRadioUseCase = getRadio,
        )
        val (events, collector) = collectEvents(scope, manager)

        val tracks = (1..6).map { fakeTrack("$it") }
        manager.setQueue(tracks)
        scope.advanceUntilIdle()

        // 6-track queue: index 0 is not yet within the autoplay margin.
        coVerify(exactly = 0) { getRadio.invoke(any()) }

        manager.playNext()
        scope.advanceUntilIdle()

        coVerify { getRadio.invoke(any()) }
        assertTrue(manager.queueState.value.tracks.any { it.id == "radio-1" })
        assertTrue(events.any { it is PlaybackEvent.TrackStarted })
        collector.cancel()
    }

    @Test
    fun `playNext decrements userQueueCount when advancing through manual tracks`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2"), fakeTrack("3")))
        scope.advanceUntilIdle()
        manager.addToQueue(fakeTrack("A")) // [1, A, 2, 3]
        scope.advanceUntilIdle()
        assertEquals(1, manager.queueState.value.userQueueCount)

        manager.playNext() // → index 1 (A), advancing by 1 consumes one manual track
        scope.advanceUntilIdle()

        assertEquals("A", manager.queueState.value.currentTrack?.id)
        assertEquals(0, manager.queueState.value.userQueueCount)
    }

    @Test
    fun `radio tracks are inserted behind manual queued tracks`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val getRadio = mockk<GetRadioUseCase>()
        coEvery { getRadio.invoke(any()) } returns listOf(fakeTrack("radio-1"))

        val scope = managerScope()
        val manager = PlaybackManagerImpl(
            meloPlayer = meloPlayer,
            getStreamUseCase = getStreamUseCase,
            scope = scope,
            getRadioUseCase = getRadio,
        )

        // Single track schedules the radio autoplay asynchronously...
        manager.setQueue(listOf(fakeTrack("1")))
        // ... so a manual play-next track lands before the radio completes.
        manager.addToQueue(fakeTrack("A"))
        scope.advanceUntilIdle()

        assertEquals(
            listOf("1", "A", "radio-1"),
            manager.queueState.value.tracks.map { it.id }
        )
        assertEquals(1, manager.queueState.value.userQueueCount)
    }

    @Test
    fun `removeFromQueue decrements userQueueCount when removing a manual track`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2"), fakeTrack("3")))
        scope.advanceUntilIdle()
        manager.addToQueue(fakeTrack("A")) // [1, A, 2, 3]
        manager.addToQueue(fakeTrack("B")) // [1, A, B, 2, 3]
        scope.advanceUntilIdle()
        assertEquals(2, manager.queueState.value.userQueueCount)

        manager.removeFromQueue(2) // removes B (manual block = indices 1..2)
        assertEquals(listOf("1", "A", "2", "3"), manager.queueState.value.tracks.map { it.id })
        assertEquals(1, manager.queueState.value.userQueueCount)

        manager.removeFromQueue(1) // removes A
        assertEquals(listOf("1", "2", "3"), manager.queueState.value.tracks.map { it.id })
        assertEquals(0, manager.queueState.value.userQueueCount)

        manager.removeFromQueue(2) // removing a non-manual track leaves count untouched
        assertEquals(0, manager.queueState.value.userQueueCount)
    }

    @Test
    fun `moveQueueItem adjusts userQueueCount for manual block moves`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2"), fakeTrack("3"), fakeTrack("4")))
        scope.advanceUntilIdle()
        manager.addToQueue(fakeTrack("A")) // [1, A, 2, 3, 4], block = index 1
        scope.advanceUntilIdle()

        // Manual track moved out of the block → count decrements
        manager.moveQueueItem(1, 3) // → [1, 2, 3, A, 4]
        assertEquals(0, manager.queueState.value.userQueueCount)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2"), fakeTrack("3"), fakeTrack("4")))
        scope.advanceUntilIdle()
        manager.addToQueue(fakeTrack("A")) // [1, A, 2, 3, 4]
        scope.advanceUntilIdle()

        // Track moved into the block → count increments
        manager.moveQueueItem(3, 1) // → [1, 3, A, 2, 4]
        assertEquals(2, manager.queueState.value.userQueueCount)

        // Move within the block → count unchanged
        manager.moveQueueItem(2, 1) // → [1, A, 3, 2, 4]
        assertEquals(2, manager.queueState.value.userQueueCount)
    }

    @Test
    fun `toggleShuffle resets userQueueCount when enabled`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2"), fakeTrack("3")))
        scope.advanceUntilIdle()
        manager.addToQueue(fakeTrack("A"))
        scope.advanceUntilIdle()
        assertEquals(1, manager.queueState.value.userQueueCount)

        manager.toggleShuffle()

        assertEquals(0, manager.queueState.value.userQueueCount)
        assertTrue(manager.queueState.value.shuffleEnabled)
        assertEquals("1", manager.queueState.value.currentTrack?.id)
    }

    @Test
    fun `setQueue resets userQueueCount`() = runTest {
        coEvery { getStreamUseCase(any()) } returns "http://stream.url"
        val scope = managerScope()
        val manager = createManager(scope)

        manager.setQueue(listOf(fakeTrack("1"), fakeTrack("2")))
        scope.advanceUntilIdle()
        manager.addToQueue(fakeTrack("A"))
        scope.advanceUntilIdle()
        assertEquals(1, manager.queueState.value.userQueueCount)

        manager.setQueue(listOf(fakeTrack("x"), fakeTrack("y")))
        scope.advanceUntilIdle()

        assertEquals(0, manager.queueState.value.userQueueCount)
        assertEquals("x", manager.queueState.value.currentTrack?.id)
    }
}