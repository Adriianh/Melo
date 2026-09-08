package com.github.adriianh.melo.di

import com.github.adriianh.core.domain.manager.DownloadManager
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.core.domain.provider.MetadataProvider
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.repository.FavoritesRepository
import com.github.adriianh.core.domain.repository.HistoryRepository
import com.github.adriianh.core.domain.repository.LoginRepository
import com.github.adriianh.core.domain.repository.LyricsRepository
import com.github.adriianh.core.domain.repository.MusicRepository
import com.github.adriianh.core.domain.repository.PlaylistRepository
import com.github.adriianh.core.domain.repository.RemoteLibraryRepository
import com.github.adriianh.core.domain.repository.SearchHistoryRepository
import com.github.adriianh.core.domain.repository.SessionRepository
import com.github.adriianh.core.domain.repository.SettingsRepository
import com.github.adriianh.core.domain.repository.UpdateRepository
import com.github.adriianh.core.domain.usecase.library.AddTrackToPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.AddTracksToPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.CreatePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.DeletePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.GetAccountProfileUseCase
import com.github.adriianh.core.domain.usecase.library.GetLikedSongsUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistIdsForTrackUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistTracksUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetRemoteHistoryUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserAlbumsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserArtistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.ObserveLibraryUpdatesUseCase
import com.github.adriianh.core.domain.usecase.library.RemoveTrackFromPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.RenamePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.ReorderPlaylistTracksUseCase
import com.github.adriianh.core.domain.usecase.library.SubscribeChannelUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeAlbumUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeTrackUseCase
import com.github.adriianh.core.domain.usecase.login.SetSessionCookiesUseCase
import com.github.adriianh.core.domain.usecase.login.VerifySessionUseCase
import com.github.adriianh.core.domain.usecase.lyrics.GetTrackLyricsUseCase
import com.github.adriianh.core.domain.usecase.lyrics.TranslateLyricsUseCase
import com.github.adriianh.core.domain.usecase.offline.AutoCleanupUseCase
import com.github.adriianh.core.domain.usecase.offline.DeleteDownloadedTrackUseCase
import com.github.adriianh.core.domain.usecase.offline.DownloadTrackUseCase
import com.github.adriianh.core.domain.usecase.offline.EnrichLocalTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.GetOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.MarkTrackAccessedUseCase
import com.github.adriianh.core.domain.usecase.offline.ScanLocalTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.SyncOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.UpdateTrackMetadataUseCase
import com.github.adriianh.core.domain.usecase.playback.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.playback.RecordPlayUseCase
import com.github.adriianh.core.domain.usecase.search.BrowseCategoryUseCase
import com.github.adriianh.core.domain.usecase.search.DeleteSearchQueryUseCase
import com.github.adriianh.core.domain.usecase.search.GetArtistRadioUseCase
import com.github.adriianh.core.domain.usecase.search.GetChartsUseCase
import com.github.adriianh.core.domain.usecase.search.GetEntityDetailsUseCase
import com.github.adriianh.core.domain.usecase.search.GetExploreUseCase
import com.github.adriianh.core.domain.usecase.search.GetHomeUseCase
import com.github.adriianh.core.domain.usecase.search.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.GetMoodAndGenresUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
import com.github.adriianh.core.domain.usecase.search.GetRelatedTracksUseCase
import com.github.adriianh.core.domain.usecase.search.GetSearchHistoryUseCase
import com.github.adriianh.core.domain.usecase.search.GetSearchSuggestionsUseCase
import com.github.adriianh.core.domain.usecase.search.GetSyncedLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrendingUseCase
import com.github.adriianh.core.domain.usecase.search.SaveSearchQueryUseCase
import com.github.adriianh.core.domain.usecase.search.SearchAlbumsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchArtistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchSummaryUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.adriianh.core.domain.usecase.search.SearchVideosUseCase
import com.github.adriianh.core.domain.usecase.session.ClearSessionUseCase
import com.github.adriianh.core.domain.usecase.session.RestoreSessionUseCase
import com.github.adriianh.core.domain.usecase.session.SaveSessionUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.adriianh.core.domain.usecase.update.CheckForUpdateUseCase
import com.github.adriianh.core.domain.usecase.update.DownloadUpdateUseCase
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.data.manager.DownloadManagerImpl
import com.github.adriianh.data.player.PlaybackManagerImpl
import com.github.adriianh.data.provider.artwork.CompositeArtworkProvider
import com.github.adriianh.data.provider.artwork.DeezerArtworkProvider
import com.github.adriianh.data.provider.artwork.ItunesArtworkProvider
import com.github.adriianh.data.provider.audio.PipedAudioProvider
import com.github.adriianh.data.provider.discovery.InnerTubeDiscoveryProvider
import com.github.adriianh.data.provider.music.InnerTubeMusicProvider
import com.github.adriianh.data.remote.itunes.ItunesApiClient
import com.github.adriianh.data.remote.lyrics.LyricsApiClient
import com.github.adriianh.data.remote.lyrics.LyricsTranslator
import com.github.adriianh.data.remote.piped.PipedApiClient
import com.github.adriianh.data.repository.FavoritesRepositoryImpl
import com.github.adriianh.data.repository.HistoryRepositoryImpl
import com.github.adriianh.data.repository.InnerTubeLoginRepository
import com.github.adriianh.data.repository.LyricsRepositoryImpl
import com.github.adriianh.data.repository.MusicRepositoryImpl
import com.github.adriianh.data.repository.PlaylistRepositoryImpl
import com.github.adriianh.data.repository.RemoteLibraryRepositoryImpl
import com.github.adriianh.data.repository.SearchHistoryRepositoryImpl
import com.github.adriianh.data.repository.SessionRepositoryImpl
import com.github.adriianh.data.repository.SettingsRepositoryImpl
import com.github.adriianh.data.repository.UpdateRepositoryImpl
import com.github.adriianh.melo.ui.SidebarViewModel
import com.github.adriianh.melo.ui.detail.EntityDetailViewModel
import com.github.adriianh.melo.ui.home.HomeViewModel
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.login.LoginViewModel
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.SearchViewModel
import com.github.adriianh.melo.ui.settings.UpdateViewModel
import com.github.adriianh.melo.util.PlatformUpdateInstaller
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Common module for shared logic that doesn't depend on platform-specific APIs.
 */
