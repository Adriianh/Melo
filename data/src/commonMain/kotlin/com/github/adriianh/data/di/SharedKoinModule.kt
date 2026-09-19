package com.github.adriianh.data.di

import com.github.adriianh.core.domain.cache.HomeFeedCache
import com.github.adriianh.core.domain.provider.MetadataProvider
import com.github.adriianh.core.domain.repository.DiscoveryRepository
import com.github.adriianh.core.domain.repository.FavoritesRepository
import com.github.adriianh.core.domain.repository.LoginRepository
import com.github.adriianh.core.domain.repository.LyricsRepository
import com.github.adriianh.core.domain.repository.MusicRepository
import com.github.adriianh.core.domain.repository.PlaylistRepository
import com.github.adriianh.core.domain.repository.RemoteLibraryRepository
import com.github.adriianh.core.domain.repository.SearchHistoryRepository
import com.github.adriianh.core.domain.repository.SessionRepository
import com.github.adriianh.core.domain.repository.SettingsRepository
import com.github.adriianh.core.domain.repository.StreamCacheRepository
import com.github.adriianh.core.domain.usecase.library.AddFavoriteUseCase
import com.github.adriianh.core.domain.usecase.library.AddTrackToPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.AddTracksToPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.CreatePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.DeletePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.GetAccountProfileUseCase
import com.github.adriianh.core.domain.usecase.library.GetFavoritesUseCase
import com.github.adriianh.core.domain.usecase.library.GetLikedSongsUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistIdsForTrackUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistTracksUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetRemoteHistoryUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserAlbumsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserArtistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.IsFavoriteUseCase
import com.github.adriianh.core.domain.usecase.library.ObserveLibraryUpdatesUseCase
import com.github.adriianh.core.domain.usecase.library.RemoveFavoriteUseCase
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
import com.github.adriianh.core.domain.usecase.playback.AuthenticateLastFmUseCase
import com.github.adriianh.core.domain.usecase.playback.CompleteWebAuthUseCase
import com.github.adriianh.core.domain.usecase.playback.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.playback.RecordPlayUseCase
import com.github.adriianh.core.domain.usecase.playback.ScrobbleUseCase
import com.github.adriianh.core.domain.usecase.playback.StartWebAuthUseCase
import com.github.adriianh.core.domain.usecase.playback.UpdateNowPlayingUseCase
import com.github.adriianh.core.domain.usecase.search.BrowseCategoryUseCase
import com.github.adriianh.core.domain.usecase.search.DeleteSearchQueryUseCase
import com.github.adriianh.core.domain.usecase.search.GetArtistRadioUseCase
import com.github.adriianh.core.domain.usecase.search.GetArtistTagsUseCase
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
import com.github.adriianh.core.domain.usecase.search.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.search.GetSyncedLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrackUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrendingUseCase
import com.github.adriianh.core.domain.usecase.search.LoadMoreAlbumsUseCase
import com.github.adriianh.core.domain.usecase.search.LoadMoreArtistsUseCase
import com.github.adriianh.core.domain.usecase.search.LoadMorePlaylistsUseCase
import com.github.adriianh.core.domain.usecase.search.LoadMoreTracksUseCase
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
import com.github.adriianh.core.domain.usecase.stats.GetListeningStatsUseCase
import com.github.adriianh.core.domain.usecase.stats.GetTopArtistsUseCase
import com.github.adriianh.core.domain.usecase.stats.GetTopTracksUseCase
import com.github.adriianh.core.domain.usecase.update.CheckForUpdateUseCase
import com.github.adriianh.core.domain.usecase.update.DownloadUpdateUseCase
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.data.cache.HomeFeedCacheImpl
import com.github.adriianh.data.provider.artwork.CompositeArtworkProvider
import com.github.adriianh.data.provider.artwork.DeezerArtworkProvider
import com.github.adriianh.data.provider.artwork.ItunesArtworkProvider
import com.github.adriianh.data.remote.deezer.DeezerApiClient
import com.github.adriianh.data.remote.itunes.ItunesApiClient
import com.github.adriianh.data.remote.lyrics.LyricsApiClient
import com.github.adriianh.data.remote.lyrics.LyricsTranslator
import com.github.adriianh.data.remote.piped.PipedApiClient
import com.github.adriianh.data.repository.DiscoveryRepositoryImpl
import com.github.adriianh.data.repository.FavoritesRepositoryImpl
import com.github.adriianh.data.repository.InnerTubeLoginRepository
import com.github.adriianh.data.repository.LyricsRepositoryImpl
import com.github.adriianh.data.repository.MusicRepositoryImpl
import com.github.adriianh.data.repository.PlaylistRepositoryImpl
import com.github.adriianh.data.repository.RemoteLibraryRepositoryImpl
import com.github.adriianh.data.repository.SearchHistoryRepositoryImpl
import com.github.adriianh.data.repository.SessionRepositoryImpl
import com.github.adriianh.data.repository.SettingsRepositoryImpl
import com.github.adriianh.data.repository.StreamCacheRepositoryImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Shared Koin bindings used by every Melo frontend (Compose app and TUI).
 *
 * Only bindings whose dependency graph is satisfiable by all apps live here:
 * remote API clients, data-layer repositories/caches and use-case factories.
 * App/platform-specific bindings (HttpClient config, providers, `MeloPlayer`,
 * `PlaybackManager`, offline/tracking repos, UI wiring) stay in each app's
 * own module.
 *
 * Repos that differ between apps on purpose (e.g. `HistoryRepository`) are
 * also kept per-app to preserve their current behavior.
 */
