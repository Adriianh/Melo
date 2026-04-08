package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult

interface MusicRepository {
    suspend fun search(query: String): List<Track>
    suspend fun searchAlbums(query: String): List<SearchResult.Album>
    suspend fun searchArtists(query: String): List<SearchResult.Artist>
    suspend fun searchPlaylists(query: String): List<SearchResult.Playlist>

    suspend fun loadMore(query: String, offset: Int): List<Track>
    fun hasMore(offset: Int): Boolean
    suspend fun getTrack(id: String): Track?
}