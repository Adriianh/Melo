package com.github.adriianh.core.util

import com.github.adriianh.core.domain.cache.EntityCache
import com.github.adriianh.core.domain.cache.LibraryCache
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.FavoriteEntity
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.model.search.entityId
import com.github.adriianh.core.domain.model.toSearchResult
import com.github.adriianh.core.domain.repository.OfflineRepository

fun cleanTrackId(id: String): String =
    id
        .removePrefix("piped:")
        .removePrefix("local:")
        .removePrefix("file:")
        .removePrefix("VL")
        .removePrefix("spotify:")
        .removePrefix("itunes:")

fun normalizeTrackTitle(title: String): String =
    title
        .lowercase()
        .replace(Regex("""\s*[(\[{].*?[)\]}]"""), "")
        .replace(Regex("""\s*-\s*(official|audio|video|remastered|lyrics).*"""), "")
        .trim()

fun buildCompletedTrackLookups(completedOffline: List<OfflineTrack>): Pair<Set<String>, Set<String>> {
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

fun isSongCompleted(
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

fun findMatchingOfflineArtistTracks(
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

fun resolveOfflineAlbum(
    initial: SearchResult.Album,
    allOffline: List<OfflineTrack>,
): SearchResult.Album? {
    if (!initial.songs.isNullOrEmpty()) return initial

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
    return if (matchingTracks.isNotEmpty()) {
        initial.copy(
            title = initial.title.ifBlank { matchingTracks.first().album },
            songs = matchingTracks,
            artworkUrl = initial.artworkUrl ?: matchingTracks.firstOrNull()?.artworkUrl,
            author = initial.author.ifBlank { matchingTracks.first().artist },
        )
    } else {
        null
    }
}

fun resolveOfflineArtist(
    initial: SearchResult.Artist,
    allOffline: List<OfflineTrack>,
    cachedArtistName: String? = null,
    cachedArtworkUrl: String? = null,
): SearchResult.Artist? {
    val effectiveName = initial.name.ifBlank { cachedArtistName.orEmpty() }
    val effectiveArtwork = initial.artworkUrl ?: cachedArtworkUrl
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

    val sections = buildArtistSectionsFromTracks(artistName, matchingTracks)

    return initial.copy(
        name = artistName,
        topSongs = matchingTracks,
        artworkUrl = finalArtwork,
        sections = sections,
    )
}

suspend fun resolveOfflineEntity(
    initial: SearchResult,
    entityCache: EntityCache?,
    offlineRepository: OfflineRepository,
    libraryCache: LibraryCache? = null,
): SearchResult? {
    val cached = entityCache?.get(initial.entityId)
    if (cached != null) return cached

    val allOffline = offlineRepository.getOfflineTracks()
    val cachedArtist =
        if (initial is SearchResult.Artist && initial.name.isBlank()) {
            libraryCache?.get()?.artists?.firstOrNull { it.id == initial.id }
        } else {
            null
        }

    return when (initial) {
        is SearchResult.Album -> resolveOfflineAlbum(initial, allOffline)
        is SearchResult.Artist ->
            resolveOfflineArtist(
                initial = initial,
                allOffline = allOffline,
                cachedArtistName = cachedArtist?.name,
                cachedArtworkUrl = cachedArtist?.artworkUrl,
            )
        else -> null
    }
}

fun resolveOfflineEntitySync(
    initial: SearchResult,
    entityCache: EntityCache?,
    allOffline: List<OfflineTrack>,
    libraryCache: LibraryCache? = null,
): SearchResult? {
    val cached = entityCache?.getSync(initial.entityId)
    if (cached != null) return cached

    val cachedArtist =
        if (initial is SearchResult.Artist && initial.name.isBlank()) {
            libraryCache?.getSync()?.artists?.firstOrNull { it.id == initial.id }
        } else {
            null
        }

    return when (initial) {
        is SearchResult.Album -> resolveOfflineAlbum(initial, allOffline)
        is SearchResult.Artist ->
            resolveOfflineArtist(
                initial = initial,
                allOffline = allOffline,
                cachedArtistName = cachedArtist?.name,
                cachedArtworkUrl = cachedArtist?.artworkUrl,
            )
        else -> null
    }
}

fun isEntityAvailableOffline(
    entity: SearchResult,
    offlineTracks: List<OfflineTrack>,
    entityCache: EntityCache? = null,
): Boolean =
    when (entity) {
        is SearchResult.Album -> {
            resolveOfflineAlbum(entity, offlineTracks) != null ||
                (entityCache?.getSync(entity.id) as? SearchResult.Album)?.songs?.isNotEmpty() == true
        }

        is SearchResult.Artist -> {
            findMatchingOfflineArtistTracks(entity, offlineTracks, entity.id).isNotEmpty() ||
                (entityCache?.getSync(entity.id) as? SearchResult.Artist)?.topSongs?.isNotEmpty() == true
        }

        is SearchResult.Playlist -> {
            val isLocal = entity.id.startsWith("local:")
            val hasCachedSongs =
                (entityCache?.getSync(entity.id) as? SearchResult.Playlist)?.songs?.isNotEmpty() == true
            val hasDownloadedSongs =
                entity.songs?.any { song ->
                    val (keys, sigs) = buildCompletedTrackLookups(offlineTracks)
                    isSongCompleted(song, keys, sigs)
                } == true
            isLocal || hasCachedSongs || hasDownloadedSongs
        }

        is SearchResult.Song -> {
            val (keys, sigs) = buildCompletedTrackLookups(offlineTracks)
            isSongCompleted(entity.track, keys, sigs)
        }
    }

fun isFavoriteEntityAvailableOffline(
    entity: FavoriteEntity,
    offlineTracks: List<OfflineTrack>,
    entityCache: EntityCache? = null,
): Boolean = isEntityAvailableOffline(entity.toSearchResult(), offlineTracks, entityCache)
