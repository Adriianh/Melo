package com.github.adriianh.core.domain.provider

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult

interface MusicProvider {
    suspend fun search(query: String): List<Track>
    suspend fun searchAlbums(query: String): List<SearchResult.Album> = emptyList()
    suspend fun searchArtists(query: String): List<SearchResult.Artist> = emptyList()
    suspend fun searchPlaylists(query: String): List<SearchResult.Playlist> = emptyList()

    suspend fun searchAll(query: String): List<Track> = search(query)
    suspend fun searchAllAlbums(query: String): List<SearchResult.Album> = searchAlbums(query)
    suspend fun searchAllArtists(query: String): List<SearchResult.Artist> = searchArtists(query)
    suspend fun searchAllPlaylists(query: String): List<SearchResult.Playlist> = searchPlaylists(query)
    suspend fun getTrack(id: String): Track?

    suspend fun getAlbumDetails(id: String): SearchResult.Album? = null
    suspend fun getArtistDetails(id: String): SearchResult.Artist? = null
    suspend fun getPlaylistDetails(id: String): SearchResult.Playlist? = null
}