package com.github.adriianh.melo

import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.network.NetworkMonitor
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.player.QueueState
import com.github.adriianh.core.domain.usecase.offline.GetOfflineSuggestionsUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
import com.github.adriianh.core.domain.usecase.search.GetRelatedTracksUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.melo.ui.player.QueueViewModel
import io.mockk.coEvery
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
class QueueViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val queueStateFlow = MutableStateFlow(QueueState())
    private val playbackManager =
        mockk<PlaybackManager>(relaxed = true) {
            every { queueState } returns queueStateFlow
        }
    private val getRelatedTracksUseCase = mockk<GetRelatedTracksUseCase>(relaxed = true)
    private val getRadioUseCase = mockk<GetRadioUseCase>(relaxed = true)
    private val getOfflineSuggestionsUseCase = mockk<GetOfflineSuggestionsUseCase>(relaxed = true)
    private val getSettingsUseCase = mockk<GetSettingsUseCase>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>(relaxed = true)

    private val settingsFlow = MutableStateFlow(Settings())
    private val isOnlineFlow = MutableStateFlow(true)

    private val sampleTrack =
        Track("t1", "Song", "Artist", "Album", 180_000, emptyList(), null, null)
    private val offlineTrack =
        Track("off1", "Offline Song", "Artist", "Album", 180_000, emptyList(), null, null)

    private fun createViewModel(): QueueViewModel =
        QueueViewModel(
            manager = playbackManager,
            getRadioUseCase = getRadioUseCase,
            getRelatedTracksUseCase = getRelatedTracksUseCase,
            getOfflineSuggestionsUseCase = getOfflineSuggestionsUseCase,
            getSettingsUseCase = getSettingsUseCase,
            networkMonitor = networkMonitor,
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getSettingsUseCase() } returns settingsFlow
        every { networkMonitor.isOnline } returns isOnlineFlow
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `queueState delegates to manager`() =
        runTest(testDispatcher) {
            val track = Track("t1", "Song", "Artist", "Album", 180_000, emptyList(), null, null)
            val expectedState = QueueState(tracks = listOf(track), currentIndex = 0)
            every { playbackManager.queueState } returns MutableStateFlow(expectedState)

            val customVm = QueueViewModel(playbackManager)

            assertEquals(expectedState, customVm.queueState.value)
        }

    @Test
    fun `playTrack calls manager`() {
        val vm = createViewModel()
        vm.playTrack(sampleTrack)
        verify { playbackManager.playTrack(sampleTrack) }
    }

    @Test
    fun `addToQueue calls manager`() {
        val vm = createViewModel()
        vm.addToQueue(sampleTrack)
        verify { playbackManager.addToQueue(sampleTrack) }
    }

    @Test
    fun `playTrackInQueue calls manager`() {
        val vm = createViewModel()
        vm.playTrackInQueue(sampleTrack)
        verify { playbackManager.playTrackInQueue(sampleTrack) }
    }

    @Test
    fun `toggleShuffle calls manager`() {
        val vm = createViewModel()
        vm.toggleShuffle()
        verify { playbackManager.toggleShuffle() }
    }

    @Test
    fun `toggleRepeat calls manager`() {
        val vm = createViewModel()
        vm.toggleRepeat()
        verify { playbackManager.toggleRepeat() }
    }

    @Test
    fun `when offlineMode is active, suggestions use offline usecase and skip network`() =
        runTest(testDispatcher) {
            settingsFlow.value = Settings(offlineMode = true)
            coEvery {
                getOfflineSuggestionsUseCase.invoke(sampleTrack, any(), any())
            } returns listOf(offlineTrack)

            val vm = createViewModel()
            queueStateFlow.value = QueueState(tracks = listOf(sampleTrack), currentIndex = 0)
            advanceUntilIdle()

            coVerify(exactly = 0) { getRelatedTracksUseCase.invoke(any()) }
            coVerify(exactly = 0) { getRadioUseCase.invoke(any()) }
            coVerify(exactly = 1) { getOfflineSuggestionsUseCase.invoke(sampleTrack, any(), 12) }
            assertEquals(listOf(offlineTrack), vm.suggestions.value)
        }

    @Test
    fun `when network is offline, suggestions use offline usecase and skip network`() =
        runTest(testDispatcher) {
            isOnlineFlow.value = false
            coEvery {
                getOfflineSuggestionsUseCase.invoke(sampleTrack, any(), any())
            } returns listOf(offlineTrack)

            val vm = createViewModel()
            queueStateFlow.value = QueueState(tracks = listOf(sampleTrack), currentIndex = 0)
            advanceUntilIdle()

            coVerify(exactly = 0) { getRelatedTracksUseCase.invoke(any()) }
            coVerify(exactly = 0) { getRadioUseCase.invoke(any()) }
            coVerify(exactly = 1) { getOfflineSuggestionsUseCase.invoke(sampleTrack, any(), 12) }
            assertEquals(listOf(offlineTrack), vm.suggestions.value)
        }

    @Test
    fun `when online calls throw, suggestions fall back to offline usecase`() =
        runTest(testDispatcher) {
            coEvery { getRelatedTracksUseCase.invoke(any()) } throws RuntimeException("Network error")
            coEvery { getRadioUseCase.invoke(any()) } throws RuntimeException("Network error")
            coEvery {
                getOfflineSuggestionsUseCase.invoke(sampleTrack, any(), any())
            } returns listOf(offlineTrack)

            val vm = createViewModel()
            queueStateFlow.value = QueueState(tracks = listOf(sampleTrack), currentIndex = 0)
            advanceUntilIdle()

            assertEquals(listOf(offlineTrack), vm.suggestions.value)
        }

    @Test
    fun `startRadio when effectively offline enqueues offline suggestions`() =
        runTest(testDispatcher) {
            settingsFlow.value = Settings(offlineMode = true)
            coEvery {
                getOfflineSuggestionsUseCase.invoke(sampleTrack, setOf(sampleTrack.id), 25)
            } returns listOf(offlineTrack)

            val vm = createViewModel()
            vm.startRadio(sampleTrack)
            advanceUntilIdle()

            verify { playbackManager.setQueue(listOf(sampleTrack, offlineTrack), 0) }
            coVerify(exactly = 0) { getRadioUseCase.invoke(any()) }
        }
}
