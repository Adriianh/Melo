package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.BrowseCategoryResult
import com.github.adriianh.core.domain.model.HomeFeed
import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.MoodAndGenreGroup
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult

interface MusicRepository {
    suspend fun search(query: String): List<Track>
    suspend fun searchAlbums(query: String): List<SearchResult.Album>
    suspend fun searchArtists(query: String): List<SearchResult.Artist>
    suspend fun searchPlaylists(query: String): List<SearchResult.Playlist>

    suspend fun loadMore(query: String, offset: Int): List<Track>
    fun hasMore(offset: Int): Boolean

    suspend fun loadMoreAlbums(query: String, offset: Int): List<SearchResult.Album>
    fun hasMoreAlbums(offset: Int): Boolean

    suspend fun loadMoreArtists(query: String, offset: Int): List<SearchResult.Artist>
    fun hasMoreArtists(offset: Int): Boolean

    suspend fun loadMorePlaylists(query: String, offset: Int): List<SearchResult.Playlist>
    fun hasMorePlaylists(offset: Int): Boolean

    suspend fun getTrack(id: String): Track?

    suspend fun getHome(): List<HomeSection>
    suspend fun getHomeFeed(params: String? = null, continuation: String? = null): HomeFeed
    suspend fun getExplore(): List<HomeSection>
    suspend fun getCharts(): List<HomeSection>
    suspend fun getTrending(): List<Track>
    suspend fun getMoodAndGenres(): List<MoodAndGenreGroup>
    suspend fun getRadio(videoId: String): List<Track>
    suspend fun browseCategory(browseId: String, params: String?): BrowseCategoryResult?
}