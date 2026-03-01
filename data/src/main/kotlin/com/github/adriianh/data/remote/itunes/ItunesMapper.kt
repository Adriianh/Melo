package com.github.adriianh.data.remote.itunes

import com.github.adriianh.core.domain.model.Track

fun ItunesTrackDto.toDomain(): Track = Track(
    id = "itunes:$trackId",
    title = trackName,
    artist = artistName,
    album = collectionName ?: "Unknown",
    durationMs = trackTimeMillis ?: 0L,
    genres = if (primaryGenreName != null) listOf(primaryGenreName) else emptyList(),
    artworkUrl = artworkUrl100?.replace("100x100", "600x600"),
    sourceId = null
)

