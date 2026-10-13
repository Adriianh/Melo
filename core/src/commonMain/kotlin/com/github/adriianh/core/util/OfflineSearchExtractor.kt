package com.github.adriianh.core.util

import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult

fun buildArtistSectionsFromTracks(
    artistName: String,
    matchingTracks: List<Track>,
): List<SearchResult.ArtistSection> {
    val albumGroups =
        matchingTracks
            .filter { it.album.isNotBlank() && it.album != "Unknown" }
            .groupBy { it.album }

    val sections = mutableListOf<SearchResult.ArtistSection>()
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
        sections.add(
            SearchResult.ArtistSection(
                title = "Álbumes en tus descargas",
                items = albumCards,
            ),
        )
    }
    return sections
}

fun fuzzyMatch(
    token: String,
    text: String,
): Boolean {
    var i = 0
    for (c in text) {
        if (i < token.length && c == token[i]) i++
    }
    return i == token.length
}

fun extractOfflineSongs(
    tracks: List<Track>,
    query: String,
): List<Track> {
    val queryTokens = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (queryTokens.isEmpty()) return emptyList()

    return tracks
        .filter { track ->
            val title = track.title.lowercase()
            val artist = track.artist.lowercase()
            val album = track.album.lowercase()
            track.title.contains(query, ignoreCase = true) ||
                track.artist.contains(query, ignoreCase = true) ||
                track.album.contains(query, ignoreCase = true) ||
                queryTokens.all { token ->
                    token in title ||
                        token in artist ||
                        token in album ||
                        fuzzyMatch(token, title) ||
                        fuzzyMatch(token, artist) ||
                        fuzzyMatch(token, album)
                }
        }.distinctBy { it.id }
}

fun extractOfflineAlbums(
    tracks: List<Track>,
    query: String,
): List<SearchResult.Album> {
    val queryTokens = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (queryTokens.isEmpty()) return emptyList()

    return tracks
        .filter { track ->
            if (track.album.isBlank()) return@filter false
            val album = track.album.lowercase()
            val artist = track.artist.lowercase()
            track.album.contains(query, ignoreCase = true) ||
                track.artist.contains(query, ignoreCase = true) ||
                queryTokens.all { token ->
                    token in album || token in artist || fuzzyMatch(token, album)
                }
        }.groupBy { it.album }
        .map { (albumTitle, albumTracks) ->
            SearchResult.Album(
                id = albumTitle,
                title = albumTitle,
                author = albumTracks.first().artist,
                year = null,
                artworkUrl = albumTracks.firstOrNull { it.artworkUrl != null }?.artworkUrl,
                songs = albumTracks,
            )
        }
}

fun extractOfflineArtists(
    tracks: List<Track>,
    query: String,
): List<SearchResult.Artist> {
    val queryTokens = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (queryTokens.isEmpty()) return emptyList()

    return tracks
        .filter { track ->
            if (track.artist.isBlank()) return@filter false
            val artist = track.artist.lowercase()
            track.artist.contains(query, ignoreCase = true) ||
                queryTokens.all { token ->
                    token in artist || fuzzyMatch(token, artist)
                }
        }.groupBy { it.artist }
        .map { (artistName, artistTracks) ->
            SearchResult.Artist(
                id = artistName,
                name = artistName,
                artworkUrl = artistTracks.firstOrNull { it.artworkUrl != null }?.artworkUrl,
                topSongs = artistTracks,
                sections = buildArtistSectionsFromTracks(artistName, artistTracks),
            )
        }
}

fun extractOfflinePlaylists(
    localPlaylists: List<Playlist>,
    query: String,
    remotePlaylists: List<SearchResult.Playlist> = emptyList(),
    offlineTracks: List<OfflineTrack> = emptyList(),
): List<SearchResult.Playlist> {
    val queryTokens = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (queryTokens.isEmpty()) return emptyList()

    val matchingLocal =
        localPlaylists
            .filter { playlist ->
                val name = playlist.name.lowercase()
                playlist.name.contains(query, ignoreCase = true) ||
                    queryTokens.all { token -> token in name || fuzzyMatch(token, name) }
            }.map { playlist ->
                SearchResult.Playlist(
                    id = "local:${playlist.id}",
                    title = playlist.name,
                    author = "Local",
                    trackCount = playlist.trackCount,
                    artworkUrl = playlist.artworks.firstOrNull(),
                    songs = null,
                )
            }

    val matchingRemote =
        remotePlaylists
            .filter { playlist ->
                val title = playlist.title.lowercase()
                val author = playlist.author.lowercase()
                val matches =
                    playlist.title.contains(query, ignoreCase = true) ||
                        author.contains(query, ignoreCase = true) ||
                        queryTokens.all { token ->
                            token in title || token in author || fuzzyMatch(token, title)
                        }
                matches && isEntityAvailableOffline(playlist, offlineTracks)
            }

    return (matchingLocal + matchingRemote).distinctBy { it.id }
}
