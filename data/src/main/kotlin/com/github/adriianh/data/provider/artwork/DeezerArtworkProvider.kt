package com.github.adriianh.data.provider.artwork

import com.github.adriianh.core.domain.model.ResolvedMetadata
import com.github.adriianh.core.domain.provider.MetadataProvider
import com.github.adriianh.data.remote.deezer.DeezerSearchResponse
import com.github.adriianh.data.remote.deezer.DeezerTrackDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class DeezerArtworkProvider(private val httpClient: HttpClient) : MetadataProvider {
    override suspend fun resolveMetadata(title: String, artist: String): ResolvedMetadata? {
        return try {
            val response = httpClient.get("https://api.deezer.com/search/track") {
                parameter("q", "$title $artist")
                parameter("limit", 1)
            }.body<DeezerSearchResponse<DeezerTrackDto>>()
            val first = response.data.firstOrNull() ?: return null
            ResolvedMetadata(
                album = first.album.title.takeIf { it.isNotBlank() },
                artworkUrl = first.album.coverMedium.takeIf { it.isNotBlank() }
            )
        } catch (_: Exception) {
            null
        }
    }
}