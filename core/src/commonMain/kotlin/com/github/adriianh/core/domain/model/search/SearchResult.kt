package com.github.adriianh.core.domain.model.search

import com.github.adriianh.core.domain.model.Track
import kotlinx.serialization.Serializable

@Serializable
sealed interface SearchResult {
    @Serializable
    data class Song(val track: Track) : SearchResult

    @Serializable
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

    @Serializable
    data class ArtistSection(
        val title: String,
        val items: List<SearchResult>
    )

    @Serializable
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

    @Serializable
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

val SearchResult.entityId: String
    get() = when (this) {
        is SearchResult.Song -> track.id
        is SearchResult.Album -> id
        is SearchResult.Artist -> id
        is SearchResult.Playlist -> id
    }

fun SearchResult.itemKey(): String = when (this) {
    is SearchResult.Song -> "song_${track.id}"
    is SearchResult.Album -> "album_$id"
    is SearchResult.Artist -> "artist_$id"
    is SearchResult.Playlist -> "playlist_$id"
}