package com.github.adriianh.core.domain.usecase.offline

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.MusicRepository
import com.github.adriianh.core.domain.repository.OfflineRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Enriches local tracks with online metadata, cover artwork, and genres when available,
 * and saves the enriched tags directly into the audio files.
 */
class EnrichLocalTracksUseCase(
    private val musicRepository: MusicRepository,
    private val offlineRepository: OfflineRepository
) {

    suspend operator fun invoke(tracks: List<Track>): List<Track> = coroutineScope {
        val semaphore = Semaphore(4)
        val deferreds = tracks.map { track ->
            async {
                if (!track.artworkUrl.isNullOrBlank() &&
                    track.artist != "Artista Desconocido" &&
                    track.artist != "Unknown Artist" &&
                    track.album.isNotBlank()
                ) {
                    return@async track
                }

                semaphore.withPermit {
                    try {
                        val query =
                            if (track.artist != "Artista Desconocido" && track.artist != "Unknown Artist" && track.artist.isNotBlank()) {
                                "${track.title} ${track.artist}"
                            } else {
                                cleanQuery(track.title)
                            }

                        val results = musicRepository.search(query)
                        val bestMatch = results.firstOrNull()

                        if (bestMatch != null) {
                            val enriched = track.copy(
                                artworkUrl = track.artworkUrl ?: bestMatch.artworkUrl,
                                album = track.album.ifBlank { bestMatch.album },
                                artist = if (track.artist == "Artista Desconocido" || track.artist == "Unknown Artist") bestMatch.artist else track.artist,
                                genres = track.genres.ifEmpty { bestMatch.genres },
                                sourceId = track.sourceId ?: bestMatch.id
                            )

                            try {
                                offlineRepository.updateTrackMetadata(
                                    trackId = enriched.id,
                                    title = enriched.title,
                                    artist = enriched.artist,
                                    album = enriched.album
                                )
                            } catch (_: Exception) {
                            }

                            enriched
                        } else {
                            track
                        }
                    } catch (_: Exception) {
                        track
                    }
                }
            }
        }
        deferreds.awaitAll()
    }

    private fun cleanQuery(title: String): String {
        return title
            .replace(Regex("^\\d{1,3}[\\s.\\-_]+"), "")
            .replace(Regex("\\[.*?]|\\(.*?\\)"), "")
            .trim()
            .takeIf { it.isNotBlank() } ?: title
    }
}