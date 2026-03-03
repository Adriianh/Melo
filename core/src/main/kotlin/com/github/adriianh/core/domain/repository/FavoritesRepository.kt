package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getFavorites(): Flow<List<Track>>
    suspend fun addFavorite(track: Track)
    suspend fun removeFavorite(trackId: String)
    suspend fun isFavorite(trackId: String): Boolean
}

