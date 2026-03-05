package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getPlaylists(): Flow<List<Playlist>>
    fun getPlaylistTracks(playlistId: Long): Flow<List<Track>>
    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(id: Long, name: String)
    suspend fun deletePlaylist(id: Long)
    suspend fun addTrackToPlaylist(playlistId: Long, track: Track)
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)
}

