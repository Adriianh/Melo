package com.github.adriianh.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.github.adriianh.core.domain.model.FavoriteEntity
import com.github.adriianh.core.domain.model.FavoriteEntityType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.FavoritesRepository
import com.github.adriianh.core.platform.currentTimeSeconds
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.data.local.Favorite_entities
import com.github.adriianh.data.local.Favorites
import com.github.adriianh.data.local.MeloDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FavoritesRepositoryImpl(database: MeloDatabase) : FavoritesRepository {

    private val queries = database.favoritesQueries
    private val entityQueries = database.favoriteEntitiesQueries

    override fun getFavorites(): Flow<List<Track>> =
        queries.selectAllFavorites()
            .asFlow()
            .mapToList(MeloDispatchers.IO)
            .map { rows -> rows.map { it.toTrack() } }

    override suspend fun addFavorite(track: Track) {
        withContext(MeloDispatchers.IO) {
            queries.insertFavorite(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                duration_ms = track.durationMs,
                artwork_url = track.artworkUrl,
                source_id = track.sourceId,
                added_at = currentTimeSeconds() * 1000L,
            )
        }
    }

    override suspend fun removeFavorite(trackId: String) {
        withContext(MeloDispatchers.IO) {
            queries.deleteFavorite(trackId)
            val altId =
                if (trackId.startsWith("piped:")) trackId.removePrefix("piped:") else "piped:$trackId"
            queries.deleteFavorite(altId)
        }
    }

    override suspend fun isFavorite(trackId: String): Boolean = withContext(MeloDispatchers.IO) {
        val altId =
            if (trackId.startsWith("piped:")) trackId.removePrefix("piped:") else "piped:$trackId"
        queries.isFavorite(trackId).executeAsOne() > 0 || queries.isFavorite(altId)
            .executeAsOne() > 0
    }

    override fun getFavoriteEntities(type: FavoriteEntityType?): Flow<List<FavoriteEntity>> {
        val query = if (type == null) {
            entityQueries.selectAllFavoriteEntities()
        } else {
            entityQueries.selectFavoriteEntitiesByType(type.name)
        }
        return query
            .asFlow()
            .mapToList(MeloDispatchers.IO)
            .map { rows -> rows.map { it.toFavoriteEntity() } }
    }

    override suspend fun addFavoriteEntity(entity: FavoriteEntity) {
        withContext(MeloDispatchers.IO) {
            entityQueries.insertFavoriteEntity(
                id = entity.id,
                type = entity.type.name,
                title = entity.title,
                subtitle = entity.subtitle,
                artwork_url = entity.artworkUrl,
                track_count = entity.trackCount?.toLong(),
                added_at = if (entity.addedAt > 0L) entity.addedAt else currentTimeSeconds() * 1000L,
            )
        }
    }

    override suspend fun removeFavoriteEntity(entityId: String) {
        withContext(MeloDispatchers.IO) {
            entityQueries.deleteFavoriteEntity(entityId)
        }
    }

    override suspend fun isFavoriteEntity(entityId: String): Boolean =
        withContext(MeloDispatchers.IO) {
            entityQueries.isFavoriteEntity(entityId).executeAsOne() > 0
        }

    private fun Favorite_entities.toFavoriteEntity() = FavoriteEntity(
        id = id,
        type = runCatching { FavoriteEntityType.valueOf(type) }.getOrDefault(FavoriteEntityType.ALBUM),
        title = title,
        subtitle = subtitle,
        artworkUrl = artwork_url,
        trackCount = track_count?.toInt(),
        addedAt = added_at,
    )

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