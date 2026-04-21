package com.github.adriianh.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.PlaylistRepository
import com.github.adriianh.data.local.MeloDatabase
import com.github.adriianh.data.local.Playlist_tracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PlaylistRepositoryImpl(database: MeloDatabase) : PlaylistRepository {

    private val playlistsQueries = database.playlistsQueries
    private val tracksQueries = database.playlistTracksQueries

    override fun getPlaylists(): Flow<List<Playlist>> =
        playlistsQueries.selectAllPlaylists()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map { row ->
                    Playlist(
                        id = row.id,
                        name = row.name,
                        trackCount = row.track_count.toInt(),
                        createdAt = row.created_at,
                    )
                }
            }

    override fun getPlaylistTracks(playlistId: Long): Flow<List<Track>> =
        tracksQueries.selectTracksForPlaylist(playlistId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toTrack() } }

    override suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistsQueries.transactionWithResult {
            playlistsQueries.insertPlaylist(name = name, created_at = System.currentTimeMillis())
            playlistsQueries.lastInsertId().executeAsOne()
        }
    }

    override suspend fun renamePlaylist(id: Long, name: String) = withContext(Dispatchers.IO) {
        playlistsQueries.renamePlaylist(name = name, id = id)
    }

    override suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        tracksQueries.deleteAllTracksForPlaylist(id)
        playlistsQueries.deletePlaylist(id)
    }

    override suspend fun addTrackToPlaylist(playlistId: Long, track: Track) =
        withContext(Dispatchers.IO) {
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
                added_at = System.currentTimeMillis(),
            )
        }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) =
        withContext(Dispatchers.IO) {
            tracksQueries.removeTrackFromPlaylist(playlist_id = playlistId, track_id = trackId)
        }

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

