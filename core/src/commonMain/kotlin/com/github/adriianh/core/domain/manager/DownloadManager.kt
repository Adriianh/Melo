package com.github.adriianh.core.domain.manager

import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface DownloadManager {
    /**
     * Map of active track IDs to their download progress (0.0f to 1.0f).
     */
    val activeDownloads: StateFlow<Map<String, Float>>

    /**
     * Downloads a single track.
     * @return true if download succeeded, false otherwise.
     */
    suspend fun downloadTrack(track: Track, customPath: String? = null): Boolean

    /**
     * Auto-caches a track into the temporary LRU cache directory.
     * @return true if caching succeeded, false otherwise.
     */
    suspend fun cacheTrack(track: Track): Boolean

    /**
     * Downloads multiple tracks sequentially in the background.
     */
    suspend fun downloadTracks(tracks: List<Track>, customPath: String? = null)

    /**
     * Cancels an ongoing download for a track.
     */
    suspend fun cancelDownload(trackId: String)

    /**
     * Deletes a downloaded track from storage and database.
     */
    suspend fun deleteDownload(trackId: String)

    /**
     * Checks if a track is completed and available locally.
     */
    suspend fun isDownloaded(trackId: String): Boolean
}