package com.github.adriianh.core.domain.usecase.library

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesUseCase(private val repository: FavoritesRepository) {
    operator fun invoke(): Flow<List<Track>> = repository.getFavorites()
}

class AddFavoriteUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(track: Track) = repository.addFavorite(track)
}

class RemoveFavoriteUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(trackId: String) = repository.removeFavorite(trackId)
}

class IsFavoriteUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(trackId: String): Boolean = repository.isFavorite(trackId)
}

