package com.github.adriianh.core.usecase

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.usecase.offline.GetOfflineSuggestionsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetOfflineSuggestionsUseCaseTest {
    private val repository = mockk<OfflineRepository>()
    private val useCase = GetOfflineSuggestionsUseCase(repository)

    private val currentTrack =
        Track(
            id = "current",
            title = "Track A",
            artist = "Artist X",
            album = "Album 1",
            genres = listOf("Rock"),
            durationMs = 180000,
            artworkUrl = null,
            sourceId = null,
        )

    private fun createOfflineTrack(
        id: String,
        artist: String = "Other Artist",
        album: String = "Other Album",
        genres: List<String> = emptyList(),
        status: DownloadStatus = DownloadStatus.COMPLETED,
    ) = OfflineTrack(
        track =
            Track(
                id = id,
                title = "Title $id",
                artist = artist,
                album = album,
                genres = genres,
                durationMs = 180000,
                artworkUrl = null,
                sourceId = null,
            ),
        downloadStatus = status,
    )

    @Test
    fun `returns empty list when no tracks are offline`() =
        runTest {
            coEvery { repository.getOfflineTracks() } returns emptyList()

            val result = useCase(currentTrack)

            assertTrue(result.isEmpty())
        }

    @Test
    fun `filters out non completed tracks, current track, and excluded ids`() =
        runTest {
            val tracks =
                listOf(
                    createOfflineTrack("pending", status = DownloadStatus.PENDING),
                    createOfflineTrack("current"),
                    createOfflineTrack("excluded"),
                    createOfflineTrack("valid"),
                )
            coEvery { repository.getOfflineTracks() } returns tracks

            val result =
                useCase(
                    currentTrack = currentTrack,
                    excludeTrackIds = setOf("excluded"),
                )

            assertEquals(1, result.size)
            assertEquals("valid", result.first().id)
        }

    @Test
    fun `prioritizes same album then same artist then same genre`() =
        runTest {
            val sameAlbum = createOfflineTrack("album_match", album = "Album 1", artist = "Other")
            val sameArtist = createOfflineTrack("artist_match", artist = "Artist X", album = "Diff")
            val sameGenre = createOfflineTrack("genre_match", genres = listOf("Rock"))
            val other = createOfflineTrack("other")

            coEvery { repository.getOfflineTracks() } returns
                listOf(other, sameGenre, sameArtist, sameAlbum)

            val result = useCase(currentTrack)

            assertEquals(4, result.size)
            assertEquals("album_match", result[0].id)
            assertEquals("artist_match", result[1].id)
            assertEquals("genre_match", result[2].id)
            assertEquals("other", result[3].id)
        }

    @Test
    fun `respects limit`() =
        runTest {
            val tracks = (1..15).map { createOfflineTrack("id_$it") }
            coEvery { repository.getOfflineTracks() } returns tracks

            val result = useCase(currentTrack, limit = 5)

            assertEquals(5, result.size)
        }
}
