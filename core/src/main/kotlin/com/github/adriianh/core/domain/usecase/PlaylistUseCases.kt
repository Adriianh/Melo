package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow

class GetPlaylistsUseCase(private val repository: PlaylistRepository) {
    operator fun invoke(): Flow<List<Playlist>> = repository.getPlaylists()
}

class GetPlaylistTracksUseCase(private val repository: PlaylistRepository) {
    operator fun invoke(playlistId: Long): Flow<List<Track>> =
        repository.getPlaylistTracks(playlistId)
}

class CreatePlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(name: String): Long = repository.createPlaylist(name.trim())
}

class RenamePlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(id: Long, name: String) =
        repository.renamePlaylist(id, name.trim())
}

class DeletePlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(id: Long) = repository.deletePlaylist(id)
}

class AddTrackToPlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: Long, track: Track) =
        repository.addTrackToPlaylist(playlistId, track)
}

class RemoveTrackFromPlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: Long, trackId: String) =
        repository.removeTrackFromPlaylist(playlistId, trackId)
}

