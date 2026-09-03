package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaylistRepositoryTest {

    private val db = createInMemoryDatabase()
    private val repo = PlaylistRepositoryImpl(db)

    private fun fakeTrack(id: String = "t1", artworkUrl: String? = null) = Track(
        id = id, title = "Song $id", artist = "Artist", album = "Album",
        durationMs = 180_000, genres = emptyList(),
        artworkUrl = artworkUrl, sourceId = null
    )

    @Test
    fun `createPlaylist returns valid id`() = runTest {
        val id = repo.createPlaylist("My Playlist")

        assertTrue(id > 0, "Returned id should be positive")
    }

    @Test
    fun `getPlaylists returns created playlists`() = runTest {
        repo.createPlaylist("Playlist A")
        repo.createPlaylist("Playlist B")

        val playlists = repo.getPlaylists().first()
        assertEquals(2, playlists.size)
    }

    @Test
    fun `getPlaylists ordered by created_at descending`() = runTest {
        repo.createPlaylist("First")
        Thread.sleep(1100) // currentTimeSeconds has 1s resolution
        repo.createPlaylist("Second")

        val playlists = repo.getPlaylists().first()
        assertEquals("Second", playlists[0].name, "Most recent should be first")
        assertEquals("First", playlists[1].name)
    }

    @Test
    fun `renamePlaylist updates name`() = runTest {
        val id = repo.createPlaylist("Old Name")

        repo.renamePlaylist(id, "New Name")

        val playlists = repo.getPlaylists().first()
        assertEquals("New Name", playlists[0].name)
    }

    @Test
    fun `deletePlaylist removes playlist`() = runTest {
        val id = repo.createPlaylist("To Delete")

        repo.deletePlaylist(id)

        val playlists = repo.getPlaylists().first()
        assertTrue(playlists.isEmpty())
    }

    @Test
    fun `addTrackToPlaylist adds track`() = runTest {
        val playlistId = repo.createPlaylist("My Playlist")

        repo.addTrackToPlaylist(playlistId, fakeTrack("t1"))

        val tracks = repo.getPlaylistTracks(playlistId).first()
        assertEquals(1, tracks.size)
        assertEquals("t1", tracks[0].id)
    }

    @Test
    fun `removeTrackFromPlaylist removes track`() = runTest {
        val playlistId = repo.createPlaylist("My Playlist")
        repo.addTrackToPlaylist(playlistId, fakeTrack("t1"))
        repo.addTrackToPlaylist(playlistId, fakeTrack("t2"))

        repo.removeTrackFromPlaylist(playlistId, "t1")

        val tracks = repo.getPlaylistTracks(playlistId).first()
        assertEquals(1, tracks.size)
        assertEquals("t2", tracks[0].id)
    }

    @Test
    fun `trackCount reflects actual tracks in playlist`() = runTest {
        val playlistId = repo.createPlaylist("My Playlist")
        repo.addTrackToPlaylist(playlistId, fakeTrack("t1"))
        repo.addTrackToPlaylist(playlistId, fakeTrack("t2"))
        repo.addTrackToPlaylist(playlistId, fakeTrack("t3"))

        val playlists = repo.getPlaylists().first()
        assertEquals(3, playlists[0].trackCount)
    }

    @Test
    fun `deletePlaylist also removes associated tracks`() = runTest {
        val playlistId = repo.createPlaylist("My Playlist")
        repo.addTrackToPlaylist(playlistId, fakeTrack("t1"))
        repo.addTrackToPlaylist(playlistId, fakeTrack("t2"))

        repo.deletePlaylist(playlistId)

        val tracks = repo.getPlaylistTracks(playlistId).first()
        assertTrue(tracks.isEmpty())
    }

    @Test
    fun `playlist tracks are ordered by position`() = runTest {
        val playlistId = repo.createPlaylist("My Playlist")
        repo.addTrackToPlaylist(playlistId, fakeTrack("t3"))
        repo.addTrackToPlaylist(playlistId, fakeTrack("t1"))
        repo.addTrackToPlaylist(playlistId, fakeTrack("t2"))

        val tracks = repo.getPlaylistTracks(playlistId).first()
        assertEquals("t3", tracks[0].id, "First added should be first (position 0)")
        assertEquals("t1", tracks[1].id)
        assertEquals("t2", tracks[2].id)
    }

    @Test
    fun `getPlaylistIdsForTrack returns correct set of playlist ids`() = runTest {
        val p1 = repo.createPlaylist("Playlist 1")
        val p2 = repo.createPlaylist("Playlist 2")
        val p3 = repo.createPlaylist("Playlist 3")

        repo.addTrackToPlaylist(p1, fakeTrack("t1"))
        repo.addTrackToPlaylist(p3, fakeTrack("t1"))

        val playlistIds = repo.getPlaylistIdsForTrack("t1").first()
        assertEquals(setOf(p1, p3), playlistIds)
    }

    @Test
    fun `reorderPlaylistTracks updates track positions in playlist`() = runTest {
        val playlistId = repo.createPlaylist("Reorder Test")
        repo.addTrackToPlaylist(playlistId, fakeTrack("t1"))
        repo.addTrackToPlaylist(playlistId, fakeTrack("t2"))
        repo.addTrackToPlaylist(playlistId, fakeTrack("t3"))

        repo.reorderPlaylistTracks(playlistId, listOf("t3", "t1", "t2"))

        val reorderedTracks = repo.getPlaylistTracks(playlistId).first()
        assertEquals("t3", reorderedTracks[0].id)
        assertEquals("t1", reorderedTracks[1].id)
        assertEquals("t2", reorderedTracks[2].id)
    }

    @Test
    fun `getPlaylists parses up to 4 distinct artworks`() = runTest {
        val playlistId = repo.createPlaylist("Artworks Test")
        repo.addTrackToPlaylist(playlistId, fakeTrack("t1", "art1"))
        repo.addTrackToPlaylist(playlistId, fakeTrack("t2", "art2"))
        repo.addTrackToPlaylist(playlistId, fakeTrack("t3", "art3"))
        repo.addTrackToPlaylist(playlistId, fakeTrack("t4", "art4"))
        repo.addTrackToPlaylist(playlistId, fakeTrack("t5", "art5"))

        val playlists = repo.getPlaylists().first()
        val pl = playlists.first { it.id == playlistId }
        assertEquals(4, pl.artworks.size)
        assertEquals(listOf("art1", "art2", "art3", "art4"), pl.artworks)
    }

    @Test
    fun `addTracksToPlaylist inserts batch and skips duplicates when enabled`() = runTest {
        val playlistId = repo.createPlaylist("Batch Test")
        repo.addTrackToPlaylist(playlistId, fakeTrack("t1"))

        val count = repo.addTracksToPlaylist(
            playlistId = playlistId,
            tracks = listOf(fakeTrack("t1"), fakeTrack("t2"), fakeTrack("t3")),
            skipDuplicates = true
        )

        assertEquals(2, count, "Only 2 new tracks should be added")
        val tracks = repo.getPlaylistTracks(playlistId).first()
        assertEquals(3, tracks.size)
        assertEquals(listOf("t1", "t2", "t3"), tracks.map { it.id })
    }
}