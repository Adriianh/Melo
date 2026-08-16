package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.ResolvedMetadata
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.core.domain.provider.MetadataProvider
import com.github.adriianh.core.domain.provider.MusicProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MusicRepositoryTest {

    private val musicProvider = mockk<MusicProvider>()
    private val audioProvider = mockk<AudioProvider>(relaxed = true)
    private val discoveryProvider = mockk<DiscoveryProvider>(relaxed = true)
    private val metadataProvider = mockk<MetadataProvider>(relaxed = true)

    private fun createRepository() = MusicRepositoryImpl(
        musicProvider, audioProvider, discoveryProvider, metadataProvider
    )

    private fun fakeTrack(
        id: String,
        title: String = "Track $id",
        artist: String = "Artist",
        durationMs: Long = 180_000,
        sourceId: String? = null,
        artworkUrl: String? = null,
        album: String = "Album"
    ) = Track(
        id = id, title = title, artist = artist, album = album,
        durationMs = durationMs, genres = emptyList(),
        artworkUrl = artworkUrl, sourceId = sourceId
    )

    @Test
    fun `search returns first page of results`() = runTest {
        val tracks = (1..25).map { fakeTrack("t$it") }
        coEvery { musicProvider.search("test") } returns tracks
        coEvery { musicProvider.searchAll("test") } returns tracks

        val result = createRepository().search("test")

        assertEquals(20, result.size, "Should return pageSize (20)")
    }

    @Test
    fun `search deduplicates results`() = runTest {
        val tracks = listOf(
            fakeTrack("1", title = "Song", artist = "Artist A"),
            fakeTrack("2", title = "Song", artist = "Artist A")
        )
        coEvery { musicProvider.search("test") } returns tracks
        coEvery { musicProvider.searchAll("test") } returns tracks

        val result = createRepository().search("test")

        assertEquals(1, result.size, "Duplicate should be removed")
    }

    @Test
    fun `search returns all when less than pageSize`() = runTest {
        coEvery { musicProvider.search("test") } returns listOf(fakeTrack("1"), fakeTrack("2"))
        coEvery { musicProvider.searchAll("test") } returns listOf(fakeTrack("1"), fakeTrack("2"))

        val result = createRepository().search("test")

        assertEquals(2, result.size)
    }

    @Test
    fun `hasMore returns false before search`() = runTest {
        assertTrue(!createRepository().hasMore(0))
    }

    @Test
    fun `hasMore returns true when more results exist`() = runTest {
        coEvery { musicProvider.search("test") } returns (1..30).map { fakeTrack("t$it") }
        coEvery { musicProvider.searchAll("test") } returns (1..30).map { fakeTrack("t$it") }

        val repo = createRepository()
        repo.search("test")

        assertTrue(repo.hasMore(0))
        assertTrue(repo.hasMore(20))
    }

    @Test
    fun `hasMore returns false when offset past results`() = runTest {
        coEvery { musicProvider.search("test") } returns listOf(fakeTrack("1"))
        coEvery { musicProvider.searchAll("test") } returns listOf(fakeTrack("1"))

        val repo = createRepository()
        repo.search("test")

        // Wait for background fetch on real IO dispatcher
        Thread.sleep(200)

        assertTrue(!repo.hasMore(20))
    }

    @Test
    fun `getHome delegates to musicProvider`() = runTest {
        val sections = listOf(HomeSection("Top", HomeSectionType.MIXED, emptyList()))
        coEvery { musicProvider.getHome() } returns sections

        assertEquals(sections, createRepository().getHome())
    }

    @Test
    fun `getExplore delegates to musicProvider`() = runTest {
        val sections = listOf(HomeSection("Explore", HomeSectionType.MIXED, emptyList()))
        coEvery { musicProvider.getExplore() } returns sections

        assertEquals(sections, createRepository().getExplore())
    }

    @Test
    fun `getTrending delegates to musicProvider`() = runTest {
        val tracks = listOf(fakeTrack("1"), fakeTrack("2"))
        coEvery { musicProvider.getTrending() } returns tracks

        assertEquals(tracks, createRepository().getTrending())
    }

    @Test
    fun `getRadio delegates to musicProvider`() = runTest {
        val tracks = listOf(fakeTrack("1"))
        coEvery { musicProvider.getRadio("video1") } returns tracks

        assertEquals(tracks, createRepository().getRadio("video1"))
    }

    @Test
    fun `getTrack returns null when provider returns null`() = runTest {
        coEvery { musicProvider.getTrack("nonexistent") } returns null

        assertNull(createRepository().getTrack("nonexistent"))
    }

    @Test
    fun `getTrack enriches genres from discoveryProvider`() = runTest {
        val track = fakeTrack("1")
        coEvery { musicProvider.getTrack("1") } returns track
        coEvery { discoveryProvider.getGenres("Artist") } returns listOf("rock", "indie")

        val result = createRepository().getTrack("1")

        assertEquals(listOf("rock", "indie"), result?.genres)
    }

    @Test
    fun `getTrack uses existing sourceId when available`() = runTest {
        val track = fakeTrack("1", sourceId = "existing_source")
        coEvery { musicProvider.getTrack("1") } returns track

        val result = createRepository().getTrack("1")

        assertEquals("existing_source", result?.sourceId)
        coVerify(exactly = 0) { audioProvider.getSourceId(any(), any(), any()) }
    }

    @Test
    fun `getTrack resolves sourceId when missing`() = runTest {
        val track = fakeTrack("1", sourceId = null)
        coEvery { musicProvider.getTrack("1") } returns track
        coEvery { audioProvider.getSourceId(any(), any(), any()) } returns "resolved_source"

        val result = createRepository().getTrack("1")

        assertEquals("resolved_source", result?.sourceId)
    }

    @Test
    fun `getTrack resolves metadata when artwork is missing`() = runTest {
        val track = fakeTrack("1", artworkUrl = null, album = "")
        coEvery { musicProvider.getTrack("1") } returns track
        coEvery { metadataProvider.resolveMetadata("Track 1", "Artist") } returns
                ResolvedMetadata(artworkUrl = "http://art.jpg", album = "Resolved Album")

        val result = createRepository().getTrack("1")

        assertEquals("http://art.jpg", result?.artworkUrl)
        assertEquals("Resolved Album", result?.album)
    }

    @Test
    fun `getTrack skips metadata when artwork and album exist`() = runTest {
        val track = fakeTrack("1", artworkUrl = "http://existing.jpg", album = "Existing Album")
        coEvery { musicProvider.getTrack("1") } returns track

        val result = createRepository().getTrack("1")

        assertEquals("http://existing.jpg", result?.artworkUrl)
        assertEquals("Existing Album", result?.album)
        coVerify(exactly = 0) { metadataProvider.resolveMetadata(any(), any()) }
    }

    @Test
    fun `getTrack works with null optional providers`() = runTest {
        val track = fakeTrack("1", sourceId = "src")
        coEvery { musicProvider.getTrack("1") } returns track

        val repo = MusicRepositoryImpl(musicProvider, null, null, null)
        val result = repo.getTrack("1")

        assertEquals("src", result?.sourceId)
        assertEquals(emptyList(), result?.genres)
    }

    @Test
    fun `dedup ignores parenthetical text in title`() = runTest {
        val tracks = listOf(
            fakeTrack("1", title = "Song (Remastered)"),
            fakeTrack("2", title = "Song")
        )
        coEvery { musicProvider.search("test") } returns tracks
        coEvery { musicProvider.searchAll("test") } returns tracks

        assertEquals(1, createRepository().search("test").size)
    }

    @Test
    fun `dedup uses duration buckets`() = runTest {
        val tracks = listOf(
            fakeTrack("1", title = "Song", durationMs = 180_000),
            fakeTrack("2", title = "Song", durationMs = 185_000)
        )
        coEvery { musicProvider.search("test") } returns tracks
        coEvery { musicProvider.searchAll("test") } returns tracks

        assertEquals(1, createRepository().search("test").size)
    }

    @Test
    fun `loadMore returns correct page from cache`() = runTest {
        coEvery { musicProvider.search("test") } returns (1..50).map { fakeTrack("t$it") }
        coEvery { musicProvider.searchAll("test") } returns (1..50).map { fakeTrack("t$it") }

        val repo = createRepository()
        repo.search("test")
        Thread.sleep(200)

        val page1 = repo.loadMore("test", 0)
        val page2 = repo.loadMore("test", 20)

        assertEquals(20, page1.size)
        assertEquals(20, page2.size)
        assertTrue(page1[0].id != page2[0].id)
    }

    @Test
    fun `search cancels previous background fetch`() = runTest {
        coEvery { musicProvider.search("query1") } returns (1..10).map { fakeTrack("a$it") }
        coEvery { musicProvider.searchAll("query1") } returns (1..10).map { fakeTrack("a$it") }
        coEvery { musicProvider.search("query2") } returns (1..5).map { fakeTrack("b$it") }
        coEvery { musicProvider.searchAll("query2") } returns (1..5).map { fakeTrack("b$it") }

        val repo = createRepository()
        repo.search("query1")
        repo.search("query2")
        Thread.sleep(500)

        val result = repo.loadMore("query2", 0)
        assertEquals(5, result.size)
    }
}