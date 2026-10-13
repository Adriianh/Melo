package com.github.adriianh.core.domain.usecase.offline

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.OfflineRepository

class GetOfflineSuggestionsUseCase(
    private val offlineRepository: OfflineRepository,
) {
    suspend operator fun invoke(
        currentTrack: Track,
        excludeTrackIds: Set<String> = emptySet(),
        limit: Int = 10,
    ): List<Track> {
        val completed =
            offlineRepository
                .getOfflineTracks()
                .filter { it.downloadStatus == DownloadStatus.COMPLETED }
                .map { it.track }
                .filter { it.id !in excludeTrackIds && it.id != currentTrack.id }

        if (completed.isEmpty()) return emptyList()

        val sameAlbum =
            completed.filter {
                currentTrack.album.isNotBlank() &&
                    it.album.equals(currentTrack.album, ignoreCase = true)
            }
        val sameArtist =
            completed.filter {
                it.artist.equals(currentTrack.artist, ignoreCase = true) &&
                    it !in sameAlbum
            }
        val sameGenre =
            completed.filter { candidate ->
                currentTrack.genres.isNotEmpty() &&
                    candidate.genres.any { genre ->
                        currentTrack.genres.any { it.equals(genre, ignoreCase = true) }
                    } &&
                    candidate !in sameArtist &&
                    candidate !in sameAlbum
            }
        val others =
            completed
                .filter {
                    it !in sameArtist && it !in sameAlbum && it !in sameGenre
                }.shuffled()

        val ranked = sameAlbum.shuffled() + sameArtist.shuffled() + sameGenre.shuffled() + others
        return ranked.take(limit)
    }
}
