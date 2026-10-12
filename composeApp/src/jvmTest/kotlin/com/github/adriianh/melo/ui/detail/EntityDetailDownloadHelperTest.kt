package com.github.adriianh.melo.ui.detail

import com.github.adriianh.core.domain.cache.LibraryCache
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.LibraryCacheData
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.repository.OfflineRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EntityDetailDownloadHelperTest {
    private fun createTrack(
        id: String,
        title: String,
        artist: String,
        album: String = "Test Album",
        sourceId: String? = null,
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 180000L,
        genres = emptyList(),
        artworkUrl = "https://example.com/art.jpg",
        sourceId = sourceId,
    )

    @Test
    fun cleanTrackIdRemovesKnownPrefixes() {
        assertEquals("abc123", cleanTrackId("piped:abc123"))
        assertEquals("local_song", cleanTrackId("local:local_song"))
        assertEquals("file_song", cleanTrackId("file:file_song"))
        assertEquals("123", cleanTrackId("VL123"))
        assertEquals("spot_id", cleanTrackId("spotify:spot_id"))
        assertEquals("itunes_id", cleanTrackId("itunes:itunes_id"))
        assertEquals("clean_id", cleanTrackId("clean_id"))
    }

    @Test
    fun findMatchingOfflineArtistTracksFiltersByArtistNameAndStatus() {
        val artist =
            SearchResult.Artist(
                id = "artist-1",
                name = "Daft Punk",
                artworkUrl = null,
            )

        val track1 = createTrack("t1", "One More Time", "Daft Punk")
        val track2 = createTrack("t2", "Get Lucky", "Daft Punk feat. Pharrell Williams")
        val track3 = createTrack("t3", "Instant Crush", "Daft Punk")
        val track4 = createTrack("t4", "Starboy", "The Weeknd")

        val offlineTracks =
            listOf(
                OfflineTrack(track = track1, downloadStatus = DownloadStatus.COMPLETED),
                OfflineTrack(track = track2, downloadStatus = DownloadStatus.COMPLETED),
                OfflineTrack(track = track3, downloadStatus = DownloadStatus.DOWNLOADING),
                OfflineTrack(track = track4, downloadStatus = DownloadStatus.COMPLETED),
            )

        val matches =
            findMatchingOfflineArtistTracks(
                artist = artist,
                offlineTracks = offlineTracks,
                initialArtistId = "",
            )

        assertEquals(2, matches.size)
        assertTrue(matches.any { it.id == "t1" })
        assertTrue(matches.any { it.id == "t2" })
    }

    @Test
    fun findMatchingOfflineArtistTracksMatchesByInitialArtistId() {
        val artist =
            SearchResult.Artist(
                id = "artist-2",
                name = "Gorillaz",
                artworkUrl = null,
            )

        val track1 = createTrack("t1", "Feel Good Inc", "channel-gorillaz-id")

        val offlineTracks =
            listOf(
                OfflineTrack(track = track1, downloadStatus = DownloadStatus.COMPLETED),
            )

        val matches =
            findMatchingOfflineArtistTracks(
                artist = artist,
                offlineTracks = offlineTracks,
                initialArtistId = "channel-gorillaz-id",
            )

        assertEquals(1, matches.size)
        assertEquals("t1", matches.first().id)
    }

    @Test
    fun resolveOfflineEntityResolvesArtistUsingLibraryCacheWhenNameIsBlank() =
        runTest {
            val initial =
                SearchResult.Artist(
                    id = "UC_CHANNEL_123",
                    name = "",
                    artworkUrl = null,
                )

            val track1 = createTrack("t1", "Harder Better Faster", "Daft Punk", album = "Discovery")
            val offlineRepository = mockk<OfflineRepository>()
            coEvery { offlineRepository.getOfflineTracks() } returns
                listOf(
                    OfflineTrack(track = track1, downloadStatus = DownloadStatus.COMPLETED),
                )

            val libraryCache = mockk<LibraryCache>()
            coEvery { libraryCache.get() } returns
                LibraryCacheData(
                    artists =
                        listOf(
                            SearchResult.Artist(
                                id = "UC_CHANNEL_123",
                                name = "Daft Punk",
                                artworkUrl = "https://example.com/daft.jpg",
                            ),
                        ),
                )

            val resolved =
                resolveOfflineEntity(
                    initial = initial,
                    entityCache = null,
                    offlineRepository = offlineRepository,
                    libraryCache = libraryCache,
                ) as? SearchResult.Artist

            assertNotNull(resolved)
            assertEquals("Daft Punk", resolved.name)
            assertEquals("https://example.com/daft.jpg", resolved.artworkUrl)
            assertEquals(1, resolved.topSongs?.size)
            assertEquals(1, resolved.sections.size)
            val firstSection = resolved.sections.first()
            assertEquals("Álbumes en tus descargas", firstSection.title)
            val albumCard = firstSection.items.first() as SearchResult.Album
            assertEquals("Discovery", albumCard.title)
        }
}
