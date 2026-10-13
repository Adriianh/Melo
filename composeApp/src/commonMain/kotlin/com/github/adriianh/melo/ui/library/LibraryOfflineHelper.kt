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
            val cleanId =
                track.id
                    .removePrefix("piped:")
                    .removePrefix("local:")
                    .removePrefix("file:")
            add(cleanId)
            add("piped:$cleanId")
            track.sourceId?.let {
                add(it)
                add(it.removePrefix("piped:"))
            }
        }
    }

private fun isTrackOffline(
    track: Track,
    allOfflineTrackIds: Set<String>,
): Boolean =
    track.id in allOfflineTrackIds ||
        track.id.removePrefix("piped:") in allOfflineTrackIds ||
        (track.sourceId != null && track.sourceId in allOfflineTrackIds)

internal fun computeOfflineAlbumIds(
    albums: List<SearchResult.Album>,
    allOfflineTracks: List<Track>,
    entityCache: EntityCache?,
): Set<String> =
    buildSet {
        val offlineTrackIds = allOfflineTracks.map { it.id }.toSet()
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
            val hasCachedTracks = cached?.songs?.any { isTrackOffline(it, offlineTrackIds) } == true
            if (hasTracks || hasCachedTracks) {
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
        val offlineTrackIds = allOfflineTracks.map { it.id }.toSet()
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
            val hasCachedTracks = cached?.topSongs?.any { isTrackOffline(it, offlineTrackIds) } == true
            if (hasTracks || hasCachedTracks) {
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
            val hasTracks = cached?.songs?.any { isTrackOffline(it, allOfflineTrackIds) } == true
            if (hasTracks) {
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
            if (tracks != null && tracks.any { isTrackOffline(it, allOfflineTrackIds) }) {
                add(id)
            }
        }
    }
