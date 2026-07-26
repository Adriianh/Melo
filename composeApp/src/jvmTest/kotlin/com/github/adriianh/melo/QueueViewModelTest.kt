package com.github.adriianh.melo

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.player.QueueState
import com.github.adriianh.melo.ui.player.QueueViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class QueueViewModelTest {

    private val playbackManager = mockk<PlaybackManager>(relaxed = true)
    private val vm = QueueViewModel(playbackManager)

    @Test
    fun `queueState delegates to manager`() = runTest {
        val track = Track("t1", "Song", "Artist", "Album", 180_000, emptyList(), null, null)
        val expectedState = QueueState(tracks = listOf(track), currentIndex = 0)
        every { playbackManager.queueState } returns MutableStateFlow(expectedState)

        val vm = QueueViewModel(playbackManager)

        assertEquals(expectedState, vm.queueState.value)
    }

    @Test
    fun `playTrack calls manager`() {
        val track = Track("t1", "Song", "Artist", "Album", 180_000, emptyList(), null, null)

        vm.playTrack(track)

        verify { playbackManager.playTrack(track) }
    }

    @Test
    fun `addToQueue calls manager`() {
        val track = Track("t1", "Song", "Artist", "Album", 180_000, emptyList(), null, null)

        vm.addToQueue(track)

        verify { playbackManager.addToQueue(track) }
    }

    @Test
    fun `playTrackInQueue calls manager`() {
        val track = Track("t1", "Song", "Artist", "Album", 180_000, emptyList(), null, null)

        vm.playTrackInQueue(track)

        verify { playbackManager.playTrackInQueue(track) }
    }

    @Test
    fun `toggleShuffle calls manager`() {
        vm.toggleShuffle()

        verify { playbackManager.toggleShuffle() }
    }

    @Test
    fun `toggleRepeat calls manager`() {
        vm.toggleRepeat()

        verify { playbackManager.toggleRepeat() }
    }
}