val sharedModule: Module = module {

    // ── Remote API clients ───────────────────────────────────────────────────
    singleOf(::ItunesApiClient)
    singleOf(::PipedApiClient)
    singleOf(::LyricsApiClient)
    singleOf(::LyricsTranslator)
    singleOf(::DeezerApiClient)

    // ── Login / auth ─────────────────────────────────────────────────────────
    singleOf(::InnerTubeLoginRepository) { bind<LoginRepository>() }

    // ── Metadata / artwork ───────────────────────────────────────────────────
    single<MetadataProvider> {
        CompositeArtworkProvider(
            DeezerArtworkProvider(get()),
            ItunesArtworkProvider(get())
        )
    }

    // ── Repositories ─────────────────────────────────────────────────────────
    single<MusicRepository> {
        MusicRepositoryImpl(
            musicProvider = get(),
            audioProvider = getOrNull(),
            discoveryProvider = getOrNull(),
            metadataProvider = getOrNull()
        )
    }
    single<LyricsRepository> { LyricsRepositoryImpl(get(), get()) }
    single<DiscoveryRepository> { DiscoveryRepositoryImpl(get()) }

    singleOf(::PlaylistRepositoryImpl) { bind<PlaylistRepository>() }
    singleOf(::FavoritesRepositoryImpl) { bind<FavoritesRepository>() }
    singleOf(::SearchHistoryRepositoryImpl) { bind<SearchHistoryRepository>() }
    singleOf(::StreamCacheRepositoryImpl) { bind<StreamCacheRepository>() }
    singleOf(::SessionRepositoryImpl) { bind<SessionRepository>() }
    singleOf(::RemoteLibraryRepositoryImpl) { bind<RemoteLibraryRepository>() }

    single<SettingsRepository> {
        SettingsRepositoryImpl(
            configDirPath = get<String>(named("configDirPath")),
            dispatcher = MeloDispatchers.IO
        )
    }

    // ── Caches ───────────────────────────────────────────────────────────────
    single<HomeFeedCache> {
        HomeFeedCacheImpl(
            configDirPath = get<String>(named("configDirPath")),
            dispatcher = MeloDispatchers.IO
        )
    }

    // ── Use-case factories (union of every app's needs) ──────────────────────
    singleOf(::GetStreamUseCase)
    singleOf(::RecordPlayUseCase)
    singleOf(::GetRecentTracksUseCase)
    singleOf(::GetLyricsUseCase)
    singleOf(::GetSyncedLyricsUseCase)
    singleOf(::GetTrackLyricsUseCase)
    singleOf(::TranslateLyricsUseCase)
    singleOf(::GetTrackUseCase)
    singleOf(::GetSimilarTracksUseCase)
    singleOf(::GetArtistTagsUseCase)
    singleOf(::SearchTracksUseCase)
    singleOf(::SearchAlbumsUseCase)
    singleOf(::SearchArtistsUseCase)
    singleOf(::SearchPlaylistsUseCase)
    singleOf(::SearchVideosUseCase)
    singleOf(::SearchSummaryUseCase)
    singleOf(::LoadMoreTracksUseCase)
    singleOf(::LoadMoreAlbumsUseCase)
    singleOf(::LoadMoreArtistsUseCase)
    singleOf(::LoadMorePlaylistsUseCase)
    singleOf(::GetHomeUseCase)
    singleOf(::GetExploreUseCase)
    singleOf(::GetChartsUseCase)
    singleOf(::GetMoodAndGenresUseCase)
    singleOf(::GetTrendingUseCase)
    singleOf(::GetRadioUseCase)
    singleOf(::GetArtistRadioUseCase)
    singleOf(::GetRelatedTracksUseCase)
    singleOf(::GetEntityDetailsUseCase)
    singleOf(::GetSearchHistoryUseCase)
    singleOf(::GetSearchSuggestionsUseCase)
    singleOf(::SaveSearchQueryUseCase)
    singleOf(::DeleteSearchQueryUseCase)
    singleOf(::BrowseCategoryUseCase)
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
    singleOf(::GetFavoritesUseCase)
    singleOf(::AddFavoriteUseCase)
    singleOf(::RemoveFavoriteUseCase)
    singleOf(::IsFavoriteUseCase)
    singleOf(::GetOfflineTracksUseCase)
    singleOf(::DownloadTrackUseCase)
    singleOf(::DeleteDownloadedTrackUseCase)
    singleOf(::ScanLocalTracksUseCase)
    singleOf(::EnrichLocalTracksUseCase)
    singleOf(::SyncOfflineTracksUseCase)
    singleOf(::AutoCleanupUseCase)
    singleOf(::MarkTrackAccessedUseCase)
    singleOf(::UpdateTrackMetadataUseCase)
    singleOf(::UpdateNowPlayingUseCase)
    singleOf(::ScrobbleUseCase)
    singleOf(::AuthenticateLastFmUseCase)
    singleOf(::StartWebAuthUseCase)
    singleOf(::CompleteWebAuthUseCase)
    singleOf(::GetTopTracksUseCase)
    singleOf(::GetTopArtistsUseCase)
    singleOf(::GetListeningStatsUseCase)
    singleOf(::SaveSessionUseCase)
    singleOf(::RestoreSessionUseCase)
    singleOf(::ClearSessionUseCase)
    singleOf(::CheckForUpdateUseCase)
    singleOf(::DownloadUpdateUseCase)
}