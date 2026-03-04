package com.github.adriianh.cli.di

import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.provider.ArtworkProvider
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.repository.*
import com.github.adriianh.core.domain.usecase.*
import com.github.adriianh.data.local.DatabaseFactory
import com.github.adriianh.data.local.MeloDatabase
import com.github.adriianh.data.provider.*
import com.github.adriianh.data.remote.itunes.ItunesApiClient
import com.github.adriianh.data.remote.lastfm.LastFmApiClient
import com.github.adriianh.data.remote.lyrics.LyricsApiClient
import com.github.adriianh.data.remote.piped.PipedApiClient
import com.github.adriianh.data.remote.spotify.SpotifyApiClient
import com.github.adriianh.data.remote.spotify.SpotifyAuthClient
import com.github.adriianh.data.repository.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module


private fun hasSpotifyKeys() =
    resolveEnv("SPOTIFY_CLIENT_ID") != null &&
            resolveEnv("SPOTIFY_CLIENT_SECRET") != null

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
    single {
        SpotifyAuthClient(
            httpClient    = get(),
            clientId      = resolveEnv("SPOTIFY_CLIENT_ID")     ?: "",
            clientSecret  = resolveEnv("SPOTIFY_CLIENT_SECRET") ?: "",
        )
    }
    single { SpotifyApiClient(get(), get()) }
    single {
        LastFmApiClient(
            httpClient = get(),
            apiKey     = resolveEnv("LASTFM_API_KEY") ?: "",
        )
    }
    single { LyricsApiClient(get()) }
    single { PipedApiClient(get()) }

    // Providers
    single<MusicProvider> {
        val itunes = ItunesMusicProvider(get())
        val providers = mutableListOf<MusicProvider>(itunes, PipedMusicProvider(get()))
        if (hasSpotifyKeys()) providers.add(SpotifyMusicProvider(get()))
        MergedMusicProvider(providers)
    }
    single<ArtworkProvider> { ItunesMusicProvider(get()) }
    single<DiscoveryProvider> { LastFmDiscoveryProvider(get()) }
    single<AudioProvider> { YtDlpAudioProvider(get()) }

    // Repositories
    single<MusicRepository> { MusicRepositoryImpl(get(), get(), get(), get()) }
    single<LyricsRepository> { LyricsRepositoryImpl(get()) }
    single<DiscoveryRepository> { DiscoveryRepositoryImpl(get()) }
    single<MeloDatabase> { DatabaseFactory.create() }
    single<FavoritesRepository> { FavoritesRepositoryImpl(get()) }
    single<HistoryRepository> { HistoryRepositoryImpl(get()) }

    // Use Cases
    single { SearchTracksUseCase(get()) }
    single { LoadMoreTracksUseCase(get()) }
    single { GetTrackUseCase(get()) }
    single { GetLyricsUseCase(get()) }
    single { GetSimilarTracksUseCase(get()) }
    single { GetFavoritesUseCase(get()) }
    single { AddFavoriteUseCase(get()) }
    single { RemoveFavoriteUseCase(get()) }
    single { IsFavoriteUseCase(get()) }
    single { GetRecentTracksUseCase(get()) }
    single { RecordPlayUseCase(get()) }
    single { GetStreamUseCase(get()) }
}