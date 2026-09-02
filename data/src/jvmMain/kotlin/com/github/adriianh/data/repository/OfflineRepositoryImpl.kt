package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.repository.SettingsRepository
import com.github.adriianh.core.platform.PlatformFileSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.audio.exceptions.CannotReadVideoException
import org.jaudiotagger.tag.FieldKey
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

    private val artworkCacheDir =
        File(defaultDownloadsDir.parentFile, "artworks").apply { if (!exists()) mkdirs() }

    private fun extractArtwork(file: File, tag: org.jaudiotagger.tag.Tag?): String? {
        return try {
            val artBytes = tag?.firstArtwork?.binaryData
            if (artBytes != null && artBytes.isNotEmpty()) {
                val artFile = File(artworkCacheDir, "art_${file.absolutePath.hashCode()}.jpg")
                if (!artFile.exists() || artFile.length() == 0L) {
                    artFile.writeBytes(artBytes)
                }
                "file://${artFile.absolutePath}"
            } else {
                val parent = file.parentFile
                if (parent != null && parent.exists()) {
                    val coverNames = listOf(
                        "cover.jpg",
                        "cover.png",
                        "folder.jpg",
                        "folder.png",
                        "front.jpg",
                        "front.png"
                    )
                    val localCover =
                        coverNames.map { File(parent, it) }.firstOrNull { it.exists() && it.isFile }
                    localCover?.let { "file://${it.absolutePath}" }
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts metadata from the audio file tags.
     */
    private fun getFileMetadata(file: File): TrackMetadata? {
        return try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag
            val header: AudioHeader = audioFile.audioHeader

            val title = tag?.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() }
            val artist = (tag?.getFirst(FieldKey.ARTIST)
                ?: tag?.getFirst(FieldKey.ALBUM_ARTIST))?.takeIf { it.isNotBlank() }
            val album = tag?.getFirst(FieldKey.ALBUM)?.ifBlank { null }
            val durationMs = (header.trackLength * 1000L).takeIf { it > 0 } ?: 0L
            val artworkUrl = extractArtwork(file, tag)

            val safeTitle = title ?: run {
                val parts = file.nameWithoutExtension.split(" - ", limit = 2)
                if (parts.size == 2) parts[1] else parts[0]
            }
            val safeArtist = artist ?: run {
                val parts = file.nameWithoutExtension.split(" - ", limit = 2)
                if (parts.size == 2) parts[0] else "Artista Desconocido"
            }
            TrackMetadata(safeTitle, safeArtist, album ?: "", durationMs, artworkUrl)
        } catch (_: CannotReadVideoException) {
            null
        } catch (_: Exception) {
            val parts = file.nameWithoutExtension.split(" - ", limit = 2)
            val (artistPart, titlePart) = if (parts.size == 2) parts[0] to parts[1] else "Artista Desconocido" to parts[0]
            TrackMetadata(titlePart, artistPart, "", 0L, null)
        }
    }

    private data class TrackMetadata(
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val artworkUrl: String? = null
    )

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
        val current = getOfflineTracks().filter {
            it.downloadType == DownloadType.PREFETCH && it.downloadStatus == DownloadStatus.COMPLETED && (it.lastAccessedAt
                ?: it.downloadedAt ?: Long.MAX_VALUE) < cutoff
            }
        current.forEach { removeOfflineTrack(it.track.id) }
    }

    override suspend fun cleanupCache(maxSizeMb: Int) {
        val maxSizeBytes = maxSizeMb.toLong() * 1024 * 1024
        val current = _offlineTracksFlow.value.toMutableList()

        var totalSize = current.sumOf { it.fileSize }
        if (totalSize <= maxSizeBytes) return

        val sorted =
            current.filter { it.downloadStatus == DownloadStatus.COMPLETED && it.downloadType == DownloadType.PREFETCH }
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
            val customCacheDir = settings.cachePath?.let { File(it) } ?: File(
                defaultDownloadsDir.parentFile, "cache"
            )
            val customDownloadDir = settings.downloadPath?.let { File(it) }
            val current = _offlineTracksFlow.value.toMutableList()

            val updated = current.mapNotNull { track ->
                val localPath = track.localFilePath

                if (localPath != null) {
                    val file = File(localPath)
                    when {
                        file.exists() && track.downloadStatus != DownloadStatus.COMPLETED -> track.copy(
                            downloadStatus = DownloadStatus.COMPLETED,
                            fileSize = file.length(),
                            downloadedAt = track.downloadedAt ?: file.lastModified()
                        )

                        !file.exists() && track.downloadStatus == DownloadStatus.COMPLETED -> track.copy(
                            localFilePath = null,
                            downloadStatus = DownloadStatus.PENDING,
                            fileSize = 0L
                        )

                        else -> track
                    }
                } else {
                    when (track.downloadStatus) {
                        DownloadStatus.FAILED -> if (track.downloadType == DownloadType.PREFETCH) null
                        else track.copy(downloadStatus = DownloadStatus.PENDING)

                        DownloadStatus.DOWNLOADING -> if (track.downloadType == DownloadType.PREFETCH) null
                        else track.copy(downloadStatus = DownloadStatus.PENDING)

                        else -> track
                    }
                }
            }

            if (updated != current) {
                _offlineTracksFlow.value = updated
                saveMetadataToDisk(updated)
            }

            val validPaths = updated.mapNotNull { it.localFilePath }.map { it.lowercase() }.toSet()

            val audioExtensions = setOf("mp3", "flac", "m4a", "opus", "ogg", "wav", "aac")
            val metadataExtensions = setOf("webp", "png", "jpg", "jpeg")

            val dirsToClean =
                listOfNotNull(customCacheDir, customDownloadDir).filter { it.exists() }
            dirsToClean.forEach { dir ->
                dir.listFiles()?.forEach { file ->
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
        val audioExtensions = setOf(
            "mp3",
            "flac",
            "m4a",
            "opus",
            "ogg",
            "oga",
            "wav",
            "wave",
            "aac",
            "wma",
            "m4b",
            "m4p",
            "aiff",
            "aif"
        )
        val results = mutableListOf<Track>()

        return withContext(dispatcher) {
            val effectivePaths = paths.ifEmpty { PlatformFileSystem.getDefaultMusicPaths() }
            effectivePaths.forEach { path ->
                val dir = File(path)
                if (dir.exists() && dir.isDirectory) {
                    try {
                        dir.walkTopDown().maxDepth(12)
                            .filter { it.isFile && it.extension.lowercase() in audioExtensions }
                            .forEach { file ->
                                val metadata = getFileMetadata(file) ?: return@forEach
                                results.add(
                                    Track(
                                        id = "local:${file.absolutePath}",
                                        title = metadata.title,
                                        artist = metadata.artist,
                                        album = metadata.album,
                                        durationMs = metadata.durationMs,
                                        genres = emptyList(),
                                        artworkUrl = metadata.artworkUrl,
                                        sourceId = null
                                    )
                                )
                            }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            results.distinctBy { it.id }
        }
    }

    override suspend fun updateTrackMetadata(
        trackId: String, title: String?, artist: String?, album: String?
    ) {
        val path = if (trackId.startsWith("local:")) {
            trackId.removePrefix("local:")
        } else if (trackId.startsWith("file:")) {
            trackId.removePrefix("file:")
        } else {
            getOfflineTrack(trackId)?.localFilePath
        }

        withContext(dispatcher) {
            if (path != null) {
                val file = File(path)
                if (file.exists() && file.isFile) {
                    try {
                        val audioFile = AudioFileIO.read(file)
                        val tag = audioFile.tag ?: audioFile.createDefaultTag()

                        title?.let { tag.setField(FieldKey.TITLE, it) }
                        artist?.let { tag.setField(FieldKey.ARTIST, it) }
                        album?.let { tag.setField(FieldKey.ALBUM, it) }

                        audioFile.commit()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val current = _offlineTracksFlow.value.toMutableList()
            val index = current.indexOfFirst { it.track.id == trackId }
            if (index != -1) {
                val updatedTrack = current[index].track.copy(
                    title = title ?: current[index].track.title,
                    artist = artist ?: current[index].track.artist,
                    album = album ?: current[index].track.album
                )
                current[index] = current[index].copy(track = updatedTrack)
                _offlineTracksFlow.value = current
                saveMetadataToDisk(current)
            }
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