package com.github.adriianh.data.provider.artwork

import com.github.adriianh.core.domain.provider.ArtworkProvider
import com.github.adriianh.data.remote.deezer.DeezerSearchResponse
import com.github.adriianh.data.remote.deezer.DeezerTrackDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class DeezerArtworkProvider(private val httpClient: HttpClient) : ArtworkProvider {
    override suspend fun resolveArtwork(title: String, artist: String): String? {
        return try {
            val response = httpClient.get("https://api.deezer.com/search/track") {
                parameter("q", "$title $artist")
                parameter("limit", 1)
            }.body<DeezerSearchResponse<DeezerTrackDto>>()
            response.data.firstOrNull()?.album?.coverMedium?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}