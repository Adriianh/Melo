package com.github.adriianh.data.maper

import com.github.adriianh.data.remote.spotify.SpotifyAlbumDto
import com.github.adriianh.data.remote.spotify.SpotifyArtistDto
import com.github.adriianh.data.remote.spotify.SpotifyImageDto
import com.github.adriianh.data.remote.spotify.SpotifyTrackDto
import com.github.adriianh.data.remote.spotify.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals

class SpotifyMapperTest {

    private val spotifyTrack = SpotifyTrackDto(
        id = "123",
        name = "Test Track",
        durationMs = 210000,
        artists = listOf(
            SpotifyArtistDto(
                id = "456",
                name = "Test Artist"
            )
        ),
        album = SpotifyAlbumDto(name = "Test Album")
    )

    @Test
    fun `maps id, title, album and duration correctly`() {
        val track = spotifyTrack.toDomain()

        assertEquals("123", track.id)
        assertEquals("Test Track", track.title)
        assertEquals("Test Album", track.album)
        assertEquals(210000L, track.durationMs)
    }

    @Test
    fun `takes first artist when multiple are present`() {
        val spotifyTrackWithMultipleArtists = spotifyTrack.copy(
            artists = listOf(
                SpotifyArtistDto(id = "456", name = "Test Artist"),
                SpotifyArtistDto(id = "789", name = "Another Artist")
            )
        )
        val track = spotifyTrackWithMultipleArtists.toDomain()

        assertEquals("Test Artist", track.artist)
    }

    @Test
    fun `takes first artworkUrl when multiple are present`() {
        val spotifyTrackWithMultipleImages = spotifyTrack.copy(
            album = spotifyTrack.album?.copy(
                images = listOf(
                    SpotifyImageDto(url = "http://example.com/image1.jpg", width = 640, height = 640),
                    SpotifyImageDto(url = "http://example.com/image2.jpg", width = 300, height = 300)
                )
            )
        )
        val track = spotifyTrackWithMultipleImages.toDomain()

        assertEquals("http://example.com/image1.jpg", track.artworkUrl)
    }

    @Test
    fun `returns Unknown when artist list is empty`() {
        val spotifyTrackWithoutArtist = spotifyTrack.copy(artists = emptyList())
        val track = spotifyTrackWithoutArtist.toDomain()

        assertEquals("Unknown", track.artist)
    }

    @Test
    fun `returns null when artwork list is empty`() {
        val spotifyTrackWithoutArtwork = spotifyTrack.copy(album = spotifyTrack.album?.copy(images = emptyList()))
        val track = spotifyTrackWithoutArtwork.toDomain()

        assertEquals(null, track.artworkUrl)
    }
}