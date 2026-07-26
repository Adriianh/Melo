package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryRepositoryTest {

    private val db = createInMemoryDatabase()
    private val repo = HistoryRepositoryImpl(db)

    private fun fakeTrack(id: String = "t1") = Track(
        id = id, title = "Song $id", artist = "Artist", album = "Album",
        durationMs = 180_000, genres = emptyList(),
        artworkUrl = null, sourceId = null
    )

    private fun fakeEntry(track: Track = fakeTrack(), playedAt: Long = System.currentTimeMillis()) =
        HistoryEntry(track = track, playedAt = playedAt)

    @Test
    fun `recordPlay adds entry to history`() = runTest {
        repo.recordPlay(fakeEntry(playedAt = 1000L))

        val history = repo.getRecentTracks().first()
        assertEquals(1, history.size)
        assertEquals("t1", history[0].track.id)
    }

    @Test
    fun `getRecentTracks returns most recent first`() = runTest {
        repo.recordPlay(fakeEntry(fakeTrack("t1"), playedAt = 1000L))
        repo.recordPlay(fakeEntry(fakeTrack("t2"), playedAt = 2000L))

        val history = repo.getRecentTracks().first()
        assertEquals(2, history.size)
        assertEquals("t2", history[0].track.id, "Most recent should be first")
        assertEquals("t1", history[1].track.id)
    }

    @Test
    fun `getRecentTracks respects limit`() = runTest {
        repo.recordPlay(fakeEntry(fakeTrack("t1"), playedAt = 1000L))
        repo.recordPlay(fakeEntry(fakeTrack("t2"), playedAt = 2000L))
        repo.recordPlay(fakeEntry(fakeTrack("t3"), playedAt = 3000L))

        val history = repo.getRecentTracks(limit = 2).first()
        assertEquals(2, history.size)
    }

    @Test
    fun `pruneOldHistory keeps only 5000 most recent`() = runTest {
        // Insert 3 entries, prune should keep latest 5000
        repo.recordPlay(fakeEntry(fakeTrack("t1"), playedAt = 1000L))
        repo.recordPlay(fakeEntry(fakeTrack("t2"), playedAt = 2000L))
        repo.recordPlay(fakeEntry(fakeTrack("t3"), playedAt = 3000L))

        repo.pruneOldHistory()

        val history = repo.getRecentTracks().first()
        assertEquals(3, history.size, "All 3 should survive (under 5000 limit)")
    }

    @Test
    fun `history entry maps track fields correctly`() = runTest {
        val track = Track(
            id = "t1", title = "My Song", artist = "My Artist",
            album = "My Album", durationMs = 240_000, genres = emptyList(),
            artworkUrl = "http://art.jpg", sourceId = "src123"
        )
        repo.recordPlay(fakeEntry(track, playedAt = 5000L))

        val history = repo.getRecentTracks().first()
        val entry = history[0]
        assertEquals("My Song", entry.track.title)
        assertEquals("My Artist", entry.track.artist)
        assertEquals("My Album", entry.track.album)
        assertEquals(240_000L, entry.track.durationMs)
        assertEquals("http://art.jpg", entry.track.artworkUrl)
        assertEquals("src123", entry.track.sourceId)
        assertEquals(5000L, entry.playedAt)
    }
}