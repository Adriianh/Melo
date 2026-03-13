package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.OfflineTrack
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
        if (index != -1) {
            current[index] = offlineTrack
        } else {
            current.add(offlineTrack)
        }
        _offlineTracksFlow.value = current
        saveMetadataToDisk(current)
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

    override suspend fun cleanupCache(maxSizeMb: Int) {
        val maxSizeBytes = maxSizeMb.toLong() * 1024 * 1024
        val current = _offlineTracksFlow.value.toMutableList()

        var totalSize = current.sumOf { it.fileSize }
        if (totalSize <= maxSizeBytes) return

        val sorted = current.filter { it.downloadStatus == DownloadStatus.COMPLETED }
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

            // Get all audio files in the downloads directory
            val existingFiles = downloadsDir.listFiles { file ->
                audioExtensions.any { ext -> file.name.endsWith(".$ext", ignoreCase = true) }
            }?.associateBy { it.name.lowercase() } ?: emptyMap()

            var hasChanges = false

            for (i in current.indices) {
                val track = current[i]
                val localPath = track.localFilePath

                // Check if track has a local file path
                if (localPath != null) {
                    val file = File(localPath)

                    // If file exists but status is not COMPLETED, fix it
                    if (file.exists() && track.downloadStatus != DownloadStatus.COMPLETED) {
                        current[i] = track.copy(
                            downloadStatus = DownloadStatus.COMPLETED,
                            fileSize = file.length(),
                            downloadedAt = track.downloadedAt ?: file.lastModified()
                        )
                        hasChanges = true
                    }
                    // If file doesn't exist, reset to PENDING
                    else if (!file.exists()) {
                        current[i] = track.copy(
                            localFilePath = null,
                            downloadStatus = DownloadStatus.PENDING,
                            fileSize = 0L
                        )
                        hasChanges = true
                    }
                } else {
                    // Try to find a matching file for this track
                    val matchingFile = findMatchingFile(track.track, existingFiles)
                    if (matchingFile != null) {
                        current[i] = track.copy(
                            localFilePath = matchingFile.absolutePath,
                            downloadStatus = DownloadStatus.COMPLETED,
                            fileSize = matchingFile.length(),
                            downloadedAt = track.downloadedAt ?: matchingFile.lastModified()
                        )
                        hasChanges = true
                    }
                }
            }

            if (hasChanges) {
                _offlineTracksFlow.value = current
                saveMetadataToDisk(current)
            }
        }
    }

    private fun findMatchingFile(track: com.github.adriianh.core.domain.model.Track, files: Map<String, File>): File? {
        val title = track.title.lowercase()
        val artist = track.artist.lowercase()

        // Try to find a file that matches the track
        return files.values.firstOrNull { file ->
            val name = file.name.lowercase()
            // Match if filename contains both artist and title
            (name.contains(title) && name.contains(artist)) ||
            // Or if it contains the title (for tracks without clear artist in filename)
            name.contains(title)
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