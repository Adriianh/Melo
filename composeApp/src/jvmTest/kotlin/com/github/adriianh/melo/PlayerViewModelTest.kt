package com.github.adriianh.melo

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.player.PlaybackState
import com.github.adriianh.melo.ui.player.PlayerViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerViewModelTest {

    private val playbackManager = mockk<PlaybackManager>(relaxed = true)
    private val vm = PlayerViewModel(playbackManager)

    @Test
    fun `playbackState delegates to manager`() = runTest {
        val expectedState = PlaybackState(
            currentTrack = Track("t1", "Song", "Artist", "Album", 180_000, emptyList(), null, null),
            isPlaying = true
        )
        every { playbackManager.playbackState } returns MutableStateFlow(expectedState)

        val vm = PlayerViewModel(playbackManager)

        assertEquals(expectedState, vm.playbackState.value)
    }

    @Test
    fun `togglePlayPause calls manager`() {
        vm.togglePlayPause()

        verify { playbackManager.togglePlayPause() }
    }

    @Test
    fun `seekTo calls manager with position`() {
        vm.seekTo(45_000L)

        verify { playbackManager.seekTo(45_000L) }
    }

    @Test
    fun `playNext calls manager`() {
        vm.playNext()

        verify { playbackManager.playNext() }
    }

    @Test
    fun `playPrevious calls manager`() {
        vm.playPrevious()

        verify { playbackManager.playPrevious() }
    }
}