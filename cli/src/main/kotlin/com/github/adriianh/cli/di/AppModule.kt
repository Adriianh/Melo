package com.github.adriianh.cli.di

import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.config.shareDir
import com.github.adriianh.cli.tui.player.MediaSessionManager
import com.github.adriianh.cli.tui.service.DiscordRpcManager
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.core.domain.interactor.LibraryInteractors
import com.github.adriianh.core.domain.interactor.OfflineInteractors
import com.github.adriianh.core.domain.interactor.PlaybackInteractors
import com.github.adriianh.core.domain.interactor.SearchInteractors
import com.github.adriianh.core.domain.interactor.SessionInteractors
import com.github.adriianh.core.domain.interactor.SettingsInteractors
import com.github.adriianh.core.domain.interactor.StatsInteractors
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.core.domain.provider.MetadataProvider
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.repository.DiscoveryRepository
import com.github.adriianh.core.domain.repository.FavoritesRepository
import com.github.adriianh.core.domain.repository.HistoryRepository
import com.github.adriianh.core.domain.repository.LyricsRepository
import com.github.adriianh.core.domain.repository.MusicRepository
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.repository.PlaylistRepository
import com.github.adriianh.core.domain.repository.ScrobblingRepository
import com.github.adriianh.core.domain.repository.SessionRepository
import com.github.adriianh.core.domain.repository.SettingsRepository
import com.github.adriianh.core.domain.repository.StatsRepository
import com.github.adriianh.core.domain.usecase.library.AddFavoriteUseCase
import com.github.adriianh.core.domain.usecase.library.AddTrackToPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.CreatePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.DeletePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.GetFavoritesUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistTracksUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.IsFavoriteUseCase
import com.github.adriianh.core.domain.usecase.library.RemoveFavoriteUseCase
import com.github.adriianh.core.domain.usecase.library.RemoveTrackFromPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.RenamePlaylistUseCase
import com.github.adriianh.core.domain.usecase.offline.AutoCleanupUseCase
import com.github.adriianh.core.domain.usecase.offline.DeleteDownloadedTrackUseCase
import com.github.adriianh.core.domain.usecase.offline.DownloadTrackUseCase
import com.github.adriianh.core.domain.usecase.offline.GetOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.MarkTrackAccessedUseCase
import com.github.adriianh.core.domain.usecase.offline.SyncOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.playback.AuthenticateLastFmUseCase
import com.github.adriianh.core.domain.usecase.playback.CompleteWebAuthUseCase
import com.github.adriianh.core.domain.usecase.playback.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.playback.RecordPlayUseCase
import com.github.adriianh.core.domain.usecase.playback.ScrobbleUseCase
import com.github.adriianh.core.domain.usecase.playback.StartWebAuthUseCase
import com.github.adriianh.core.domain.usecase.playback.UpdateNowPlayingUseCase
import com.github.adriianh.core.domain.usecase.search.GetArtistTagsUseCase
import com.github.adriianh.core.domain.usecase.search.GetEntityDetailsUseCase
import com.github.adriianh.core.domain.usecase.search.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.search.GetSyncedLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrackUseCase
import com.github.adriianh.core.domain.usecase.search.LoadMoreAlbumsUseCase
import com.github.adriianh.core.domain.usecase.search.LoadMoreArtistsUseCase
import com.github.adriianh.core.domain.usecase.search.LoadMorePlaylistsUseCase
import com.github.adriianh.core.domain.usecase.search.LoadMoreTracksUseCase
import com.github.adriianh.core.domain.usecase.search.SearchAlbumsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchArtistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.adriianh.core.domain.usecase.session.ClearSessionUseCase
import com.github.adriianh.core.domain.usecase.session.RestoreSessionUseCase
import com.github.adriianh.core.domain.usecase.session.SaveSessionUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.adriianh.core.domain.usecase.stats.GetListeningStatsUseCase
import com.github.adriianh.core.domain.usecase.stats.GetTopArtistsUseCase
import com.github.adriianh.core.domain.usecase.stats.GetTopTracksUseCase
import com.github.adriianh.data.local.DatabaseFactory
import com.github.adriianh.data.local.MeloDatabase
import com.github.adriianh.data.provider.artwork.CompositeArtworkProvider
import com.github.adriianh.data.provider.artwork.DeezerArtworkProvider
import com.github.adriianh.data.provider.artwork.ItunesArtworkProvider
import com.github.adriianh.data.provider.audio.InnerTubeAudioProvider
import com.github.adriianh.data.provider.audio.YtDlpAudioProvider
import com.github.adriianh.data.provider.discovery.CompositeDiscoveryProvider
import com.github.adriianh.data.provider.discovery.DeezerDiscoveryProvider
import com.github.adriianh.data.provider.discovery.InnerTubeDiscoveryProvider
import com.github.adriianh.data.provider.discovery.LastFmDiscoveryProvider
import com.github.adriianh.data.provider.music.InnerTubeMusicProvider
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
import com.github.adriianh.data.repository.DiscoveryRepositoryImpl
import com.github.adriianh.data.repository.FavoritesRepositoryImpl
import com.github.adriianh.data.repository.HistoryRepositoryImpl
import com.github.adriianh.data.repository.LyricsRepositoryImpl
import com.github.adriianh.data.repository.MusicRepositoryImpl
import com.github.adriianh.data.repository.OfflineRepositoryImpl
import com.github.adriianh.data.repository.PlaylistRepositoryImpl
import com.github.adriianh.data.repository.ScrobblingRepositoryImpl
import com.github.adriianh.data.repository.SessionRepositoryImpl
import com.github.adriianh.data.repository.SettingsRepositoryImpl
import com.github.adriianh.data.repository.StatsRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
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
        val providers = mutableListOf(
            itunes,
            InnerTubeMusicProvider(
                fallback = PipedMusicProvider(get())
            )
        )
        if (hasSpotifyKeys()) providers.add(SpotifyMusicProvider(get()))
        MergedMusicProvider(providers)
    }
    single<MetadataProvider> {
        val itunes = ItunesArtworkProvider(get())
        val deezer = DeezerArtworkProvider(get())
        CompositeArtworkProvider(itunes, deezer)
    }
    single<DiscoveryProvider> {
        CompositeDiscoveryProvider(
            listOf(
                InnerTubeDiscoveryProvider(),
                LastFmDiscoveryProvider(get()),
                DeezerDiscoveryProvider(get()),
            )
        )
    }
    single<AudioProvider> {
        InnerTubeAudioProvider(
            fallback = YtDlpAudioProvider(get())
        )
    }
    single { MediaSessionManager(httpClient = get()) }
    single { DiscordRpcManager() }

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
    factory { SearchAlbumsUseCase(get()) }
    factory { SearchArtistsUseCase(get()) }
    factory { SearchPlaylistsUseCase(get()) }
    factory { LoadMoreTracksUseCase(get()) }
    factory { LoadMoreAlbumsUseCase(get()) }
    factory { LoadMoreArtistsUseCase(get()) }
    factory { LoadMorePlaylistsUseCase(get()) }
    factory { GetTrackUseCase(get()) }
    factory { GetLyricsUseCase(get()) }
    factory { GetSyncedLyricsUseCase(get()) }
    factory { GetSimilarTracksUseCase(get()) }
    factory { GetEntityDetailsUseCase(get()) }
    factory { GetArtistTagsUseCase(get()) }
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
    factory { SearchInteractors(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { LibraryInteractors(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { PlaybackInteractors(get(), get(), get(), get(), get()) }
    factory { OfflineInteractors(get(), get(), get(), get(), get(), get()) }
    factory { StatsInteractors(get(), get(), get()) }
    factory { SessionInteractors(get(), get(), get()) }
    factory { SettingsInteractors(get(), get()) }
}