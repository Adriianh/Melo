package com.github.adriianh.core.domain.usecase.library

import com.github.adriianh.core.domain.model.FavoriteEntity
import com.github.adriianh.core.domain.model.FavoriteEntityType
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

class GetFavoriteEntitiesUseCase(private val repository: FavoritesRepository) {
    operator fun invoke(type: FavoriteEntityType? = null): Flow<List<FavoriteEntity>> =
        repository.getFavoriteEntities(type)
}

class AddFavoriteEntityUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(entity: FavoriteEntity) = repository.addFavoriteEntity(entity)
}

class RemoveFavoriteEntityUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(entityId: String) = repository.removeFavoriteEntity(entityId)
}

class IsFavoriteEntityUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(entityId: String): Boolean = repository.isFavoriteEntity(entityId)
}

class ToggleFavoriteEntityUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(entity: FavoriteEntity): Boolean {
        return if (repository.isFavoriteEntity(entity.id)) {
            repository.removeFavoriteEntity(entity.id)
            false
        } else {
            repository.addFavoriteEntity(entity)
            true
        }
    }
}