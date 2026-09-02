package com.github.adriianh.core.domain.usecase.offline

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.platform.PlatformFileSystem

/**
 * Use case to scan local directories for music files.
 */
class ScanLocalTracksUseCase(private val offlineRepository: OfflineRepository) {
    suspend operator fun invoke(paths: List<String> = emptyList()): List<Track> {
        val allPaths = paths.ifEmpty {
            PlatformFileSystem.getDefaultMusicPaths()
        }
        return offlineRepository.scanLocalTracks(allPaths)
    }
}