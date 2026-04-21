package com.github.adriianh.data.remote.spotify

import com.github.adriianh.core.domain.model.Track

fun SpotifyTrackDto.toDomain(): Track = Track(
    id = id,
    title = name,
    artist = artists.firstOrNull()?.name ?: "Unknown",
    album = album?.name ?: "Unknown",
    durationMs = durationMs.toLong(),
    genres = emptyList(),
    artworkUrl = album?.images?.firstOrNull()?.url,
    sourceId = null
)