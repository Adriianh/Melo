package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.OfflineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class OfflineRepositoryImpl(
    dataDir: File,
    private val dispatcher: CoroutineDispatcher
) : OfflineRepository {

    private val downloadsDir = File(dataDir, "downloads")
    private val metadataFile = File(downloadsDir, "downloads.json")
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
            val current = _offlineTracksFlow.value.toMutableList()
            val audioExtensions = setOf("mp3", "flac", "m4a", "opus", "ogg", "wav", "aac")

            val existingFiles = downloadsDir.listFiles { file ->
                audioExtensions.any { ext -> file.name.endsWith(".$ext", ignoreCase = true) }
            }?.associateBy { it.name.lowercase() } ?: emptyMap()

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
                        !file.exists() ->
                            track.copy(
                                localFilePath = null,
                                downloadStatus = DownloadStatus.PENDING,
                                fileSize = 0L
                            )
                        else -> track
                    }
                } else {
                    val matchingFile = findMatchingFile(track.track, existingFiles)
                    when {
                        matchingFile != null ->
                            track.copy(
                                localFilePath = matchingFile.absolutePath,
                                downloadStatus = DownloadStatus.COMPLETED,
                                fileSize = matchingFile.length(),
                                downloadedAt = track.downloadedAt ?: matchingFile.lastModified()
                            )
                        track.downloadStatus == DownloadStatus.FAILED &&
                                track.downloadType == DownloadType.PREFETCH -> null
                        track.downloadStatus == DownloadStatus.FAILED ->
                            track.copy(downloadStatus = DownloadStatus.PENDING)
                        track.downloadStatus == DownloadStatus.DOWNLOADING &&
                                track.downloadType == DownloadType.PREFETCH -> null
                        track.downloadStatus == DownloadStatus.DOWNLOADING ->
                            track.copy(downloadStatus = DownloadStatus.PENDING)
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

            val metadataExtensions = setOf("webp", "png", "jpg", "jpeg")

            downloadsDir.listFiles()?.forEach { file ->
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

    private fun findMatchingFile(track: Track, files: Map<String, File>): File? {
        val title = track.title.lowercase()
        val artist = track.artist.lowercase()

        return files.values.firstOrNull { file ->
            val name = file.name.lowercase()
            name.contains(title) && name.contains(artist)
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
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val jsonString = json.encodeToString(tracks)
                metadataFile.writeText(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}