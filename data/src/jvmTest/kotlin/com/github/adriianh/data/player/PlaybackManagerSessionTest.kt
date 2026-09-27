package com.github.adriianh.data.player

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.player.PlaybackState
import com.github.adriianh.core.domain.repository.SavedSession
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.session.RestoreSessionUseCase
import io.mockk.coEvery
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

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackManagerSessionTest {
    private val meloPlayerState = MutableStateFlow(PlaybackState())
    private val meloPlayer =
        mockk<MeloPlayer>(relaxed = true) {
            every { state } returns meloPlayerState
        }
    private val getStreamUseCase = mockk<GetStreamUseCase>()

    private fun managerScope() = TestScope(StandardTestDispatcher())

    private fun fakeTrack(
        id: String,
        title: String = "Track $id",
    ) = Track(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        durationMs = 180_000,
        genres = emptyList(),
        artworkUrl = null,
        sourceId = null,
    )

    @Test
    fun `session restoration sets idle track at restored position`() =
        runTest {
            val restoreSession = mockk<RestoreSessionUseCase>()
            coEvery { restoreSession.invoke() } returns
                SavedSession(
                    queue = listOf(fakeTrack("1"), fakeTrack("2")),
                    queueIndex = 0,
                    positionMs = 45_000L,
                )

            val scope = managerScope()
            val manager =
                PlaybackManagerImpl(
                    meloPlayer = meloPlayer,
                    getStreamUseCase = getStreamUseCase,
                    scope = scope,
                    restoreSessionUseCase = restoreSession,
                )
            scope.advanceUntilIdle()

            assertEquals(
                "1",
                manager.queueState.value.currentTrack
                    ?.id,
            )
            verify { meloPlayer.setIdleTrack(fakeTrack("1"), 45_000L) }
        }

    @Test
    fun `togglePlayPause resumes restored track from pending position`() =
        runTest {
            coEvery { getStreamUseCase(any()) } returns "http://stream.url"
            val restoreSession = mockk<RestoreSessionUseCase>()
            coEvery { restoreSession.invoke() } returns
                SavedSession(
                    queue = listOf(fakeTrack("1"), fakeTrack("2")),
                    queueIndex = 0,
                    positionMs = 45_000L,
                )

            val scope = managerScope()
            val manager =
                PlaybackManagerImpl(
                    meloPlayer = meloPlayer,
                    getStreamUseCase = getStreamUseCase,
                    scope = scope,
                    restoreSessionUseCase = restoreSession,
                )
            scope.advanceUntilIdle()

            manager.togglePlayPause()
            scope.advanceUntilIdle()

            verify { meloPlayer.load("http://stream.url", fakeTrack("1"), 45_000L) }
        }

    @Test
    fun `setQueue resets pending position and starts new track from zero`() =
        runTest {
            coEvery { getStreamUseCase(any()) } returns "http://stream.url"
            val restoreSession = mockk<RestoreSessionUseCase>()
            coEvery { restoreSession.invoke() } returns
                SavedSession(
                    queue = listOf(fakeTrack("1"), fakeTrack("2")),
                    queueIndex = 0,
                    positionMs = 45_000L,
                )

            val scope = managerScope()
            val manager =
                PlaybackManagerImpl(
                    meloPlayer = meloPlayer,
                    getStreamUseCase = getStreamUseCase,
                    scope = scope,
                    restoreSessionUseCase = restoreSession,
                )
            scope.advanceUntilIdle()

            manager.setQueue(listOf(fakeTrack("new1")), 0)
            scope.advanceUntilIdle()

            verify { meloPlayer.load("http://stream.url", fakeTrack("new1"), 0L) }
        }

    @Test
    fun `playTrackInQueue with different track resets pending position and starts from zero`() =
        runTest {
            coEvery { getStreamUseCase(any()) } returns "http://stream.url"
            val restoreSession = mockk<RestoreSessionUseCase>()
            coEvery { restoreSession.invoke() } returns
                SavedSession(
                    queue = listOf(fakeTrack("1"), fakeTrack("2")),
                    queueIndex = 0,
                    positionMs = 45_000L,
                )

            val scope = managerScope()
            val manager =
                PlaybackManagerImpl(
                    meloPlayer = meloPlayer,
                    getStreamUseCase = getStreamUseCase,
                    scope = scope,
                    restoreSessionUseCase = restoreSession,
                )
            scope.advanceUntilIdle()

            manager.playTrackInQueue(fakeTrack("2"))
            scope.advanceUntilIdle()

            verify { meloPlayer.load("http://stream.url", fakeTrack("2"), 0L) }
        }

    @Test
    fun `playNext resets pending position and starts from zero`() =
        runTest {
            coEvery { getStreamUseCase(any()) } returns "http://stream.url"
            val restoreSession = mockk<RestoreSessionUseCase>()
            coEvery { restoreSession.invoke() } returns
                SavedSession(
                    queue = listOf(fakeTrack("1"), fakeTrack("2")),
                    queueIndex = 0,
                    positionMs = 45_000L,
                )

            val scope = managerScope()
            val manager =
                PlaybackManagerImpl(
                    meloPlayer = meloPlayer,
                    getStreamUseCase = getStreamUseCase,
                    scope = scope,
                    restoreSessionUseCase = restoreSession,
                )
            scope.advanceUntilIdle()

            manager.playNext()
            scope.advanceUntilIdle()

            verify { meloPlayer.load("http://stream.url", fakeTrack("2"), 0L) }
        }
}
