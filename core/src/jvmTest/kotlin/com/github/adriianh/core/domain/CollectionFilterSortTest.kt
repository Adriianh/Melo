package com.github.adriianh.core.domain

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.OfflineFilterType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.PlaylistSortOrder
import com.github.adriianh.core.domain.model.SortDirection
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackSortOrder
import com.github.adriianh.core.domain.model.filterAndSortOfflineTracks
import com.github.adriianh.core.domain.model.filterAndSortPlaylists
import com.github.adriianh.core.domain.model.filterAndSortTracks
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionFilterSortTest {

    private fun track(
        id: String,
        title: String,
        artist: String,
        album: String = "",
        durationMs: Long = 1000L
    ): Track {
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            genres = emptyList(),
            artworkUrl = null,
            sourceId = null
        )
    }

    private fun offline(
        track: Track,
        type: DownloadType,
        status: DownloadStatus = DownloadStatus.COMPLETED
    ): OfflineTrack {
        return OfflineTrack(
            track = track,
            downloadType = type,
            downloadStatus = status,
            downloadedAt = 1000L
        )
    }

    @Test
    fun testFilterAndSortTracks() {
        val tracks = listOf(
            track("1", "Yellow Submarine", "The Beatles", "Revolver", 180000L),
            track("2", "Hey Jude", "The Beatles", "Single", 430000L),
            track("3", "Bohemian Rhapsody", "Queen", "A Night at the Opera", 354000L),
            track("4", "Radio Ga Ga", "Queen", "The Works", 344000L)
        )

        // Query filtering by artist
        val beatles = filterAndSortTracks(tracks, query = "beatles")
        assertEquals(2, beatles.size)
        assertEquals(listOf("1", "2"), beatles.map { it.id })

        // Query filtering by album
        val nightAtOpera = filterAndSortTracks(tracks, query = "opera")
        assertEquals(1, nightAtOpera.size)
        assertEquals("3", nightAtOpera.first().id)

        // Sort by Title ASC
        val titleAsc = filterAndSortTracks(
            tracks,
            sortOrder = TrackSortOrder.TITLE,
            sortDirection = SortDirection.ASCENDING
        )
        assertEquals(listOf("3", "2", "4", "1"), titleAsc.map { it.id })

        // Sort by Title DESC
        val titleDesc = filterAndSortTracks(
            tracks,
            sortOrder = TrackSortOrder.TITLE,
            sortDirection = SortDirection.DESCENDING
        )
        assertEquals(listOf("1", "4", "2", "3"), titleDesc.map { it.id })

        // Sort by Duration ASC
        val durationAsc = filterAndSortTracks(
            tracks,
            sortOrder = TrackSortOrder.DURATION,
            sortDirection = SortDirection.ASCENDING
        )
        assertEquals(listOf("1", "4", "3", "2"), durationAsc.map { it.id })

        // Sort by Duration DESC
        val durationDesc = filterAndSortTracks(
            tracks,
            sortOrder = TrackSortOrder.DURATION,
            sortDirection = SortDirection.DESCENDING
        )
        assertEquals(listOf("2", "3", "4", "1"), durationDesc.map { it.id })
    }

    @Test
    fun testFilterAndSortOfflineTracks() {
        val t1 = track("1", "Song A", "Artist X", durationMs = 100L)
        val t2 = track("2", "Song B", "Artist Y", durationMs = 200L)
        val t3 = track("3", "Song C", "Artist X", durationMs = 300L)

        val downloads = listOf(
            offline(t1, DownloadType.MANUAL, DownloadStatus.COMPLETED),
            offline(t2, DownloadType.PREFETCH, DownloadStatus.COMPLETED),
            offline(
                t3,
                DownloadType.MANUAL,
                DownloadStatus.PENDING
            ) // Pending should be filtered out
        )

        // Filter ALL (excludes pending)
        val allCompleted = filterAndSortOfflineTracks(downloads, filterType = OfflineFilterType.ALL)
        assertEquals(2, allCompleted.size)
        assertEquals(listOf("1", "2"), allCompleted.map { it.track.id })

        // Filter MANUAL
        val manualOnly =
            filterAndSortOfflineTracks(downloads, filterType = OfflineFilterType.MANUAL)
        assertEquals(1, manualOnly.size)
        assertEquals("1", manualOnly.first().track.id)

        // Filter CACHE (PREFETCH)
        val cacheOnly = filterAndSortOfflineTracks(downloads, filterType = OfflineFilterType.CACHE)
        assertEquals(1, cacheOnly.size)
        assertEquals("2", cacheOnly.first().track.id)
    }

    @Test
    fun testFilterAndSortPlaylists() {
        val playlists = listOf(
            Playlist(id = 1L, name = "Rock Classics", trackCount = 50, createdAt = 1000L),
            Playlist(id = 2L, name = "Acoustic Chill", trackCount = 20, createdAt = 2000L),
            Playlist(id = 3L, name = "Workout Beats", trackCount = 80, createdAt = 3000L)
        )

        // Filter query
        val chill = filterAndSortPlaylists(playlists, query = "chill")
        assertEquals(1, chill.size)
        assertEquals(2L, chill.first().id)

        // Sort Name ASC
        val nameAsc = filterAndSortPlaylists(
            playlists,
            sortOrder = PlaylistSortOrder.NAME,
            sortDirection = SortDirection.ASCENDING
        )
        assertEquals(listOf(2L, 1L, 3L), nameAsc.map { it.id })

        // Sort Tracks DESC
        val tracksDesc = filterAndSortPlaylists(
            playlists,
            sortOrder = PlaylistSortOrder.TRACKS,
            sortDirection = SortDirection.DESCENDING
        )
        assertEquals(listOf(3L, 1L, 2L), tracksDesc.map { it.id })
    }
}