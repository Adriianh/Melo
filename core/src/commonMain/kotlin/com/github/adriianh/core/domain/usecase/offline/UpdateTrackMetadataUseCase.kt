package com.github.adriianh.core.domain.usecase.offline

import com.github.adriianh.core.domain.repository.OfflineRepository

/**
 * Use case to update metadata of a local track.
 */
class UpdateTrackMetadataUseCase(private val offlineRepository: OfflineRepository) {
    suspend operator fun invoke(
        trackId: String,
        title: String? = null,
        artist: String? = null,
        album: String? = null
    ) = offlineRepository.updateTrackMetadata(trackId, title, artist, album)
}
