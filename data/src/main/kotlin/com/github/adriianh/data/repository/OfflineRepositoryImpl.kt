package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class OfflineRepositoryImpl(
    dataDir: File,
    private val settingsRepository: SettingsRepository,
    private val dispatcher: CoroutineDispatcher
) : OfflineRepository {

    private val defaultDownloadsDir = File(dataDir, "cache")
    private val metadataFile = File(defaultDownloadsDir, "downloads.json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _offlineTracksFlow = MutableStateFlow(loadMetadataSync())

    override fun getOfflineTracksFlow(): Flow<List<OfflineTrack>> = _offlineTracksFlow.asStateFlow()

    override suspend fun getOfflineTracks(): List<OfflineTrack> = _offlineTracksFlow.value

    override suspend fun getOfflineTrack(trackId: String): OfflineTrack? {
        return _offlineTracksFlow.value.find { it.track.id == trackId }
    }

    override suspend fun saveOfflineTrack(offlineTrack: OfflineTrack) {
        val current = _offlineTracksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.track.id == offlineTrack.track.id }
        if (index != -1) current[index] = offlineTrack else current.add(offlineTrack)
        _offlineTracksFlow.value = current
        withContext(dispatcher) { saveMetadataToDisk(current) }
    }

    override suspend fun removeOfflineTrack(trackId: String) {
        val current = _offlineTracksFlow.value.toMutableList()
        val track = current.find { it.track.id == trackId } ?: return

        current.remove(track)
        _offlineTracksFlow.value = current

        withContext(dispatcher) {
            track.localFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
            saveMetadataToDisk(current)
        }
    }

    override suspend fun markTrackAsAccessed(trackId: String) {
        val current = _offlineTracksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.track.id == trackId }
        if (index != -1) {
            current[index] = current[index].copy(lastAccessedAt = System.currentTimeMillis())
            _offlineTracksFlow.value = current
            saveMetadataToDisk(current)
        }
    }

    override suspend fun cleanupExpired(maxAgeDays: Int) {
        val cutoff = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
        val current = getOfflineTracks()
            .filter {
                it.downloadType == DownloadType.PREFETCH &&
                        it.downloadStatus == DownloadStatus.COMPLETED &&
                        (it.lastAccessedAt ?: it.downloadedAt ?: 0L) < cutoff
            }
        current.forEach { removeOfflineTrack(it.track.id) }
    }

    override suspend fun cleanupCache(maxSizeMb: Int) {
        val maxSizeBytes = maxSizeMb.toLong() * 1024 * 1024
        val current = _offlineTracksFlow.value.toMutableList()

        var totalSize = current.sumOf { it.fileSize }
        if (totalSize <= maxSizeBytes) return

        val sorted = current
            .filter { it.downloadStatus == DownloadStatus.COMPLETED && it.downloadType == DownloadType.PREFETCH }
            .sortedBy { it.lastAccessedAt ?: it.downloadedAt ?: 0L }

        for (track in sorted) {
            if (totalSize <= maxSizeBytes) break
            track.localFilePath?.let { path ->
                withContext(dispatcher) {
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
            }
            current.remove(track)
            totalSize -= track.fileSize
        }

        _offlineTracksFlow.value = current
        saveMetadataToDisk(current)
    }

    override suspend fun syncWithFileSystem() {
        withContext(dispatcher) {
            val settings = settingsRepository.getSettings()
            val customCacheDir = settings.cachePath?.let { File(it) } ?: File(defaultDownloadsDir.parentFile, "cache")
            val current = _offlineTracksFlow.value.toMutableList()

            val updated = current.mapNotNull { track ->
                val localPath = track.localFilePath

                if (localPath != null) {
                    val file = File(localPath)
                    when {
                        file.exists() && track.downloadStatus != DownloadStatus.COMPLETED ->
                            track.copy(
                                downloadStatus = DownloadStatus.COMPLETED,
                                fileSize = file.length(),
                                downloadedAt = track.downloadedAt ?: file.lastModified()
                            )
                        !file.exists() && track.downloadStatus == DownloadStatus.COMPLETED ->
                            track.copy(
                                localFilePath = null,
                                downloadStatus = DownloadStatus.PENDING,
                                fileSize = 0L
                            )
                        else -> track
                    }
                } else {
                    when (track.downloadStatus) {
                        DownloadStatus.FAILED ->
                            if (track.downloadType == DownloadType.PREFETCH) null
                            else track.copy(downloadStatus = DownloadStatus.PENDING)

                        DownloadStatus.DOWNLOADING ->
                            if (track.downloadType == DownloadType.PREFETCH) null
                            else track.copy(downloadStatus = DownloadStatus.PENDING)

                        else -> track
                    }
                }
            }

            if (updated != current) {
                _offlineTracksFlow.value = updated
                saveMetadataToDisk(updated)
            }

            val validPaths = updated
                .mapNotNull { it.localFilePath }
                .map { it.lowercase() }
                .toSet()

            val audioExtensions = setOf("mp3", "flac", "m4a", "opus", "ogg", "wav", "aac")
            val metadataExtensions = setOf("webp", "png", "jpg", "jpeg")

            if (customCacheDir.exists()) {
                customCacheDir.listFiles()?.forEach { file ->
                    val isAudio = audioExtensions.any { ext ->
                        file.name.endsWith(".$ext", ignoreCase = true)
                    }
                    val isOrphanedAudio = isAudio && file.absolutePath.lowercase() !in validPaths
                    val isMetadata = metadataExtensions.any { ext ->
                        file.name.endsWith(".$ext", ignoreCase = true)
                    }

                    if (isOrphanedAudio || isMetadata) file.delete()
                }
            }
        }
    }

    override suspend fun scanLocalTracks(paths: List<String>): List<Track> {
        val audioExtensions = setOf("mp3", "flac", "m4a", "opus", "ogg", "wav", "aac")
        val results = mutableListOf<Track>()

        return withContext(dispatcher) {
            paths.forEach { path ->
                val dir = File(path)
                if (dir.exists() && dir.isDirectory) {
                    dir.walkTopDown()
                        .maxDepth(10)
                        .filter { it.isFile && it.extension.lowercase() in audioExtensions }
                        .forEach { file ->
                            val existing = _offlineTracksFlow.value.find { it.localFilePath == file.absolutePath }
                            if (existing != null) {
                                results.add(existing.track)
                            } else {
                                val name = file.nameWithoutExtension
                                val parts = name.split(" - ", limit = 2)
                                val (artist, title) = if (parts.size == 2) {
                                    parts[0] to parts[1]
                                } else {
                                    "Unknown Artist" to parts[0]
                                }

                                results.add(
                                    Track(
                                        id = "local:${file.absolutePath}",
                                        title = title,
                                        artist = artist,
                                        album = "",
                                        durationMs = 0,
                                        genres = emptyList(),
                                        artworkUrl = null,
                                        sourceId = null
                                    )
                                )
                            }
                        }
                }
            }
            results.distinctBy { it.id }
        }
    }

    private fun loadMetadataSync(): List<OfflineTrack> {
        if (!metadataFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<OfflineTrack>>(metadataFile.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun saveMetadataToDisk(tracks: List<OfflineTrack>) {
        withContext(dispatcher) {
            try {
                if (!defaultDownloadsDir.exists()) defaultDownloadsDir.mkdirs()
                val jsonString = json.encodeToString(tracks)
                metadataFile.writeText(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}