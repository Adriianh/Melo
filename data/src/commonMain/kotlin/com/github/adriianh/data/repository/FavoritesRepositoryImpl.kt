package com.github.adriianh.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.FavoritesRepository
import com.github.adriianh.data.local.Favorites
import com.github.adriianh.data.local.MeloDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FavoritesRepositoryImpl(database: MeloDatabase) : FavoritesRepository {

    private val queries = database.favoritesQueries

    override fun getFavorites(): Flow<List<Track>> =
        queries.selectAllFavorites()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toTrack() } }

    override suspend fun addFavorite(track: Track) = withContext(Dispatchers.IO) {
        queries.insertFavorite(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            duration_ms = track.durationMs,
            artwork_url = track.artworkUrl,
            source_id = track.sourceId,
            added_at = System.currentTimeMillis(),
        )
    }

    override suspend fun removeFavorite(trackId: String) = withContext(Dispatchers.IO) {
        queries.deleteFavorite(trackId)
    }

    override suspend fun isFavorite(trackId: String): Boolean = withContext(Dispatchers.IO) {
        queries.isFavorite(trackId).executeAsOne() > 0
    }

    private fun Favorites.toTrack() = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = duration_ms,
        genres = emptyList(),
        artworkUrl = artwork_url,
        sourceId = source_id,
    )
}
