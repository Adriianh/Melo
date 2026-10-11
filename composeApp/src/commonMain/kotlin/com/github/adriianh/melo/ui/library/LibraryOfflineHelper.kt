package com.github.adriianh.melo.ui.library

import com.github.adriianh.core.domain.cache.EntityCache
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult

internal fun computeOfflineTrackIds(downloadedTracks: List<OfflineTrack>): Set<String> =
    buildSet {
        for ((track) in downloadedTracks) {
            add(track.id)
            add(track.id.removePrefix("piped:"))
            track.sourceId?.let { add(it) }
        }
    }

internal fun computeOfflineAlbumIds(
    albums: List<SearchResult.Album>,
    allOfflineTracks: List<Track>,
    entityCache: EntityCache?,
): Set<String> =
    buildSet {
        for ((id, title, albumAuthor) in albums) {
            val albumTitle = title.trim().lowercase()
            val author =
                albumAuthor
                    .trim()
                    .lowercase()
                    .takeIf { it != "unknown" && it.isNotBlank() }
            val hasTracks =
                allOfflineTracks.any { track ->
                    val tAlbum = track.album.trim().lowercase()
                    val matchesAlbum = tAlbum.isNotBlank() && tAlbum == albumTitle
                    val matchesAuthor =
                        author == null ||
                            track.artist
                                .trim()
                                .lowercase()
                                .contains(author)
                    matchesAlbum && matchesAuthor
                }
            val cached = entityCache?.getSync(id) as? SearchResult.Album
            val hasCache = cached?.songs?.isNotEmpty() == true
            if (hasTracks || hasCache) {
                add(id)
            }
        }
    }

internal fun computeOfflineArtistIds(
    artists: List<SearchResult.Artist>,
    allOfflineTracks: List<Track>,
    entityCache: EntityCache?,
): Set<String> =
    buildSet {
        for ((id, name) in artists) {
            val artistName = name.trim().lowercase()
            val hasTracks =
                artistName.isNotBlank() &&
                    allOfflineTracks.any { track ->
                        track.artist
                            .trim()
                            .lowercase()
                            .contains(artistName)
                    }
            val cached = entityCache?.getSync(id) as? SearchResult.Artist
            val hasCache =
                cached != null &&
                    (cached.topSongs?.isNotEmpty() == true || cached.sections.isNotEmpty())
            if (hasTracks || hasCache) {
                add(id)
            }
        }
    }

internal fun computeOfflineRemotePlaylistIds(
    playlists: List<SearchResult.Playlist>,
    allOfflineTrackIds: Set<String>,
    entityCache: EntityCache?,
): Set<String> =
    buildSet {
        for ((id) in playlists) {
            val cached = entityCache?.getSync(id) as? SearchResult.Playlist
            val hasCache = cached?.songs?.isNotEmpty() == true
            val hasTracks =
                cached?.songs?.any { s ->
                    s.id in allOfflineTrackIds || s.id.removePrefix("piped:") in allOfflineTrackIds
                } == true
            if (hasCache || hasTracks) {
                add(id)
            }
        }
    }

internal fun computeOfflineCustomPlaylistIds(
    customPlaylists: List<Playlist>,
    customPlaylistTracks: Map<Long, List<Track>>,
    allOfflineTrackIds: Set<String>,
): Set<Long> =
    buildSet {
        for ((id) in customPlaylists) {
            val tracks = customPlaylistTracks[id]
            if (tracks != null &&
                tracks.any { it.id in allOfflineTrackIds || it.id.removePrefix("piped:") in allOfflineTrackIds }
            ) {
                add(id)
            }
        }
    }