val commonModule = module {
    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                    isLenient = true
                })
            }
        }
    }
}

/**
 * Data module for repositories and providers.
 */
val dataModule = module {
    single<MusicProvider> { InnerTubeMusicProvider() }
    single<DiscoveryProvider> { InnerTubeDiscoveryProvider() }
    single<LoginRepository> { InnerTubeLoginRepository() }

    singleOf(::ItunesApiClient)
    singleOf(::PipedApiClient)
    singleOf(::PipedAudioProvider)
    singleOf(::LyricsApiClient)
    singleOf(::LyricsTranslator)
    single<LyricsRepository> { LyricsRepositoryImpl(get(), get()) }

    single<MetadataProvider> {
        CompositeArtworkProvider(
            DeezerArtworkProvider(get()),
            ItunesArtworkProvider(get())
        )
    }

    single<MusicRepository> {
        MusicRepositoryImpl(
            musicProvider = get(),
            audioProvider = getOrNull(),
            discoveryProvider = getOrNull(),
            metadataProvider = getOrNull()
        )
    }

    singleOf(::PlaylistRepositoryImpl) { bind<PlaylistRepository>() }
    single<HistoryRepository> {
        HistoryRepositoryImpl(
            database = get(),
            settingsRepository = get()
        )
    }
    singleOf(::FavoritesRepositoryImpl) { bind<FavoritesRepository>() }
    singleOf(::SearchHistoryRepositoryImpl) { bind<SearchHistoryRepository>() }
    singleOf(::SessionRepositoryImpl) { bind<SessionRepository>() }
    singleOf(::RemoteLibraryRepositoryImpl) { bind<RemoteLibraryRepository>() }
    single<SettingsRepository> {
        SettingsRepositoryImpl(
            configDirPath = get<String>(named("configDirPath")),
            dispatcher = MeloDispatchers.IO
        )
    }

    single<DownloadManager> {
        DownloadManagerImpl(
            httpClient = get(),
            getStreamUseCase = get(),
            getSettingsUseCase = get(),
            offlineRepository = get(),
            configDirPath = get<String>(named("configDirPath")),
            dispatcher = MeloDispatchers.IO
        )
    }

    single { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    single<PlaybackManager> {
        PlaybackManagerImpl(
            meloPlayer = get(),
            getStreamUseCase = get(),
            scope = get(),
            getRadioUseCase = getOrNull(),
            getSettingsUseCase = getOrNull(),
            downloadManager = getOrNull(),
            offlineRepository = getOrNull(),
            recordPlayUseCase = getOrNull(),
            updateSettingsUseCase = getOrNull(),
            saveSessionUseCase = getOrNull(),
            restoreSessionUseCase = getOrNull(),
            clearSessionUseCase = getOrNull(),
            ioDispatcher = MeloDispatchers.IO,
        )
    }

    single<UpdateRepository> {
        UpdateRepositoryImpl(
            httpClient = get(),
            dispatcher = MeloDispatchers.IO
        )
    }
    singleOf(::PlatformUpdateInstaller)
}

val useCaseModule = module {
    singleOf(::GetStreamUseCase)
    singleOf(::RecordPlayUseCase)
    singleOf(::GetRecentTracksUseCase)
    singleOf(::GetLyricsUseCase)
    singleOf(::GetSyncedLyricsUseCase)
    singleOf(::GetTrackLyricsUseCase)
    singleOf(::TranslateLyricsUseCase)
    singleOf(::SearchTracksUseCase)
    singleOf(::SearchAlbumsUseCase)
    singleOf(::SearchArtistsUseCase)
    singleOf(::SearchPlaylistsUseCase)
    singleOf(::SearchVideosUseCase)
    singleOf(::SearchSummaryUseCase)
    singleOf(::GetHomeUseCase)
    singleOf(::GetExploreUseCase)
    singleOf(::GetChartsUseCase)
    singleOf(::GetMoodAndGenresUseCase)
    singleOf(::GetTrendingUseCase)
    singleOf(::GetRadioUseCase)
    singleOf(::GetArtistRadioUseCase)
    singleOf(::GetRelatedTracksUseCase)
    singleOf(::GetSettingsUseCase)
    singleOf(::UpdateSettingsUseCase)
    singleOf(::SetSessionCookiesUseCase)
    singleOf(::VerifySessionUseCase)
    singleOf(::GetAccountProfileUseCase)
    singleOf(::GetUserPlaylistsUseCase)
    singleOf(::GetPlaylistsUseCase)
    singleOf(::GetPlaylistTracksUseCase)
    singleOf(::CreatePlaylistUseCase)
    singleOf(::RenamePlaylistUseCase)
    singleOf(::DeletePlaylistUseCase)
    singleOf(::AddTrackToPlaylistUseCase)
    singleOf(::AddTracksToPlaylistUseCase)
    singleOf(::RemoveTrackFromPlaylistUseCase)
    singleOf(::ReorderPlaylistTracksUseCase)
    singleOf(::GetPlaylistIdsForTrackUseCase)
    singleOf(::GetLikedSongsUseCase)
    singleOf(::GetUserArtistsUseCase)
    singleOf(::GetUserAlbumsUseCase)
    singleOf(::GetRemoteHistoryUseCase)
    singleOf(::ObserveLibraryUpdatesUseCase)
    singleOf(::ToggleLikeTrackUseCase)
    singleOf(::ToggleLikeAlbumUseCase)
    singleOf(::ToggleLikePlaylistUseCase)
    singleOf(::SubscribeChannelUseCase)
    singleOf(::GetEntityDetailsUseCase)
    singleOf(::GetSearchHistoryUseCase)
    singleOf(::SaveSearchQueryUseCase)
    singleOf(::DeleteSearchQueryUseCase)
    singleOf(::GetSearchSuggestionsUseCase)
    singleOf(::BrowseCategoryUseCase)
    singleOf(::GetOfflineTracksUseCase)
    singleOf(::DownloadTrackUseCase)
    singleOf(::DeleteDownloadedTrackUseCase)
    singleOf(::ScanLocalTracksUseCase)
    singleOf(::EnrichLocalTracksUseCase)
    singleOf(::SyncOfflineTracksUseCase)
    singleOf(::AutoCleanupUseCase)
    singleOf(::MarkTrackAccessedUseCase)
    singleOf(::UpdateTrackMetadataUseCase)
    singleOf(::CheckForUpdateUseCase)
    singleOf(::DownloadUpdateUseCase)
    singleOf(::SaveSessionUseCase)
    singleOf(::RestoreSessionUseCase)
    singleOf(::ClearSessionUseCase)
}

/**
 * ViewModel module for all shared ViewModels.
 */
val viewModelModule = module {
    viewModel {
        PlayerViewModel(
            manager = get(),
            httpClient = get(),
            toggleLikeTrackUseCase = get(),
            recordPlayUseCase = get(),
            musicProvider = get(),
            getTrackLyricsUseCase = get(),
            translateLyricsUseCase = get(),
            getLikedSongsUseCase = getOrNull<GetLikedSongsUseCase>(),
            observeLibraryUpdatesUseCase = getOrNull<ObserveLibraryUpdatesUseCase>(),
        )
    }
    viewModelOf(::QueueViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::LoginViewModel)
    viewModel {
        LibraryViewModel(
            getSettingsUseCase = get(),
            updateSettingsUseCase = get(),
            getAccountProfileUseCase = get(),
            getUserPlaylistsUseCase = get(),
            getPlaylistsUseCase = get(),
            createPlaylistUseCase = get(),
            renamePlaylistUseCase = get(),
            deletePlaylistUseCase = get(),
            addTrackToPlaylistUseCase = get(),
            addTracksToPlaylistUseCase = get(),
            getPlaylistIdsForTrackUseCase = get(),
            getLikedSongsUseCase = get(),
            getUserArtistsUseCase = get(),
            getUserAlbumsUseCase = get(),
            getRemoteHistoryUseCase = get(),
            getRecentTracksUseCase = get(),
            toggleLikeTrackUseCase = get(),
            getOfflineTracksUseCase = get(),
            scanLocalTracksUseCase = get(),
            enrichLocalTracksUseCase = get(),
            deleteDownloadedTrackUseCase = get(),
            syncOfflineTracksUseCase = get(),
            downloadManager = get(),
            playbackManager = get(),
            observeLibraryUpdatesUseCase = get()
        )
    }
    viewModelOf(::SidebarViewModel)
    viewModelOf(::EntityDetailViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::UpdateViewModel)
}

expect val platformModule: Module
