package com.github.adriianh.core.domain.provider

import com.github.adriianh.core.domain.model.Track

interface MusicProvider {
    suspend fun search(query: String): List<Track>
    suspend fun getTrack(id: String): Track?
}