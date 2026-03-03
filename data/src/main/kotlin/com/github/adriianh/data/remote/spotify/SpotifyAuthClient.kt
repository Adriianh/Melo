package com.github.adriianh.data.remote.spotify

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
)

class SpotifyAuthClient(
    private val httpClient: HttpClient,
    private val clientId: String,
    private val clientSecret: String,
) {

    private var accessToken: String? = null
    private var tokenExpiresAt: Long = 0

    suspend fun getAccessToken(): String {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return accessToken!!
        }
        return fetchNewToken()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun fetchNewToken(): String {
        val credentials = Base64.encode("$clientId:$clientSecret".toByteArray())

        val response = httpClient.post("https://accounts.spotify.com/api/token") {
            header(HttpHeaders.Authorization, "Basic $credentials")
            setBody(FormDataContent(Parameters.build {
                append("grant_type", "client_credentials")
            }))
        }

        val tokenResponse = response.body<TokenResponse>()
        accessToken = tokenResponse.accessToken
        tokenExpiresAt = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000L) - 60_000L

        return tokenResponse.accessToken
    }
}