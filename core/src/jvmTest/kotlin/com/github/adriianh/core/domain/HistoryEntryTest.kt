package com.github.adriianh.core.domain

import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.mergeHistoryEntries
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryEntryTest {

    private fun entry(
        id: String,
        title: String,
        playedAt: Long,
        sourceId: String? = null
    ): HistoryEntry {
        return HistoryEntry(
            track = Track(
                id = id,
                title = title,
                artist = "Test Artist",
                album = "Test Album",
                durationMs = 180_000L,
                genres = emptyList(),
                artworkUrl = null,
                sourceId = sourceId,
            ),
            playedAt = playedAt,
        )
    }

    @Test
    fun `returns remote when local is empty`() {
        val remote = listOf(entry("r1", "Remote 1", 1000L))
        val merged = mergeHistoryEntries(emptyList(), remote)
        assertEquals(remote, merged)
    }

    @Test
    fun `returns local when remote is empty`() {
        val local = listOf(entry("l1", "Local 1", 2000L))
        val merged = mergeHistoryEntries(local, emptyList())
        assertEquals(local, merged)
    }

    @Test
    fun `deduplicates exact id and piped prefix`() {
        val local = listOf(
            entry("piped:song1", "Song 1", 5000L),
            entry("song2", "Song 2", 4000L),
        )
        val remote = listOf(
            entry("song1", "Song 1 (Remote)", 3000L),
            entry("piped:song2", "Song 2 (Remote)", 2000L),
            entry("song3", "Song 3", 1000L),
        )

        val merged = mergeHistoryEntries(local, remote)

        assertEquals(3, merged.size)
        assertEquals("piped:song1", merged[0].track.id)
        assertEquals("song2", merged[1].track.id)
        assertEquals("song3", merged[2].track.id)
    }

    @Test
    fun `deduplicates by sourceId`() {
        val local = listOf(
            entry("local_file_id", "Song Local", 5000L, sourceId = "yt_123"),
        )
        val remote = listOf(
            entry("yt_123", "Song Remote", 3000L),
            entry("yt_456", "Song Another", 2000L),
        )

        val merged = mergeHistoryEntries(local, remote)

        assertEquals(2, merged.size)
        assertEquals("local_file_id", merged[0].track.id)
        assertEquals("yt_456", merged[1].track.id)
    }
}
