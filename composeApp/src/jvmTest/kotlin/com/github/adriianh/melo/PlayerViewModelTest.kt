package com.github.adriianh.melo

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.player.PlaybackState
import com.github.adriianh.core.domain.player.QueueState
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.AccentColorExtractor
import io.ktor.client.HttpClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val playbackManager = mockk<PlaybackManager>(relaxed = true)
    private val playbackFlow = MutableStateFlow(PlaybackState())
    private val queueFlow = MutableStateFlow(QueueState())
    private val httpClient = mockk<HttpClient>(relaxed = true)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { playbackManager.playbackState } returns playbackFlow
        every { playbackManager.queueState } returns queueFlow
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm() = PlayerViewModel(playbackManager, httpClient)

    @Test
    fun `playbackState delegates to manager`() = runTest {
        val track = Track("t1", "Song", "Artist", "Album", 180_000, emptyList(), null, null)
        playbackFlow.value = PlaybackState(currentTrack = track, isPlaying = true)

        val vm = createVm()

        assertEquals(track, vm.playbackState.value.currentTrack)
        assertTrue(vm.playbackState.value.isPlaying)
    }

    @Test
    fun `uiState maps playback correctly`() = runTest {
        val track =
            Track("t1", "Song Title", "Artist Name", "Album", 240_000, emptyList(), null, null)
        playbackFlow.value = PlaybackState(
            currentTrack = track,
            isPlaying = true,
            progressMs = 60_000,
            durationMs = 240_000
        )

        val vm = createVm()
        advanceUntilIdle()

        val ui = vm.uiState.value
        assertEquals("Song Title", ui.title)
        assertEquals("Artist Name", ui.artist)
        assertTrue(ui.isPlaying)
        assertTrue(ui.elapsedLabel.contains(":"))
        assertTrue(ui.remainingLabel.contains(":"))
    }

    @Test
    fun `uiState computes progress fraction`() = runTest {
        playbackFlow.value = PlaybackState(
            currentTrack = Track("t1", "S", "A", "B", 200_000, emptyList(), null, null),
            progressMs = 100_000,
            durationMs = 200_000
        )

        val vm = createVm()
        advanceUntilIdle()

        assertEquals(0.5f, vm.uiState.value.progressFraction, 0.01f)
    }

    @Test
    fun `uiState has empty when no track`() = runTest {
        val vm = createVm()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.hasTrack)
        assertEquals("", vm.uiState.value.title)
    }

    @Test
    fun `uiState uses fallback accent when no artwork`() = runTest {
        playbackFlow.value = PlaybackState(
            currentTrack = Track("t1", "S", "A", "B", 200_000, emptyList(), null, null)
        )

        val vm = createVm()
        advanceUntilIdle()

        assertEquals(AccentColorExtractor.fallback.dominant, vm.uiState.value.accentColor)
    }

    @Test
    fun `uiState reflects queue shuffle and repeat`() = runTest {
        queueFlow.value = QueueState(
            tracks = listOf(mockk()),
            currentIndex = 0,
            shuffleEnabled = true,
            repeatMode = RepeatMode.ALL
        )

        val vm = createVm()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.shuffleEnabled)
        assertEquals(RepeatMode.ALL, vm.uiState.value.repeatMode)
    }

    @Test
    fun `togglePlayPause calls manager`() {
        createVm().togglePlayPause()
        verify { playbackManager.togglePlayPause() }
    }

    @Test
    fun `seekTo calls manager with position`() {
        createVm().seekTo(45_000L)
        verify { playbackManager.seekTo(45_000L) }
    }

    @Test
    fun `playNext calls manager`() {
        createVm().playNext()
        verify { playbackManager.playNext() }
    }

    @Test
    fun `playPrevious calls manager`() {
        createVm().playPrevious()
        verify { playbackManager.playPrevious() }
    }

    @Test
    fun `toggleShuffle calls manager`() {
        createVm().toggleShuffle()
        verify { playbackManager.toggleShuffle() }
    }

    @Test
    fun `toggleRepeat calls manager`() {
        createVm().toggleRepeat()
        verify { playbackManager.toggleRepeat() }
    }
}
