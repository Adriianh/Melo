package com.github.adriianh.cli.command

import com.github.adriianh.cli.command.config.ConfigCommand
import com.github.adriianh.cli.command.player.SearchCommand
import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.core.domain.provider.ArtworkProvider
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.usecase.*
import com.github.adriianh.data.remote.piped.PipedApiClient
import io.ktor.client.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.getValue
import kotlin.system.exitProcess

class MeloCommand : CliktCommand(
    name = "melo"
), KoinComponent {
    init {
        subcommands(
            SearchCommand(),
            ConfigCommand(),
        )
    }

    override val invokeWithoutSubcommand: Boolean = true

    override fun help(context: Context): String = Messages.get("help.melo_command")

    override fun run() {
        if (currentContext.invokedSubcommand != null) return

        if (resolveEnv("LASTFM_API_KEY") == null) {
            echo(Messages.get("error.missing_lastfm_key", "configDir" to configDir), err = true)
            exitProcess(1)
        }

        startKoin { modules(appModule) }

        val searchTracks: SearchTracksUseCase by inject()
        val loadMoreTracks: LoadMoreTracksUseCase by inject()
        val getTrack: GetTrackUseCase by inject()
        val getLyrics: GetLyricsUseCase by inject()
        val getSyncedLyrics: GetSyncedLyricsUseCase by inject()
        val getSimilarTracks: GetSimilarTracksUseCase by inject()
        val getFavorites: GetFavoritesUseCase by inject()
        val addFavorite: AddFavoriteUseCase by inject()
        val removeFavorite: RemoveFavoriteUseCase by inject()
        val isFavorite: IsFavoriteUseCase by inject()
        val getRecentTracks: GetRecentTracksUseCase by inject()
        val recordPlay: RecordPlayUseCase by inject()
        val getStream: GetStreamUseCase by inject()
        val getPlaylists: GetPlaylistsUseCase by inject()
        val getPlaylistTracks: GetPlaylistTracksUseCase by inject()
        val createPlaylist: CreatePlaylistUseCase by inject()
        val renamePlaylist: RenamePlaylistUseCase by inject()
        val deletePlaylist: DeletePlaylistUseCase by inject()
        val addTrackToPlaylist: AddTrackToPlaylistUseCase by inject()
        val removeTrackFromPlaylist: RemoveTrackFromPlaylistUseCase by inject()
        val saveSession: SaveSessionUseCase by inject()
        val restoreSession: RestoreSessionUseCase by inject()
        val clearSession: ClearSessionUseCase by inject()
        val updateNowPlaying: UpdateNowPlayingUseCase by inject()
        val scrobble: ScrobbleUseCase by inject()
        val getTopTracks: GetTopTracksUseCase by inject()
        val getTopArtists: GetTopArtistsUseCase by inject()
        val getListeningStats: GetListeningStatsUseCase by inject()
        val getSettings: GetSettingsUseCase by inject()
        val updateSettings: UpdateSettingsUseCase by inject()
        val artworkRenderer: ArtworkRenderer by inject()
        val artworkProvider: ArtworkProvider by inject()
        val pipedApiClient: PipedApiClient by inject()
        val getOfflineTracks: GetOfflineTracksUseCase by inject()
        val downloadTrack: DownloadTrackUseCase by inject()
        val deleteDownloadedTrack: DeleteDownloadedTrackUseCase by inject()
        val markTrackAccessed: MarkTrackAccessedUseCase by inject()
        val autoCleanup: AutoCleanupUseCase by inject()
        val httpClient: HttpClient by inject()
        val dispatcher: CoroutineDispatcher by inject()
        val audioProvider: AudioProvider by inject()

        try {
            MeloScreen(
                httpClient = httpClient,
                searchTracks = searchTracks,
                loadMoreTracks = loadMoreTracks,
                getTrack = getTrack,
                getLyrics = getLyrics,
                getSyncedLyrics = getSyncedLyrics,
                getSimilarTracks = getSimilarTracks,
                pipedApiClient = pipedApiClient,
                getFavorites = getFavorites,
                addFavorite = addFavorite,
                removeFavorite = removeFavorite,
                isFavoriteUseCase = isFavorite,
                getRecentTracks = getRecentTracks,
                recordPlay = recordPlay,
                getStream = getStream,
                getPlaylists = getPlaylists,
                getPlaylistTracks = getPlaylistTracks,
                createPlaylist = createPlaylist,
                renamePlaylist = renamePlaylist,
                deletePlaylist = deletePlaylist,
                addTrackToPlaylist = addTrackToPlaylist,
                removeTrackFromPlaylist = removeTrackFromPlaylist,
                saveSession = saveSession,
                restoreSession = restoreSession,
                clearSession = clearSession,
                updateNowPlaying = updateNowPlaying,
                scrobble = scrobble,
                getTopTracks = getTopTracks,
                getTopArtists = getTopArtists,
                getListeningStats = getListeningStats,
                getSettings = getSettings,
                updateSettings = updateSettings,
                getOfflineTracks = getOfflineTracks,
                downloadTrack = downloadTrack,
                deleteDownloadedTrack = deleteDownloadedTrack,
                markTrackAccessed = markTrackAccessed,
                autoCleanup = autoCleanup,
                artworkRenderer = artworkRenderer,
                artworkProvider = artworkProvider,
                audioProvider = audioProvider,
                dispatcher = dispatcher
            ).run()
        } finally {
            stopKoin()
        }
    }
}
