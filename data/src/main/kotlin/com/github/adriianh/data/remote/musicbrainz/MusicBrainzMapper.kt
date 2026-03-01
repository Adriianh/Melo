package com.github.adriianh.data.remote.musicbrainz

import com.github.adriianh.core.domain.model.Track

fun MusicBrainzRecording.toDomain(artworkUrl: String? = null): Track = Track(
    id = "mb:$id",
    title = title,
    artist = artistCredit.firstOrNull()?.name ?: "Unknown",
    album = releases.firstOrNull()?.title ?: "Unknown",
    durationMs = length ?: 0L,
    genres = emptyList(),
    artworkUrl = artworkUrl,
    sourceId = null
)
