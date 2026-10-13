package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.CollectionsState
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.handler.search.buildArtistDashboardItems
import com.github.adriianh.cli.tui.isEntityAvailableOffline
import com.github.adriianh.cli.tui.isFavoriteEntityAvailableOffline
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.FavoriteEntity
import com.github.adriianh.core.domain.model.FavoriteEntityType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.util.resolveOfflineEntitySync
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EntityDetailsHandlersTest {
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
    fun testBuildArtistDashboardItemsPairsTopSongsWithSections() {
        val tracks =
            listOf(
                sampleTrack("1", "Song A", "The Artist"),
                sampleTrack("2", "Song B", "The Artist"),
            )
        val albumCards =
            listOf(
                SearchResult.Album(
                    id = "alb1",
                    title = "Album 1",
                    author = "The Artist",
                    year = "2024",
                    artworkUrl = null,
                ),
            )
        val artist =
            SearchResult.Artist(
                id = "art1",
                name = "The Artist",
                artworkUrl = null,
                topSongs = tracks,
                sections =
                    listOf(
                        SearchResult.ArtistSection(
                            title = "Albums",
                            items = albumCards,
                        ),
                    ),
            )

        val dashboardItems = buildArtistDashboardItems(artist)
        assertEquals(1, dashboardItems.size)
        assertTrue(dashboardItems[0] is Pair<*, *>)

        val pair = dashboardItems[0] as Pair<*, *>
        val leftSection = pair.first as SearchResult.ArtistSection
        val rightSection = pair.second as SearchResult.ArtistSection

        assertEquals("Top Songs", leftSection.title)
        assertEquals(2, leftSection.items.size)
        assertEquals("Albums", rightSection.title)
        assertEquals(1, rightSection.items.size)
    }

    @Test
    fun testBuildArtistDashboardItemsDoesNotDuplicateExistingSongSection() {
        val songSection =
            SearchResult.ArtistSection(
                title = "Tracks",
                items = listOf(SearchResult.Song(sampleTrack("1", "Song A", "The Artist"))),
            )
        val artist =
            SearchResult.Artist(
                id = "art1",
                name = "The Artist",
                artworkUrl = null,
                topSongs = listOf(sampleTrack("1", "Song A", "The Artist")),
                sections = listOf(songSection),
            )

        val dashboardItems = buildArtistDashboardItems(artist)
        assertEquals(1, dashboardItems.size)
        val pair = dashboardItems[0] as Pair<*, *>
        val leftSection = pair.first as SearchResult.ArtistSection
        assertEquals("Tracks", leftSection.title)
    }

    @Test
    fun testResolveOfflineEntitySyncResolvesAlbumWithMatchingTracks() {
        val track = sampleTrack("t1", "Paranoid Android", "Radiohead", "OK Computer")
        val offlineTracks = listOf(sampleOffline(track))
        val initialAlbum =
            SearchResult.Album(
                id = "alb-okc",
                title = "OK Computer",
                author = "Radiohead",
                year = "1997",
                artworkUrl = null,
                songs = null,
            )

        val resolved =
            resolveOfflineEntitySync(
                initial = initialAlbum,
                entityCache = null,
                allOffline = offlineTracks,
            )

        assertNotNull(resolved)
        assertTrue(resolved is SearchResult.Album)
        val album = resolved as SearchResult.Album
        assertEquals("OK Computer", album.title)
        assertEquals(1, album.songs?.size)
        assertEquals("Paranoid Android", album.songs?.first()?.title)
    }

    @Test
    fun testResolveOfflineEntitySyncSynthesizesArtistProfile() {
        val track1 = sampleTrack("t1", "Track 1", "Daft Punk", "Discovery")
        val track2 = sampleTrack("t2", "Track 2", "Daft Punk", "Homework")
        val offlineTracks = listOf(sampleOffline(track1), sampleOffline(track2))
        val initialArtist =
            SearchResult.Artist(
                id = "daft-punk",
                name = "Daft Punk",
                artworkUrl = null,
            )

        val resolved =
            resolveOfflineEntitySync(
                initial = initialArtist,
                entityCache = null,
                allOffline = offlineTracks,
            )

        assertNotNull(resolved)
        assertTrue(resolved is SearchResult.Artist)
        val artist = resolved as SearchResult.Artist
        assertEquals("Daft Punk", artist.name)
        assertEquals(2, artist.topSongs?.size)
        assertEquals(1, artist.sections.size)
        assertEquals("Álbumes en tus descargas", artist.sections.first().title)
        assertEquals(
            2,
            artist.sections
                .first()
                .items.size,
        )

        val dashboard = buildArtistDashboardItems(artist)
        assertEquals(1, dashboard.size)
        val pair = dashboard[0] as Pair<*, *>
        val left = pair.first as SearchResult.ArtistSection
        val right = pair.second as SearchResult.ArtistSection
        assertEquals("Top Songs", left.title)
        assertEquals("Álbumes en tus descargas", right.title)
    }

    @Test
    fun testIsEntityAvailableOfflineRespectsOfflineMode() {
        val track = sampleTrack("t1", "Paranoid Android", "Radiohead", "OK Computer")
        val offlineTracks = listOf(sampleOffline(track))
        val album =
            SearchResult.Album(
                id = "alb-okc",
                title = "OK Computer",
                author = "Radiohead",
                year = "1997",
                artworkUrl = null,
            )
        val missingAlbum =
            SearchResult.Album(
                id = "alb-kida",
                title = "Kid A",
                author = "Radiohead",
                year = "2000",
                artworkUrl = null,
            )

        val onlineState = MeloState(isOfflineMode = false)
        assertTrue(onlineState.isEntityAvailableOffline(album))
        assertTrue(onlineState.isEntityAvailableOffline(missingAlbum))

        val offlineState =
            MeloState(
                isOfflineMode = true,
                collections = CollectionsState(offlineTracks = offlineTracks),
            )
        assertTrue(offlineState.isEntityAvailableOffline(album))
        assertFalse(offlineState.isEntityAvailableOffline(missingAlbum))
    }

    @Test
    fun testIsFavoriteEntityAvailableOfflineInMeloState() {
        val track = sampleTrack("t1", "Starboy", "The Weeknd", "Starboy")
        val offlineTracks = listOf(sampleOffline(track))
        val favAlbum =
            FavoriteEntity(
                id = "starboy",
                type = FavoriteEntityType.ALBUM,
                title = "Starboy",
                subtitle = "The Weeknd",
                artworkUrl = null,
                trackCount = null,
            )
        val favMissing =
            FavoriteEntity(
                id = "after-hours",
                type = FavoriteEntityType.ALBUM,
                title = "After Hours",
                subtitle = "The Weeknd",
                artworkUrl = null,
                trackCount = null,
            )

        val onlineState = MeloState(isOfflineMode = false)
        assertTrue(onlineState.isFavoriteEntityAvailableOffline(favAlbum))
        assertTrue(onlineState.isFavoriteEntityAvailableOffline(favMissing))

        val offlineState =
            MeloState(
                isOfflineMode = true,
                collections = CollectionsState(offlineTracks = offlineTracks),
            )
        assertTrue(offlineState.isFavoriteEntityAvailableOffline(favAlbum))
        assertFalse(offlineState.isFavoriteEntityAvailableOffline(favMissing))
    }
}
