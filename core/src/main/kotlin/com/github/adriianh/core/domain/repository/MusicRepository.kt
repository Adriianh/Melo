package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.Track

interface MusicRepository {
    suspend fun search(query: String): List<Track>
    suspend fun loadMore(query: String, offset: Int): List<Track>
    suspend fun getTrack(id: String): Track?
}