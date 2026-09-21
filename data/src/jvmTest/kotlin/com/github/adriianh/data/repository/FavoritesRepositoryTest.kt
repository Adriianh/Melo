package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.FavoriteEntity
import com.github.adriianh.core.domain.model.FavoriteEntityType
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoritesRepositoryTest {

    private val db = createInMemoryDatabase()
    private val repo = FavoritesRepositoryImpl(db)

    private fun fakeTrack(id: String = "t1", title: String = "Song $id") = Track(
        id = id, title = title, artist = "Artist", album = "Album",
        durationMs = 180_000, genres = emptyList(),
        artworkUrl = null, sourceId = null
    )

    @Test
    fun `addFavorite makes track appear in favorites`() = runTest {
        val track = fakeTrack()

        repo.addFavorite(track)

        val favorites = repo.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("t1", favorites[0].id)
    }

    @Test
    fun `removeFavorite removes track from favorites`() = runTest {
        repo.addFavorite(fakeTrack("t1"))
        repo.addFavorite(fakeTrack("t2"))

        repo.removeFavorite("t1")

        val favorites = repo.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("t2", favorites[0].id)
    }

    @Test
    fun `isFavorite returns true for favorited track`() = runTest {
        repo.addFavorite(fakeTrack("t1"))

        assertTrue(repo.isFavorite("t1"))
    }

    @Test
    fun `isFavorite returns false for non-favorited track`() = runTest {
        assertFalse(repo.isFavorite("nonexistent"))
    }

    @Test
    fun `addFavorite with existing id replaces (upsert)`() = runTest {
        repo.addFavorite(fakeTrack("t1", title = "Original"))
        repo.addFavorite(fakeTrack("t1", title = "Updated"))

        val favorites = repo.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("Updated", favorites[0].title)
    }

    @Test
    fun `favorites are ordered by added_at descending`() = runTest {
        repo.addFavorite(fakeTrack("t1"))
        Thread.sleep(1100) // ensure different timestamps (currentTimeSeconds has 1s resolution)
        repo.addFavorite(fakeTrack("t2"))

        val favorites = repo.getFavorites().first()
        assertEquals("t2", favorites[0].id, "Most recent should be first")
        assertEquals("t1", favorites[1].id)
    }

    @Test
    fun `addFavoriteEntity and getFavoriteEntities by type`() = runTest {
        val album = FavoriteEntity(
            id = "alb-1",
            type = FavoriteEntityType.ALBUM,
            title = "Abbey Road",
            subtitle = "The Beatles",
            trackCount = 17,
        )
        val artist = FavoriteEntity(
            id = "art-1",
            type = FavoriteEntityType.ARTIST,
            title = "The Beatles",
            subtitle = "50M monthly listeners",
        )

        repo.addFavoriteEntity(album)
        repo.addFavoriteEntity(artist)

        val all = repo.getFavoriteEntities().first()
        assertEquals(2, all.size)

        val albums = repo.getFavoriteEntities(FavoriteEntityType.ALBUM).first()
        assertEquals(1, albums.size)
        assertEquals("alb-1", albums[0].id)
        assertEquals("Abbey Road", albums[0].title)

        val artists = repo.getFavoriteEntities(FavoriteEntityType.ARTIST).first()
        assertEquals(1, artists.size)
        assertEquals("art-1", artists[0].id)

        assertTrue(repo.isFavoriteEntity("alb-1"))
        assertTrue(repo.isFavoriteEntity("art-1"))
        assertFalse(repo.isFavoriteEntity("nonexistent"))

        repo.removeFavoriteEntity("alb-1")
        assertFalse(repo.isFavoriteEntity("alb-1"))
        assertEquals(0, repo.getFavoriteEntities(FavoriteEntityType.ALBUM).first().size)
    }
}