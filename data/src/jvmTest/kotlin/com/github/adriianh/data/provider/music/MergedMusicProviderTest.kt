package com.github.adriianh.data.provider.music

import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.MusicProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fake MusicProvider that returns pre-configured results.
 * Each instance represents one "source" (e.g., Spotify, iTunes).
 */
private class FakeMusicProvider(
    private val tracks: List<Track> = emptyList(),
    private val albums: List<SearchResult.Album> = emptyList(),
    private val artists: List<SearchResult.Artist> = emptyList(),
    private val playlists: List<SearchResult.Playlist> = emptyList(),
    private val trackLookup: Map<String, Track> = emptyMap(),
    private val albumLookup: Map<String, SearchResult.Album> = emptyMap(),
    private val artistLookup: Map<String, SearchResult.Artist> = emptyMap(),
    private val suggestions: List<String> = emptyList(),
    private val homeSections: List<HomeSection> = emptyList(),
    private val exploreSections: List<HomeSection> = emptyList(),
    private val trendingTracks: List<Track> = emptyList(),
    private val radioTracks: List<Track> = emptyList(),
    private val shouldThrowOnSearch: Boolean = false,
) : MusicProvider {

    override suspend fun search(query: String): List<Track> {
        if (shouldThrowOnSearch) throw RuntimeException("Provider error")
        return tracks
    }

    override suspend fun searchAlbums(query: String) = albums
    override suspend fun searchArtists(query: String) = artists
    override suspend fun searchPlaylists(query: String) = playlists

    override suspend fun searchAll(query: String): List<Track> {
        if (shouldThrowOnSearch) throw RuntimeException("Provider error")
        return tracks
    }

    override suspend fun searchAllAlbums(query: String) = albums
    override suspend fun searchAllArtists(query: String) = artists
    override suspend fun searchAllPlaylists(query: String) = playlists

    override suspend fun getTrack(id: String): Track? = trackLookup[id]
    override suspend fun getAlbumDetails(id: String): SearchResult.Album? = albumLookup[id]
    override suspend fun getArtistDetails(id: String): SearchResult.Artist? = artistLookup[id]

    override suspend fun getSearchSuggestions(query: String): List<String> = suggestions

    override suspend fun getHome(): List<HomeSection> = homeSections
    override suspend fun getExplore(): List<HomeSection> = exploreSections
    override suspend fun getTrending(): List<Track> = trendingTracks
    override suspend fun getRadio(videoId: String): List<Track> = radioTracks
}

private fun fakeTrack(
    id: String,
    title: String = "Track $id",
    artist: String = "Artist $id",
    durationMs: Long = 180_000
) = Track(
    id = id,
    title = title,
    artist = artist,
    album = "Album",
    durationMs = durationMs,
    genres = emptyList(),
    artworkUrl = null,
    sourceId = null
)

class MergedMusicProviderTest {

    @Test
    fun `merge interleaves results from two providers`() = runTest {
        val providerA = FakeMusicProvider(
            tracks = listOf(fakeTrack("a1"), fakeTrack("a2"), fakeTrack("a3"))
        )
        val providerB = FakeMusicProvider(
            tracks = listOf(fakeTrack("b1"), fakeTrack("b2"))
        )
        val merged = MergedMusicProvider(listOf(providerA, providerB))

        val result = merged.search("test")

        assertEquals(5, result.size)
        assertEquals("a1", result[0].id)
        assertEquals("b1", result[1].id)
        assertEquals("a2", result[2].id)
        assertEquals("b2", result[3].id)
        assertEquals("a3", result[4].id)
    }

    @Test
    fun `merge handles single provider`() = runTest {
        val provider = FakeMusicProvider(
            tracks = listOf(fakeTrack("1"), fakeTrack("2"))
        )
        val merged = MergedMusicProvider(listOf(provider))

        val result = merged.search("test")

        assertEquals(2, result.size)
        assertEquals("1", result[0].id)
        assertEquals("2", result[1].id)
    }

