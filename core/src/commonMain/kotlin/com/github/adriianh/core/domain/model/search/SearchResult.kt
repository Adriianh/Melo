package com.github.adriianh.core.domain.model.search

import com.github.adriianh.core.domain.model.Track

sealed interface SearchResult {
    data class Song(val track: Track) : SearchResult
    data class Album(
        val id: String,
        val title: String,
        val author: String,
        val year: String?,
        val artworkUrl: String?,
        val songs: List<Track>? = null,
        val description: String? = null,
        val otherVersions: List<Album>? = null
    ) : SearchResult
    data class ArtistSection(
        val title: String,
        val items: List<SearchResult>
    )

    data class Artist(
        val id: String,
        val name: String,
        val artworkUrl: String?,
        val description: String? = null,
        val subscriberCountText: String? = null,
        val monthlyListenerCount: String? = null,
        val topSongs: List<Track>? = null,
        val sections: List<ArtistSection> = emptyList()
    ) : SearchResult
    data class Playlist(
        val id: String,
        val title: String,
        val author: String,
        val trackCount: Int?,
        val artworkUrl: String?,
        val songs: List<Track>? = null,
        val description: String? = null
    ) : SearchResult
}