package com.github.adriianh.core.util

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfflineSearchExtractorTest {
    private fun sampleTrack(
        id: String,
        title: String,
        artist: String,
        album: String = "Test Album",
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 180000L,
        genres = emptyList(),
        artworkUrl = "https://example.com/$id.jpg",
        sourceId = null,
    )

    private fun sampleOffline(track: Track) =
        OfflineTrack(
            track = track,
            localFilePath = "/tmp/${track.id}.mp3",
            downloadStatus = DownloadStatus.COMPLETED,
        )

    @Test
    fun testFuzzyMatchMatchesSubsequence() {
        assertTrue(fuzzyMatch("radio", "Radiohead".lowercase()))
        assertTrue(fuzzyMatch("rdhd", "radiohead"))
        assertFalse(fuzzyMatch("xyz", "radiohead"))
    }

    @Test
    fun testExtractOfflineSongsMatchesTitleArtistAndAlbum() {
        val tracks =
            listOf(
                sampleTrack("1", "Paranoid Android", "Radiohead", "OK Computer"),
                sampleTrack("2", "Karma Police", "Radiohead", "OK Computer"),
                sampleTrack("3", "Get Lucky", "Daft Punk", "Random Access Memories"),
            )

        val byArtist = extractOfflineSongs(tracks, "radio")
        assertEquals(2, byArtist.size)

        val byTitle = extractOfflineSongs(tracks, "lucky")
        assertEquals(1, byTitle.size)
        assertEquals("Get Lucky", byTitle.first().title)

        val byAlbum = extractOfflineSongs(tracks, "computer")
        assertEquals(2, byAlbum.size)

        val emptyResult = extractOfflineSongs(tracks, "coldplay")
        assertTrue(emptyResult.isEmpty())

        val blankQuery = extractOfflineSongs(tracks, "   ")
        assertTrue(blankQuery.isEmpty())
    }

    @Test
    fun testExtractOfflineAlbumsGroupsTracksAndMatchesQuery() {
        val tracks =
            listOf(
                sampleTrack("1", "Paranoid Android", "Radiohead", "OK Computer"),
                sampleTrack("2", "Karma Police", "Radiohead", "OK Computer"),
                sampleTrack("3", "Airbag", "Radiohead", "OK Computer"),
                sampleTrack("4", "Idioteque", "Radiohead", "Kid A"),
            )

        val albums = extractOfflineAlbums(tracks, "computer")
        assertEquals(1, albums.size)
        val okComputer = albums.first()
        assertEquals("OK Computer", okComputer.title)
        assertEquals(3, okComputer.songs?.size)

        val allRadioheadAlbums = extractOfflineAlbums(tracks, "radiohead")
        assertEquals(2, allRadioheadAlbums.size)
    }

    @Test
    fun testExtractOfflineArtistsGroupsTracksAndBuildsSections() {
        val tracks =
            listOf(
                sampleTrack("1", "Paranoid Android", "Radiohead", "OK Computer"),
                sampleTrack("2", "Idioteque", "Radiohead", "Kid A"),
                sampleTrack("3", "Around the World", "Daft Punk", "Homework"),
            )

        val artists = extractOfflineArtists(tracks, "radio")
        assertEquals(1, artists.size)
        val radiohead = artists.first()
        assertEquals("Radiohead", radiohead.name)
        assertEquals(2, radiohead.topSongs?.size)
        val firstSection = radiohead.sections.first()
        assertEquals("Álbumes en tus descargas", firstSection.title)
        assertEquals(2, firstSection.items.size)
    }

    @Test
    fun testExtractOfflinePlaylistsCombinesLocalAndAvailableRemotePlaylists() {
        val localPlaylist =
            Playlist(
                id = 1L,
                name = "Rock Classics",
                createdAt = 1704067200000L,
                trackCount = 2,
                artworks = emptyList(),
            )

        val offlineTrack =
            sampleOffline(sampleTrack("t1", "Paranoid Android", "Radiohead", "OK Computer"))

        val remoteAvailable =
            SearchResult.Playlist(
                id = "remote-1",
                title = "Radiohead Essentials",
                author = "Spotify",
                trackCount = 1,
                artworkUrl = null,
                songs = listOf(offlineTrack.track),
            )

        val remoteUnavailable =
            SearchResult.Playlist(
                id = "remote-2",
                title = "Radiohead Rarities",
                author = "Spotify",
                trackCount = 1,
                artworkUrl = null,
                songs = listOf(sampleTrack("t99", "Creep Acoustic", "Radiohead")),
            )

        val results =
            extractOfflinePlaylists(
                localPlaylists = listOf(localPlaylist),
                query = "radiohead",
                remotePlaylists = listOf(remoteAvailable, remoteUnavailable),
                offlineTracks = listOf(offlineTrack),
            )

        assertEquals(1, results.size)
        assertEquals("remote-1", results.first().id)

        val localResults =
            extractOfflinePlaylists(
                localPlaylists = listOf(localPlaylist),
                query = "rock",
                remotePlaylists = listOf(remoteAvailable),
                offlineTracks = listOf(offlineTrack),
            )

        assertEquals(1, localResults.size)
        assertEquals("local:1", localResults.first().id)
        assertEquals("Rock Classics", localResults.first().title)
    }
}