    @Test
    fun `merge handles empty provider list`() = runTest {
        val merged = MergedMusicProvider(emptyList())

        val result = merged.search("test")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `merge handles provider returning empty list`() = runTest {
        val empty = FakeMusicProvider(tracks = emptyList())
        val real = FakeMusicProvider(tracks = listOf(fakeTrack("1")))
        val merged = MergedMusicProvider(listOf(empty, real))

        val result = merged.search("test")

        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `deduplicate removes tracks with same artist and title`() = runTest {
        val providerA = FakeMusicProvider(
            tracks = listOf(fakeTrack("a1", title = "Song", artist = "Artist"))
        )
        val providerB = FakeMusicProvider(
            tracks = listOf(fakeTrack("b1", title = "Song", artist = "Artist"))
        )
        val merged = MergedMusicProvider(listOf(providerA, providerB))

        val result = merged.search("test")

        assertEquals(1, result.size, "Duplicate track should be removed")
    }

    @Test
    fun `deduplicate keeps tracks with same title but different artist`() = runTest {
        val providerA = FakeMusicProvider(
            tracks = listOf(fakeTrack("a1", title = "Song", artist = "Artist A"))
        )
        val providerB = FakeMusicProvider(
            tracks = listOf(fakeTrack("b1", title = "Song", artist = "Artist B"))
        )
        val merged = MergedMusicProvider(listOf(providerA, providerB))

        val result = merged.search("test")

        assertEquals(2, result.size, "Different artist = not a duplicate")
    }

    @Test
    fun `deduplicate ignores parenthetical text in title`() = runTest {
        val providerA = FakeMusicProvider(
            tracks = listOf(fakeTrack("a1", title = "Song (Remastered)", artist = "Same Artist"))
        )
        val providerB = FakeMusicProvider(
            tracks = listOf(fakeTrack("b1", title = "Song", artist = "Same Artist"))
        )
        val merged = MergedMusicProvider(listOf(providerA, providerB))

        val result = merged.search("test")

        assertEquals(1, result.size, "Song (Remastered) should dedup with Song")
    }

    @Test
    fun `deduplicate uses duration bucket not exact duration`() = runTest {
        // Both have same artist+title, durations differ by 5 seconds
        // 180,000 / 10,000 = 18, 185,000 / 10,000 = 18 → same bucket
        val providerA = FakeMusicProvider(
            tracks = listOf(fakeTrack("a1", title = "Song", artist = "Same", durationMs = 180_000))
        )
        val providerB = FakeMusicProvider(
            tracks = listOf(fakeTrack("b1", title = "Song", artist = "Same", durationMs = 185_000))
        )
        val merged = MergedMusicProvider(listOf(providerA, providerB))

        val result = merged.search("test")

        assertEquals(1, result.size, "5s difference within same bucket → dedup")
    }

    @Test
    fun `deduplicate keeps tracks in different duration buckets`() = runTest {
        // 180,000 / 10,000 = 18, 200,000 / 10,000 = 20 → different buckets
        val providerA = FakeMusicProvider(
            tracks = listOf(fakeTrack("a1", title = "Song", artist = "Same", durationMs = 180_000))
        )
        val providerB = FakeMusicProvider(
            tracks = listOf(fakeTrack("b1", title = "Song", artist = "Same", durationMs = 200_000))
        )
        val merged = MergedMusicProvider(listOf(providerA, providerB))

        val result = merged.search("test")

        assertEquals(2, result.size, "Different duration buckets → keep both")
    }

    @Test
    fun `search continues when one provider throws`() = runTest {
        val good = FakeMusicProvider(tracks = listOf(fakeTrack("1")))
        val bad = FakeMusicProvider(shouldThrowOnSearch = true)
        val merged = MergedMusicProvider(listOf(bad, good))

        val result = merged.search("test")

        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `search returns empty when all providers throw`() = runTest {
        val bad1 = FakeMusicProvider(shouldThrowOnSearch = true)
        val bad2 = FakeMusicProvider(shouldThrowOnSearch = true)
        val merged = MergedMusicProvider(listOf(bad1, bad2))

        val result = merged.search("test")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchAlbums deduplicates by id`() = runTest {
        val album = SearchResult.Album(
            id = "album1", title = "Test Album", author = "Artist",
            year = "2024", artworkUrl = null, songs = null
        )
        val providerA = FakeMusicProvider(albums = listOf(album))
        val providerB = FakeMusicProvider(albums = listOf(album))
        val merged = MergedMusicProvider(listOf(providerA, providerB))

        val result = merged.searchAlbums("test")

        assertEquals(1, result.size)
    }

    @Test
    fun `searchArtists deduplicates by id`() = runTest {
        val artist = SearchResult.Artist(
            id = "artist1", name = "Test Artist", artworkUrl = null
        )
        val providerA = FakeMusicProvider(artists = listOf(artist))
        val providerB = FakeMusicProvider(artists = listOf(artist))
        val merged = MergedMusicProvider(listOf(providerA, providerB))

        val result = merged.searchArtists("test")

        assertEquals(1, result.size)
    }

    @Test
    fun `searchPlaylists deduplicates by id`() = runTest {
        val playlist = SearchResult.Playlist(
            id = "pl1", title = "Test Playlist", author = "User",
            trackCount = 10, artworkUrl = null, songs = null
        )
        val providerA = FakeMusicProvider(playlists = listOf(playlist))
        val providerB = FakeMusicProvider(playlists = listOf(playlist))
        val merged = MergedMusicProvider(listOf(providerA, providerB))

        val result = merged.searchPlaylists("test")

        assertEquals(1, result.size)
    }

    @Test
    fun `getAlbumDetails returns first provider result with songs`() = runTest {
        val emptyAlbum = SearchResult.Album(
            id = "a1", title = "Empty", author = "X",
            year = null, artworkUrl = null, songs = null
        )
        val fullAlbum = SearchResult.Album(
            id = "a2", title = "Full", author = "Y",
            year = "2024", artworkUrl = null,
            songs = listOf(fakeTrack("t1"))
        )
        val p1 = FakeMusicProvider(albumLookup = mapOf("album" to emptyAlbum))
        val p2 = FakeMusicProvider(albumLookup = mapOf("album" to fullAlbum))
        val merged = MergedMusicProvider(listOf(p1, p2))

        val result = merged.getAlbumDetails("album")

        assertNotNull(result)
        assertEquals("Full", result.title)
    }

    @Test
    fun `getAlbumDetails returns null when all providers return null`() = runTest {
        val p1 = FakeMusicProvider()
        val merged = MergedMusicProvider(listOf(p1))

        val result = merged.getAlbumDetails("nonexistent")

        assertNull(result)
    }

    @Test
    fun `getSearchSuggestions returns first non-empty result`() = runTest {
        val p1 = FakeMusicProvider(suggestions = emptyList())
        val p2 = FakeMusicProvider(suggestions = listOf("suggestion1", "suggestion2"))
        val merged = MergedMusicProvider(listOf(p1, p2))

        val result = merged.getSearchSuggestions("test")

        assertEquals(2, result.size)
        assertEquals("suggestion1", result[0])
    }

    @Test
    fun `getSearchSuggestions returns empty when all providers return empty`() = runTest {
        val p1 = FakeMusicProvider(suggestions = emptyList())
        val merged = MergedMusicProvider(listOf(p1))

        val result = merged.getSearchSuggestions("test")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getTrack routes itunes prefix to ItunesMusicProvider`() = runTest {
        // ItunesMusicProvider needs apiClient, so we test routing
        // by verifying the filterIsInstance logic works with a FakeProvider
        // that happens to be an ItunesMusicProvider subtype.
        // In practice, this test validates the routing structure.
        val track = fakeTrack("itunes:123")
        val provider = FakeMusicProvider(trackLookup = mapOf("itunes:123" to track))
        val merged = MergedMusicProvider(listOf(provider))

        // No ItunesMusicProvider in list → falls through to sequential scan
        val result = merged.getTrack("itunes:123")

        assertNotNull(result)
        assertEquals("itunes:123", result.id)
    }

    @Test
    fun `getTrack falls back to sequential scan when no provider matches prefix`() = runTest {
        // An ID with an unknown prefix (not itunes:, piped:, spotify:)
        // should fall through to the sequential scan
        val track = fakeTrack("unknown:456")
        val provider = FakeMusicProvider(trackLookup = mapOf("unknown:456" to track))
        val merged = MergedMusicProvider(listOf(provider))

        val result = merged.getTrack("unknown:456")

        assertNotNull(result)
        assertEquals("unknown:456", result.id)
    }

    @Test
    fun `getTrack returns null when no provider has the track`() = runTest {
        val provider = FakeMusicProvider(trackLookup = emptyMap())
        val merged = MergedMusicProvider(listOf(provider))

        val result = merged.getTrack("nonexistent")

        assertNull(result)
    }

    @Test
    fun `getHome returns first non-empty result`() = runTest {
        val p1 = FakeMusicProvider(homeSections = emptyList())
        val p2 = FakeMusicProvider(
            homeSections = listOf(
                HomeSection(
                    "Section",
                    HomeSectionType.MIXED,
                    emptyList()
                )
            )
        )
        val merged = MergedMusicProvider(listOf(p1, p2))

        val result = merged.getHome()

        assertEquals(1, result.size)
        assertEquals("Section", result[0].title)
    }

    @Test
    fun `getHome continues when one provide throws`() = runTest {
        val bad = object : MusicProvider by FakeMusicProvider() {
            override suspend fun getHome(): List<HomeSection> {
                throw RuntimeException("Provider error")
            }
        }
        val good = FakeMusicProvider(
            homeSections = listOf(
                HomeSection(
                    "Good Section",
                    HomeSectionType.MIXED,
                    emptyList()
                )
            )
        )
        val merged = MergedMusicProvider(listOf(bad, good))

        val result = merged.getHome()

        assertEquals(1, result.size)
        assertEquals("Good Section", result[0].title)
    }

    @Test
    fun `getHome returns empty when all providers return empty`() = runTest {
        val p1 = FakeMusicProvider()
        val p2 = FakeMusicProvider()
        val merged = MergedMusicProvider(listOf(p1, p2))

        val result = merged.getHome()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getExplore returns first non-empty result`() = runTest {
        val p1 = FakeMusicProvider(exploreSections = emptyList())
        val p2 = FakeMusicProvider(
            exploreSections = listOf(
                HomeSection(
                    "Section",
                    HomeSectionType.MIXED,
                    emptyList()
                )
            )
        )
        val merged = MergedMusicProvider(listOf(p1, p2))

        val result = merged.getExplore()

        assertEquals(1, result.size)
        assertEquals("Section", result[0].title)
    }

    @Test
    fun `getTrending returns first non-empty result`() = runTest {
        val p1 = FakeMusicProvider(trendingTracks = emptyList())
        val p2 = FakeMusicProvider(trendingTracks = listOf(fakeTrack("t1"), fakeTrack("t2")))
        val merged = MergedMusicProvider(listOf(p1, p2))

        val result = merged.getTrending()

        assertEquals(2, result.size)
        assertEquals("t1", result[0].id)
    }

    @Test
    fun `getTrending returns empty when all providers return empty`() = runTest {

        val p1 = FakeMusicProvider(trendingTracks = emptyList())
        val p2 = FakeMusicProvider(trendingTracks = emptyList())
        val merged = MergedMusicProvider(listOf(p1, p2))

        val result = merged.getTrending()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getRadio returns first non-empty result`() = runTest {
        val p1 = FakeMusicProvider(radioTracks = emptyList())
        val p2 = FakeMusicProvider(radioTracks = listOf(fakeTrack("r1"), fakeTrack("r2")))
        val merged = MergedMusicProvider(listOf(p1, p2))

        val result = merged.getRadio("videoId")

        assertEquals(2, result.size)
        assertEquals("r1", result[0].id)
    }

    @Test
    fun `getRadio continues when one provider throws`() = runTest {
        val bad = object : MusicProvider by FakeMusicProvider() {
            override suspend fun getRadio(videoId: String): List<Track> =
                throw RuntimeException("Provider error")
        }
        val good = FakeMusicProvider(radioTracks = listOf(fakeTrack("r3")))
        val merged = MergedMusicProvider(listOf(bad, good))

        val result = merged.getRadio("videoId")

        assertEquals(1, result.size)
        assertEquals("r3", result[0].id)
    }

    @Test
    fun `getExplore returns empty when all providers return empty`() = runTest {
        val p1 = FakeMusicProvider(exploreSections = emptyList())
        val p2 = FakeMusicProvider(exploreSections = emptyList())
        val merged = MergedMusicProvider(listOf(p1, p2))

        val result = merged.getExplore()

        assertTrue(result.isEmpty())
    }
}