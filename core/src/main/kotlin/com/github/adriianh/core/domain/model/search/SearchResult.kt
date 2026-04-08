package com.github.adriianh.core.domain.model.search

import com.github.adriianh.core.domain.model.Track

sealed interface SearchResult {
    data class Song(val track: Track) : SearchResult
    data class Album(val id: String, val title: String, val author: String, val year: String?, val artworkUrl: String?) : SearchResult
    data class Artist(val id: String, val name: String, val artworkUrl: String?) : SearchResult
    data class Playlist(val id: String, val title: String, val author: String, val trackCount: Int?, val artworkUrl: String?) : SearchResult
}