package com.github.adriianh.cli.di

import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.repository.DiscoveryRepository
import com.github.adriianh.core.domain.repository.LyricsRepository
import com.github.adriianh.core.domain.repository.MusicRepository
import com.github.adriianh.core.domain.usecase.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.GetTrackUseCase
import com.github.adriianh.core.domain.usecase.LoadMoreTracksUseCase
import com.github.adriianh.core.domain.usecase.SearchTracksUseCase
import com.github.adriianh.data.provider.FallbackMusicProvider
import com.github.adriianh.data.provider.ItunesMusicProvider
import com.github.adriianh.data.provider.LastFmDiscoveryProvider
import com.github.adriianh.data.provider.PipedAudioProvider
import com.github.adriianh.data.provider.SpotifyMusicProvider
import com.github.adriianh.data.remote.itunes.ItunesApiClient
import com.github.adriianh.data.remote.lastfm.LastFmApiClient
import com.github.adriianh.data.remote.lyrics.LyricsApiClient
import com.github.adriianh.data.remote.piped.PipedApiClient
import com.github.adriianh.data.remote.spotify.SpotifyApiClient
import com.github.adriianh.data.remote.spotify.SpotifyAuthClient
import com.github.adriianh.data.repository.DiscoveryRepositoryImpl
import com.github.adriianh.data.repository.LyricsRepositoryImpl
import com.github.adriianh.data.repository.MusicRepositoryImpl
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

private val env = dotenv {
    directory = System.getProperty("user.dir")
    ignoreIfMissing = true
}

private fun hasSpotifyKeys() =
    env.get("SPOTIFY_CLIENT_ID", null) != null &&
            env.get("SPOTIFY_CLIENT_SECRET", null) != null

val appModule = module {
    // Infrastructure
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                val jsonConfig = Json { 
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                json(jsonConfig)
                json(jsonConfig, contentType = ContentType.Text.JavaScript)
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            install(HttpRequestRetry) {
                retryOnExceptionOrServerErrors(maxRetries = 3)
                exponentialDelay()
            }
        }
    }

    // API Clients
    single { ItunesApiClient(get()) }
    single { SpotifyAuthClient(get()) }
    single { SpotifyApiClient(get(), get()) }
    single { LastFmApiClient(get()) }
    single { LyricsApiClient(get()) }
    single { PipedApiClient(get()) }

    // Providers
    single<MusicProvider> {
        val itunes = ItunesMusicProvider(get())
        val spotify = if (hasSpotifyKeys()) SpotifyMusicProvider(get()) else null

        if (spotify != null) {
            println("Spotify keys found, using as fallback provider")
            FallbackMusicProvider(primary = itunes, fallback = spotify)
        } else {
            itunes
        }
    }
    single<DiscoveryProvider> { LastFmDiscoveryProvider(get()) }
    single<AudioProvider> { PipedAudioProvider(get()) }

    // Repositories
    single<MusicRepository> { MusicRepositoryImpl(get(), get(), get()) }
    single<LyricsRepository> { LyricsRepositoryImpl(get()) }
    single<DiscoveryRepository> { DiscoveryRepositoryImpl(get()) }

    // Use Cases
    single { SearchTracksUseCase(get()) }
    single { LoadMoreTracksUseCase(get()) }
    single { GetTrackUseCase(get()) }
    single { GetLyricsUseCase(get()) }
    single { GetSimilarTracksUseCase(get()) }
}