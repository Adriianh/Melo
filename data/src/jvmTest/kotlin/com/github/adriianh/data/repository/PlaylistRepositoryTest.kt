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

    private fun fakeTrack(id: String = "t1") = Track(
        id = id, title = "Song $id", artist = "Artist", album = "Album",
        durationMs = 180_000, genres = emptyList(),
        artworkUrl = null, sourceId = null
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
}