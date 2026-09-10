package com.github.adriianh.melo.di

import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.IosMeloPlayer
import com.github.adriianh.core.domain.player.MediaSessionManager
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.player.NoOpMediaSessionManager
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.data.local.DatabaseFactory
import com.github.adriianh.data.local.MeloDatabase
import com.github.adriianh.data.provider.audio.InnerTubeAudioProvider
import com.github.adriianh.data.provider.audio.PipedAudioProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual val platformModule: Module = module {
    single<MeloDatabase> { DatabaseFactory.create() }
    single<MeloPlayer> { IosMeloPlayer() }
    single<MediaSessionManager> { NoOpMediaSessionManager() }

    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val documentsDirectory = paths.first() as String
    single(named("configDirPath")) { documentsDirectory }

    single<AudioProvider>(createdAtStart = true) {
        val pipedProvider = PipedAudioProvider(apiClient = get())
        InnerTubeAudioProvider(
            configDirPath = get(named("configDirPath")),
            fallback = pipedProvider,
            settingsRepository = get()
        )
    }

    single<OfflineRepository> {
        object : OfflineRepository {
            override fun getOfflineTracksFlow(): Flow<List<OfflineTrack>> = flowOf(emptyList())
            override suspend fun getOfflineTracks(): List<OfflineTrack> = emptyList()
            override suspend fun getOfflineTrack(trackId: String): OfflineTrack? = null
            override suspend fun saveOfflineTrack(offlineTrack: OfflineTrack) {}
            override suspend fun removeOfflineTrack(trackId: String) {}
            override suspend fun markTrackAsAccessed(trackId: String) {}
            override suspend fun cleanupExpired(maxAgeDays: Int) {}
            override suspend fun cleanupCache(maxSizeMb: Int) {}
            override suspend fun syncWithFileSystem() {}
            override suspend fun scanLocalTracks(paths: List<String>): List<Track> = emptyList()
            override suspend fun updateTrackMetadata(
                id: String,
                t: String?,
                a: String?,
                al: String?
            ) {
            }
        }
    }
}
