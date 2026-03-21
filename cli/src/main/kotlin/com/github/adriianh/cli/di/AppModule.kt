package com.github.adriianh.cli.di

import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.config.shareDir
import com.github.adriianh.cli.tui.player.MediaSessionManager
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.core.domain.interactor.*
import com.github.adriianh.core.domain.provider.ArtworkProvider
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.repository.*
import com.github.adriianh.core.domain.usecase.*
import com.github.adriianh.data.local.DatabaseFactory
import com.github.adriianh.data.local.MeloDatabase
import com.github.adriianh.data.provider.artwork.CompositeArtworkProvider
import com.github.adriianh.data.provider.artwork.DeezerArtworkProvider
import com.github.adriianh.data.provider.artwork.ItunesArtworkProvider
import com.github.adriianh.data.provider.audio.YtDlpAudioProvider
import com.github.adriianh.data.provider.discovery.CompositeDiscoveryProvider
import com.github.adriianh.data.provider.discovery.DeezerDiscoveryProvider
import com.github.adriianh.data.provider.discovery.LastFmDiscoveryProvider
import com.github.adriianh.data.provider.music.ItunesMusicProvider
import com.github.adriianh.data.provider.music.MergedMusicProvider
import com.github.adriianh.data.provider.music.PipedMusicProvider
import com.github.adriianh.data.provider.music.SpotifyMusicProvider
import com.github.adriianh.data.remote.deezer.DeezerApiClient
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
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import java.io.File

private fun hasSpotifyKeys() =
    resolveEnv("SPOTIFY_CLIENT_ID") != null &&
            resolveEnv("SPOTIFY_CLIENT_SECRET") != null

val appModule = module {
    // Infrastructure
    single<CoroutineDispatcher> { Dispatchers.IO.limitedParallelism(8) }

    single {
        HttpClient(CIO) {
            engine {
                // Use our limited dispatcher for the CIO engine
                dispatcher = get<CoroutineDispatcher>()
                endpoint {
                    maxConnectionsCount = 20
                    connectTimeout = 5_000
                    connectTimeout = 5_000
                }
            }
            install(ContentNegotiation) {
                val jsonConfig = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                json(jsonConfig)
                json(jsonConfig, contentType = ContentType.Text.JavaScript)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 10_000
            }
            install(HttpRequestRetry) {
                retryOnExceptionOrServerErrors(maxRetries = 2)
                exponentialDelay()
            }
        }
    }

    // API Clients
    single { ArtworkRenderer(get()) }
    single { ItunesApiClient(get()) }
    single {
        SpotifyAuthClient(
            httpClient = get(),
            clientId = resolveEnv("SPOTIFY_CLIENT_ID") ?: "",
            clientSecret = resolveEnv("SPOTIFY_CLIENT_SECRET") ?: "",
        )
    }
    single { SpotifyApiClient(get(), get()) }
    single {
        LastFmApiClient(
            httpClient = get(),
            apiKey = resolveEnv("LASTFM_API_KEY") ?: "",
            sharedSecret = resolveEnv("LASTFM_SHARED_SECRET") ?: "",
        )
    }
    single { LyricsApiClient(get()) }
    single { PipedApiClient(get()) }
    single { DeezerApiClient(get()) }

    // Providers
    single<MusicProvider> {
        val itunes = ItunesMusicProvider(get())
        val providers = mutableListOf(itunes, PipedMusicProvider(get()))
        if (hasSpotifyKeys()) providers.add(SpotifyMusicProvider(get()))
        MergedMusicProvider(providers)
    }
    single<ArtworkProvider> {
        val itunes = ItunesArtworkProvider(get())
        val deezer = DeezerArtworkProvider(get())
        CompositeArtworkProvider(itunes, deezer)
    }
    single<DiscoveryProvider> {
        CompositeDiscoveryProvider(
            listOf(
                LastFmDiscoveryProvider(get()),
                DeezerDiscoveryProvider(get()),
            )
        )
    }
    single<AudioProvider> { YtDlpAudioProvider(get()) }
    single { MediaSessionManager(httpClient = get()) }

    // Repositories
    single<MeloDatabase> { DatabaseFactory.create() }
    single<MusicRepository> { MusicRepositoryImpl(get(), get(), get(), get()) }
    single<LyricsRepository> { LyricsRepositoryImpl(get()) }
    single<DiscoveryRepository> { DiscoveryRepositoryImpl(get()) }
    single<FavoritesRepository> { FavoritesRepositoryImpl(get()) }
    single<HistoryRepository> { HistoryRepositoryImpl(get()) }
    single<PlaylistRepository> { PlaylistRepositoryImpl(get()) }
    single<SessionRepository> { SessionRepositoryImpl(get()) }
    single<ScrobblingRepository> { ScrobblingRepositoryImpl(get(), configDir) }
    single<StatsRepository> { StatsRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(File(configDir), get()) }
    single<OfflineRepository> { OfflineRepositoryImpl(File(shareDir), get(), get()) }

    factory { SearchTracksUseCase(get()) }
    factory { LoadMoreTracksUseCase(get()) }
    factory { GetTrackUseCase(get()) }
    factory { GetLyricsUseCase(get()) }
    factory { GetSyncedLyricsUseCase(get()) }
    factory { GetSimilarTracksUseCase(get()) }
    factory { GetFavoritesUseCase(get()) }
    factory { AddFavoriteUseCase(get()) }
    factory { RemoveFavoriteUseCase(get()) }
    factory { IsFavoriteUseCase(get()) }
    factory { GetRecentTracksUseCase(get()) }
    factory { RecordPlayUseCase(get()) }
    factory { GetStreamUseCase(get(), get()) }
    factory { GetOfflineTracksUseCase(get()) }
    factory { SyncOfflineTracksUseCase(get()) }
    factory { DownloadTrackUseCase(get()) }
    factory { DeleteDownloadedTrackUseCase(get()) }
    factory { MarkTrackAccessedUseCase(get()) }
    factory { AutoCleanupUseCase(get()) }
    factory { GetPlaylistsUseCase(get()) }
    factory { GetPlaylistTracksUseCase(get()) }
    factory { CreatePlaylistUseCase(get()) }
    factory { RenamePlaylistUseCase(get()) }
    factory { DeletePlaylistUseCase(get()) }
    factory { AddTrackToPlaylistUseCase(get()) }
    factory { RemoveTrackFromPlaylistUseCase(get()) }
    factory { SaveSessionUseCase(get()) }
    factory { RestoreSessionUseCase(get()) }
    factory { ClearSessionUseCase(get()) }
    factory { UpdateNowPlayingUseCase(get()) }
    factory { ScrobbleUseCase(get()) }
    factory { AuthenticateLastFmUseCase(get()) }
    factory { StartWebAuthUseCase(get()) }
    factory { CompleteWebAuthUseCase(get()) }
    factory { GetTopTracksUseCase(get()) }
    factory { GetTopArtistsUseCase(get()) }
    factory { GetListeningStatsUseCase(get()) }
    factory { GetSettingsUseCase(get()) }
    factory { UpdateSettingsUseCase(get()) }

    // Interactors
    factory { SearchInteractors(get(), get(), get(), get(), get(), get()) }
    factory { LibraryInteractors(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { PlaybackInteractors(get(), get(), get(), get(), get()) }
    factory { OfflineInteractors(get(), get(), get(), get(), get(), get()) }
    factory { StatsInteractors(get(), get(), get()) }
    factory { SessionInteractors(get(), get(), get()) }
    factory { SettingsInteractors(get(), get()) }
}