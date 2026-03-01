package com.github.adriianh.core.domain.provider

import com.github.adriianh.core.domain.model.Track

/**
 * Interface for music providers that support pagination.
 */
interface PaginableMusicProvider : MusicProvider {
    /**
     * Searches for a specific page of tracks.
     * @param query The search query.
     * @param offset The number of tracks to skip.
     * @return A list of tracks for the requested page.
     */
    suspend fun searchPage(query: String, offset: Int): List<Track>

    /**
     * Checks if there are more tracks available for the current search.
     * @param offset The current offset.
     * @return True if more tracks are available, false otherwise.
     */
    fun hasMore(offset: Int): Boolean
}
