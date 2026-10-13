package com.github.adriianh.core.util

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.FavoriteEntity
import com.github.adriianh.core.domain.model.FavoriteEntityType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfflineEntityResolverTest {
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
        artworkUrl = null,
        sourceId = null,
    )

    private fun sampleOffline(track: Track) =
        OfflineTrack(
            track = track,
            localFilePath = "/tmp/${track.id}.mp3",
            downloadStatus = DownloadStatus.COMPLETED,
        )

    @Test
    fun testIsEntityAvailableOfflineReturnsTrueWhenAlbumHasMatchingTrack() {
        val track = sampleTrack("t1", "Paranoid Android", "Radiohead", "OK Computer")
        val offlineTracks = listOf(sampleOffline(track))
        val album =
            SearchResult.Album(
                id = "alb-1",
                title = "OK Computer",
                author = "Radiohead",
                year = "1997",
                artworkUrl = null,
            )

        assertTrue(isEntityAvailableOffline(album, offlineTracks))
    }

    @Test
    fun testIsEntityAvailableOfflineReturnsFalseWhenAlbumHasNoMatchingTrack() {
        val track = sampleTrack("t1", "Paranoid Android", "Radiohead", "OK Computer")
        val offlineTracks = listOf(sampleOffline(track))
        val album =
            SearchResult.Album(
                id = "alb-2",
                title = "Kid A",
                author = "Radiohead",
                year = "2000",
                artworkUrl = null,
            )

        assertFalse(isEntityAvailableOffline(album, offlineTracks))
    }

    @Test
    fun testIsEntityAvailableOfflineReturnsTrueWhenArtistHasMatchingTrack() {
        val track = sampleTrack("t1", "One More Time", "Daft Punk", "Discovery")
        val offlineTracks = listOf(sampleOffline(track))
        val artist =
            SearchResult.Artist(
                id = "art-1",
                name = "Daft Punk",
                artworkUrl = null,
            )

        assertTrue(isEntityAvailableOffline(artist, offlineTracks))
    }

    @Test
    fun testIsEntityAvailableOfflineReturnsFalseWhenArtistHasNoMatchingTrack() {
        val track = sampleTrack("t1", "One More Time", "Daft Punk", "Discovery")
        val offlineTracks = listOf(sampleOffline(track))
        val artist =
            SearchResult.Artist(
                id = "art-2",
                name = "Justice",
                artworkUrl = null,
            )

        assertFalse(isEntityAvailableOffline(artist, offlineTracks))
    }

    @Test
    fun testIsFavoriteEntityAvailableOfflineDelegatesCorrectly() {
        val track = sampleTrack("t1", "Starboy", "The Weeknd", "Starboy")
        val offlineTracks = listOf(sampleOffline(track))

        val favArtist =
            FavoriteEntity(
                id = "weeknd",
                type = FavoriteEntityType.ARTIST,
                title = "The Weeknd",
                subtitle = null,
                artworkUrl = null,
                trackCount = null,
            )
        val favAlbum =
            FavoriteEntity(
                id = "starboy",
                type = FavoriteEntityType.ALBUM,
                title = "Starboy",
                subtitle = "The Weeknd",
                artworkUrl = null,
                trackCount = null,
            )
        val favMissingAlbum =
            FavoriteEntity(
                id = "after-hours",
                type = FavoriteEntityType.ALBUM,
                title = "After Hours",
                subtitle = "The Weeknd",
                artworkUrl = null,
                trackCount = null,
            )

        assertTrue(isFavoriteEntityAvailableOffline(favArtist, offlineTracks))
        assertTrue(isFavoriteEntityAvailableOffline(favAlbum, offlineTracks))
        assertFalse(isFavoriteEntityAvailableOffline(favMissingAlbum, offlineTracks))
    }
}
