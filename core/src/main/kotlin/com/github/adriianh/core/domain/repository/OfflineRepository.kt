package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
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
}