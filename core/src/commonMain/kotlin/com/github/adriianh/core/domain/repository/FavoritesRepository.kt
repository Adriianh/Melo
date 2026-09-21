package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.FavoriteEntity
import com.github.adriianh.core.domain.model.FavoriteEntityType
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getFavorites(): Flow<List<Track>>
    suspend fun addFavorite(track: Track)
    suspend fun removeFavorite(trackId: String)
    suspend fun isFavorite(trackId: String): Boolean

    fun getFavoriteEntities(type: FavoriteEntityType? = null): Flow<List<FavoriteEntity>>
    suspend fun addFavoriteEntity(entity: FavoriteEntity)
    suspend fun removeFavoriteEntity(entityId: String)
    suspend fun isFavoriteEntity(entityId: String): Boolean
}