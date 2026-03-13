package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.OfflineTrack
import kotlinx.coroutines.flow.Flow

interface OfflineRepository {
    /**
     * Returns a flow of all downloaded tracks.
     */
    fun getOfflineTracksFlow(): Flow<List<OfflineTrack>>

    /**
     * Gets a snapshot of all offline tracks.
     */
    suspend fun getOfflineTracks(): List<OfflineTrack>

    /**
     * Checks if a specific track is available offline.
     */
    suspend fun getOfflineTrack(trackId: String): OfflineTrack?

    /**
     * Adds a track to the download queue or updates its status.
     */
    suspend fun saveOfflineTrack(offlineTrack: OfflineTrack)

    /**
     * Removes a track from local storage.
     */
    suspend fun removeOfflineTrack(trackId: String)

    /**
     * Updates the last accessed timestamp for a track.
     */
    suspend fun markTrackAsAccessed(trackId: String)

    /**
     * Deletes tracks to stay under the size limit, prioritizing oldest accessed.
     */
    suspend fun cleanupCache(maxSizeMb: Int)

    /**
     * Synchronizes the metadata with actual files on disk.
     * Updates tracks to COMPLETED if their files exist, or removes entries if files are missing.
     */
    suspend fun syncWithFileSystem()
}