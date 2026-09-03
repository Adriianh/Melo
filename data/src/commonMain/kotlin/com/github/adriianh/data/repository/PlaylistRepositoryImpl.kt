
package com.github.adriianh.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.PlaylistRepository
import com.github.adriianh.core.platform.currentTimeSeconds
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.data.local.MeloDatabase
import com.github.adriianh.data.local.Playlist_tracks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PlaylistRepositoryImpl(database: MeloDatabase) : PlaylistRepository {

    private val playlistsQueries = database.playlistsQueries
    private val tracksQueries = database.playlistTracksQueries

    override fun getPlaylists(): Flow<List<Playlist>> =
        playlistsQueries.selectAllPlaylists()
            .asFlow()
            .mapToList(MeloDispatchers.IO)
            .map { rows ->
                rows.map { row ->
                    val artworks = row.concatenated_artworks
                        ?.split(",")
                        ?.filter { it.isNotBlank() }
                        ?.distinct()
                        ?.take(4)
                        ?: emptyList()
                    Playlist(
                        id = row.id,
                        name = row.name,
                        trackCount = row.track_count.toInt(),
                        createdAt = row.created_at,
                        artworks = artworks,
                    )
                }
            }

    override fun getPlaylistTracks(playlistId: Long): Flow<List<Track>> =
        tracksQueries.selectTracksForPlaylist(playlistId)
            .asFlow()
            .mapToList(MeloDispatchers.IO)
            .map { rows -> rows.map { it.toTrack() } }

    override suspend fun createPlaylist(name: String): Long = withContext(MeloDispatchers.IO) {
        playlistsQueries.transactionWithResult {
            playlistsQueries.insertPlaylist(name = name, created_at = currentTimeSeconds() * 1000L)
            playlistsQueries.lastInsertId().executeAsOne()
        }
    }

    override suspend fun renamePlaylist(id: Long, name: String) {
        withContext(MeloDispatchers.IO) {
            playlistsQueries.renamePlaylist(name = name, id = id)
        }
    }

    override suspend fun deletePlaylist(id: Long) {
        withContext(MeloDispatchers.IO) {
            tracksQueries.deleteAllTracksForPlaylist(id)
            playlistsQueries.deletePlaylist(id)
        }
    }

    override suspend fun addTrackToPlaylist(playlistId: Long, track: Track) {
        withContext(MeloDispatchers.IO) {
            tracksQueries.insertTrackToPlaylist(
                playlist_id = playlistId,
                track_id = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                duration_ms = track.durationMs,
                artwork_url = track.artworkUrl,
                source_id = track.sourceId,
                playlist_id_ = playlistId,
                added_at = currentTimeSeconds() * 1000L,
            )
        }
    }

    override suspend fun addTracksToPlaylist(
        playlistId: Long,
        tracks: List<Track>,
        skipDuplicates: Boolean
    ): Int = withContext(MeloDispatchers.IO) {
        if (tracks.isEmpty()) return@withContext 0
        var addedCount = 0
        tracksQueries.transactionWithResult {
            val existingIds = if (skipDuplicates) {
                tracksQueries.selectTracksForPlaylist(playlistId).executeAsList()
                    .map { it.track_id }.toSet()
            } else {
                emptySet()
            }
            val filteredTracks = if (skipDuplicates) {
                tracks.filter { it.id !in existingIds }
            } else {
                tracks
            }
            val baseTime = currentTimeSeconds() * 1000L
            filteredTracks.forEachIndexed { index, track ->
                tracksQueries.insertTrackToPlaylist(
                    playlist_id = playlistId,
                    track_id = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    duration_ms = track.durationMs,
                    artwork_url = track.artworkUrl,
                    source_id = track.sourceId,
                    playlist_id_ = playlistId,
                    added_at = baseTime + index,
                )
                addedCount++
            }
            addedCount
        }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        withContext(MeloDispatchers.IO) {
            tracksQueries.removeTrackFromPlaylist(playlist_id = playlistId, track_id = trackId)
        }
    }

    override suspend fun reorderPlaylistTracks(playlistId: Long, trackIds: List<String>) {
        withContext(MeloDispatchers.IO) {
            tracksQueries.transaction {
                trackIds.forEachIndexed { index, trackId ->
                    tracksQueries.updateTrackPosition(
                        position = index.toLong(),
                        playlist_id = playlistId,
                        track_id = trackId
                    )
                }
            }
        }
    }

    override fun getPlaylistIdsForTrack(trackId: String): Flow<Set<Long>> =
        tracksQueries.selectPlaylistIdsForTrack(trackId)
            .asFlow()
            .mapToList(MeloDispatchers.IO)
            .map { it.toSet() }

    private fun Playlist_tracks.toTrack() = Track(
        id = track_id,
        title = title,
        artist = artist,
        album = album,
        durationMs = duration_ms,
        genres = emptyList(),
        artworkUrl = artwork_url,
        sourceId = source_id,
    )
}

