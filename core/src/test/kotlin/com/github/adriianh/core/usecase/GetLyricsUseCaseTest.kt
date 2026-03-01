package com.github.adriianh.core.usecase

import com.github.adriianh.core.domain.repository.LyricsRepository
import com.github.adriianh.core.domain.usecase.GetLyricsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetLyricsUseCaseTest {

    private val repository = mockk<LyricsRepository>()
    private val getLyrics = GetLyricsUseCase(repository)

    @Test
    fun `returns null if artist is blank`() = runTest {
        val result = getLyrics("", "Some Title")
        assertNull(result)
        coVerify(exactly = 0) { repository.getLyrics(any(), any()) }
    }

    @Test
    fun `returns null if title is blank`() = runTest {
        val result = getLyrics("Some Artist", "   ")
        assertNull(result)
        coVerify(exactly = 0) { repository.getLyrics(any(), any()) }
    }

    @Test
    fun `calls repository with correct parameters`() = runTest {
        coEvery { repository.getLyrics("Some Artist", "Some Title") } returns "Some lyrics"
        val result = getLyrics("Some Artist", "Some Title")
        assertEquals("Some lyrics", result)
        coVerify(exactly = 1) { repository.getLyrics("Some Artist", "Some Title") }
    }

    @Test
    fun `returns lyrics if repository finds it`() = runTest {
        coEvery { repository.getLyrics("Artist", "Title") } returns "These are the lyrics"
        val result = getLyrics("Artist", "Title")
        assertEquals("These are the lyrics", result)
    }

    @Test
    fun `returns null if repository returns null`() = runTest {
        coEvery { repository.getLyrics("Artist", "Title") } returns null
        val result = getLyrics("Artist", "Title")
        assertNull(result)
    }
}