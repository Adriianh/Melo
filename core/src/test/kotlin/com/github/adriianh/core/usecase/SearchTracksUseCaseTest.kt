package com.github.adriianh.core.usecase

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.MusicRepository
import com.github.adriianh.core.domain.usecase.SearchTracksUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchTracksUseCaseTest {

    private val repository = mockk<MusicRepository>()
    private val searchTracks = SearchTracksUseCase(repository)

    private val fakeTrack = Track(
        id = "1",
        title = "Not Like Us",
        artist = "Kendrick Lamar",
        album = "Not Like Us",
        durationMs = 274000,
        genres = listOf("hip-hop"),
        artworkUrl = null,
        sourceId = null
    )

    @Test
    fun `returns empty list when query is blank`() = runTest {
        val result = searchTracks("")
        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { repository.search(any()) }
    }

    @Test
    fun `returns empty list when query is only whitespace`() = runTest {
        val result = searchTracks("   ")
        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { repository.search(any()) }
    }

    @Test
    fun `calls repository with correct query`() = runTest {
        coEvery { repository.search("Kendrick") } returns listOf(fakeTrack)
        val result = searchTracks("Kendrick")
        assertEquals(1, result.size)
        assertEquals("Not Like Us", result.first().title)
        coVerify(exactly = 1) { repository.search("Kendrick") }
    }

    @Test
    fun `returns all results from repository`() = runTest {
        val tracks = listOf(fakeTrack, fakeTrack.copy(id = "2", title = "HUMBLE."))
        coEvery { repository.search("Kendrick") } returns tracks
        val result = searchTracks("Kendrick")
        assertEquals(2, result.size)
    }
}