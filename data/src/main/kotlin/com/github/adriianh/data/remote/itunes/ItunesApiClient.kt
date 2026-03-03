package com.github.adriianh.data.remote.itunes

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class ItunesApiClient(
    private val httpClient: HttpClient
) {

    suspend fun search(query: String, limit: Int = 20): List<ItunesTrackDto> {
        return try {
            httpClient.get("https://itunes.apple.com/search") {
                parameter("term", query)
                parameter("media", "music")
                parameter("entity", "song")
                parameter("limit", limit)
            }.body<ItunesSearchResponse>().results
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("iTunes search error: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTrack(id: String): ItunesTrackDto? {
        return try {
            httpClient.get("https://itunes.apple.com/lookup") {
                parameter("id", id)
                parameter("entity", "song")
            }.body<ItunesSearchResponse>().results.firstOrNull()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("iTunes getTrack error: ${e.message}")
            null
        }
    }
}
