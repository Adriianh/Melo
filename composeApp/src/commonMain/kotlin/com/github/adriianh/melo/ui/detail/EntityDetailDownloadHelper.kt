package com.github.adriianh.melo.ui.detail

import com.github.adriianh.core.domain.cache.EntityCache
import com.github.adriianh.core.domain.cache.LibraryCache
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
    libraryCache: LibraryCache? = null,
): SearchResult? {
    val cached = entityCache?.get(initial.entityId)
    if (cached != null) return cached

    val allOffline = offlineRepository.getOfflineTracks()
    return when (initial) {
        is SearchResult.Album -> resolveOfflineAlbum(initial, allOffline)
        is SearchResult.Artist -> resolveOfflineArtist(initial, allOffline, libraryCache)
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

internal fun findMatchingOfflineArtistTracks(
    artist: SearchResult.Artist,
    offlineTracks: List<OfflineTrack>,
    initialArtistId: String = "",
): List<Track> {
    val cleanArtistName = artist.name.trim().lowercase()
    val cleanInitialId = initialArtistId.trim()
    return offlineTracks
        .filter { it.downloadStatus == DownloadStatus.COMPLETED }
        .map { it.track }
        .filter { track ->
            val trackArtist = track.artist.trim().lowercase()
            val matchesName =
                cleanArtistName.isNotBlank() &&
                    (trackArtist.contains(cleanArtistName) || cleanArtistName.contains(trackArtist))
            val matchesId =
                cleanInitialId.isNotBlank() &&
                    track.artist.contains(cleanInitialId, ignoreCase = true)
            matchesName || matchesId
        }
}

private suspend fun resolveOfflineArtist(
    initial: SearchResult.Artist,
    allOffline: List<OfflineTrack>,
    libraryCache: LibraryCache? = null,
): SearchResult.Artist? {
    val cachedArtist =
        if (initial.name.isBlank()) {
            libraryCache?.get()?.artists?.firstOrNull { it.id == initial.id }
        } else {
            null
        }
    val effectiveName = initial.name.ifBlank { cachedArtist?.name.orEmpty() }
    val effectiveArtwork = initial.artworkUrl ?: cachedArtist?.artworkUrl
    val artistToMatch = initial.copy(name = effectiveName, artworkUrl = effectiveArtwork)

    val matchingTracks =
        findMatchingOfflineArtistTracks(
            artist = artistToMatch,
            offlineTracks = allOffline,
            initialArtistId = initial.id,
        )
    if (matchingTracks.isEmpty()) return null

    val artistName = effectiveName.ifBlank { matchingTracks.first().artist }
    val finalArtwork = effectiveArtwork ?: matchingTracks.firstOrNull { it.artworkUrl != null }?.artworkUrl

    val albumGroups =
        matchingTracks
            .filter { it.album.isNotBlank() && it.album != "Unknown" }
            .groupBy { it.album }

    val sections =
        if (albumGroups.isNotEmpty()) {
            val albumCards =
                albumGroups.map { (albumTitle, tracks) ->
                    SearchResult.Album(
                        id = albumTitle,
                        title = albumTitle,
                        author = artistName,
                        year = null,
                        artworkUrl = tracks.firstOrNull { it.artworkUrl != null }?.artworkUrl,
                        songs = tracks,
                    )
                }
            listOf(
                SearchResult.ArtistSection(
                    title = "Álbumes en tus descargas",
                    items = albumCards,
                ),
            )
        } else {
            emptyList()
        }

    return initial.copy(
        name = artistName,
        topSongs = matchingTracks,
        artworkUrl = finalArtwork,
        sections = sections,
    )
}
