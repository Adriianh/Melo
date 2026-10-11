package com.github.adriianh.melo.ui.detail

import com.github.adriianh.core.domain.cache.EntityCache
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.model.search.entityId
import com.github.adriianh.core.domain.repository.OfflineRepository

internal fun cleanTrackId(id: String): String =
    id
        .removePrefix("piped:")
        .removePrefix("local:")
        .removePrefix("file:")
        .removePrefix("VL")
        .removePrefix("spotify:")
        .removePrefix("itunes:")

internal fun normalizeTrackTitle(title: String): String =
    title
        .lowercase()
        .replace(Regex("""\s*[(\[{].*?[)\]}]"""), "")
        .replace(Regex("""\s*-\s*(official|audio|video|remastered|lyrics).*"""), "")
        .trim()

internal fun buildCompletedTrackLookups(completedOffline: List<OfflineTrack>): Pair<Set<String>, Set<String>> {
    val completedKeys = mutableSetOf<String>()
    val completedSignatures = mutableSetOf<String>()
    for ((track) in completedOffline) {
        completedKeys.add(track.id)
        val cleanId = cleanTrackId(track.id)
        completedKeys.add(cleanId)
        completedKeys.add("piped:$cleanId")
        track.sourceId?.let {
            completedKeys.add(it)
            completedKeys.add(cleanTrackId(it))
        }
        val normTitle = normalizeTrackTitle(track.title)
        val normArtist = track.artist.trim().lowercase()
        completedSignatures.add("$normTitle:::$normArtist")
    }
    return completedKeys to completedSignatures
}

internal fun isSongCompleted(
    song: Track,
    completedKeys: Set<String>,
    completedSignatures: Set<String>,
): Boolean {
    val cleanSongId = cleanTrackId(song.id)
    val cleanSourceId = song.sourceId?.let { cleanTrackId(it) }
    val normSongTitle = normalizeTrackTitle(song.title)
    val normSongArtist = song.artist.trim().lowercase()
    val songSig = "$normSongTitle:::$normSongArtist"

    return song.id in completedKeys ||
        cleanSongId in completedKeys ||
        (song.sourceId != null && song.sourceId in completedKeys) ||
        (cleanSourceId != null && cleanSourceId in completedKeys) ||
        songSig in completedSignatures ||
        completedSignatures.any { sig ->
            val parts = sig.split(":::")
            parts.size == 2 &&
                normSongTitle.isNotBlank() &&
                parts[0] == normSongTitle &&
                (normSongArtist.contains(parts[1]) || parts[1].contains(normSongArtist))
        }
}

internal fun computeOverallDownloadProgress(
    songs: List<Track>,
    completedTrackIds: Set<String>,
    activeDownloads: Map<String, Float>,
): Float =
    if (songs.isNotEmpty()) {
        val totalTrackProgress =
            songs.sumOf { track ->
                val cleanId = cleanTrackId(track.id)
                when {
                    track.id in completedTrackIds || cleanId in completedTrackIds -> 1.0
                    track.id in activeDownloads -> activeDownloads[track.id]?.toDouble() ?: 0.0
                    cleanId in activeDownloads -> activeDownloads[cleanId]?.toDouble() ?: 0.0
                    else -> 0.0
                }
            }
        (totalTrackProgress / songs.size).toFloat()
    } else {
        0f
    }

internal suspend fun resolveOfflineEntity(
    initial: SearchResult,
    entityCache: EntityCache?,
    offlineRepository: OfflineRepository,
): SearchResult? {
    val cached = entityCache?.get(initial.entityId)
    if (cached != null) return cached

    val allOffline = offlineRepository.getOfflineTracks()
    return when (initial) {
        is SearchResult.Album -> resolveOfflineAlbum(initial, allOffline)
        is SearchResult.Artist -> resolveOfflineArtist(initial, allOffline)
        else -> null
    }
}

private fun resolveOfflineAlbum(
    initial: SearchResult.Album,
    allOffline: List<OfflineTrack>,
): SearchResult.Album? {
    val albumQuery = initial.title.ifBlank { initial.id }
    val hasAuthor = initial.author.isNotBlank() && initial.author != "Unknown"
    val matchingTracks =
        allOffline
            .filter { it.downloadStatus == DownloadStatus.COMPLETED }
            .map { it.track }
            .filter {
                val matchesAlbum =
                    albumQuery.isNotBlank() &&
                        it.album.equals(
                            albumQuery,
                            ignoreCase = true,
                        )
                val matchesAuthor =
                    hasAuthor && it.artist.equals(initial.author, ignoreCase = true)

                if (albumQuery.isNotBlank() && hasAuthor) {
                    matchesAlbum && matchesAuthor
                } else if (albumQuery.isNotBlank()) {
                    matchesAlbum
                } else if (hasAuthor) {
                    matchesAuthor
                } else {
                    false
                }
            }
    if (matchingTracks.isEmpty()) return null
    return initial.copy(
        title = initial.title.ifBlank { matchingTracks.first().album },
        songs = matchingTracks,
        artworkUrl = initial.artworkUrl ?: matchingTracks.firstOrNull()?.artworkUrl,
        author = initial.author.ifBlank { matchingTracks.first().artist },
    )
}

private fun resolveOfflineArtist(
    initial: SearchResult.Artist,
    allOffline: List<OfflineTrack>,
): SearchResult.Artist? {
    val artistQuery = initial.name.ifBlank { initial.id }
    val matchingTracks =
        allOffline
            .filter { it.downloadStatus == DownloadStatus.COMPLETED }
            .map { it.track }
            .filter { it.artist.contains(artistQuery, ignoreCase = true) }
    if (matchingTracks.isEmpty()) return null
    return initial.copy(
        name = initial.name.ifBlank { matchingTracks.first().artist },
        topSongs = matchingTracks,
        artworkUrl = initial.artworkUrl ?: matchingTracks.firstOrNull()?.artworkUrl,
    )
}
