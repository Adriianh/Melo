package com.github.adriianh.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
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
import org.jaudiotagger.audio.exceptions.CannotReadVideoException
import org.jaudiotagger.tag.FieldKey
import java.io.File

class AndroidOfflineRepositoryImpl(
    dataDir: File,
    private val settingsRepository: SettingsRepository,
    private val dispatcher: CoroutineDispatcher,
    private val context: Context? = null
) : OfflineRepository {

    private val defaultDownloadsDir = File(dataDir, "cache")
    private val metadataFile = File(defaultDownloadsDir, "downloads.json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _offlineTracksFlow = MutableStateFlow(loadMetadataSync())

    private val artworkCacheDir = File(dataDir, "artworks").apply { if (!exists()) mkdirs() }

    private fun extractEmbeddedArtwork(file: File, retriever: MediaMetadataRetriever): String? {
        return try {
            val picture = retriever.embeddedPicture
            if (picture != null && picture.isNotEmpty()) {
                val artFile = File(artworkCacheDir, "art_${file.absolutePath.hashCode()}.jpg")
                if (!artFile.exists() || artFile.length() == 0L) {
                    artFile.writeBytes(picture)
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
     * Extracts metadata from the audio file using Android's MediaMetadataRetriever and JAudioTagger fallback.
     */
    private fun getFileMetadata(file: File): TrackMetadata? {
        val retriever = MediaMetadataRetriever()
        try {
            if (file.exists() && file.canRead()) {
                java.io.FileInputStream(file).use { fis ->
                    retriever.setDataSource(fis.fd)
                }
            } else {
                retriever.setDataSource(file.absolutePath)
            }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
            val artist = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER))?.takeIf { it.isNotBlank() && it != "<unknown>" }
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() && it != "<unknown>" }
            val durationStr =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val artworkUrl = extractEmbeddedArtwork(file, retriever)

            val safeTitle = title ?: run {
                val name = file.nameWithoutExtension
                val parts = name.split(" - ", limit = 2)
                if (parts.size == 2) parts[1].trim() else name
            }
            val safeArtist = artist ?: run {
                val name = file.nameWithoutExtension
                val parts = name.split(" - ", limit = 2)
                if (parts.size == 2) parts[0].trim() else "Artista Desconocido"
            }

            return TrackMetadata(safeTitle, safeArtist, album ?: "", durationMs, artworkUrl)
        } catch (_: Exception) {
            try {
                val audioFile = AudioFileIO.read(file)
                val tag = audioFile.tag
                val title = tag?.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() }
                val artist = (tag?.getFirst(FieldKey.ARTIST)
                    ?: tag?.getFirst(FieldKey.ALBUM_ARTIST))?.takeIf { it.isNotBlank() }
                val album = tag?.getFirst(FieldKey.ALBUM)?.takeIf { it.isNotBlank() }
                val durationMs = (audioFile.audioHeader?.trackLength?.toLong() ?: 0L) * 1000L

                val safeTitle = title ?: run {
                    val name = file.nameWithoutExtension
                    val parts = name.split(" - ", limit = 2)
                    if (parts.size == 2) parts[1].trim() else name
                }
                val safeArtist = artist ?: run {
                    val name = file.nameWithoutExtension
                    val parts = name.split(" - ", limit = 2)
                    if (parts.size == 2) parts[0].trim() else "Artista Desconocido"
                }

                return TrackMetadata(safeTitle, safeArtist, album ?: "", durationMs, null)
            } catch (_: CannotReadVideoException) {
                return null
            } catch (__: Exception) {
                val name = file.nameWithoutExtension
                val parts = name.split(" - ", limit = 2)
                val (artistPart, titlePart) = if (parts.size == 2) parts[0].trim() to parts[1].trim() else "Artista Desconocido" to name
                return TrackMetadata(titlePart, artistPart, "", 0L, null)
            }
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
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
        val current = getOfflineTracks()
            .filter {
                it.downloadType != DownloadType.MANUAL &&
                        it.downloadStatus == DownloadStatus.COMPLETED &&
                        (it.lastAccessedAt ?: it.downloadedAt ?: Long.MAX_VALUE) < cutoff
            }
        current.forEach { removeOfflineTrack(it.track.id) }
    }

    override suspend fun cleanupCache(maxSizeMb: Int) {
        val maxSizeBytes = maxSizeMb.toLong() * 1024 * 1024
        val current = _offlineTracksFlow.value.toMutableList()

        var totalSize = current.sumOf { it.fileSize }
        if (totalSize <= maxSizeBytes) return

        val sorted = current
            .filter { it.downloadStatus == DownloadStatus.COMPLETED && it.downloadType != DownloadType.MANUAL }
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
                defaultDownloadsDir.parentFile,
                "cache"
            )
            val customDownloadDir = settings.downloadPath?.let { File(it) }
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
            if (context != null) {
                val mediaStoreUris = listOf(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Audio.Media.INTERNAL_CONTENT_URI
                )
                mediaStoreUris.forEach { contentUri ->
                    try {
                        val projection = arrayOf(
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.ALBUM_ID,
                            MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.DISPLAY_NAME
                        )
                        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                        val cursor = context.contentResolver.query(
                            contentUri,
                            projection,
                            selection,
                            null,
                            "${MediaStore.Audio.Media.TITLE} ASC"
                        )
                        cursor?.use { c ->
                            val idCol = c.getColumnIndex(MediaStore.Audio.Media._ID)
                            val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                            val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                            val albumCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                            val albumIdCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                            val durationCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                            val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                            val nameCol = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)

                            while (c.moveToNext()) {
                                val id = if (idCol >= 0) c.getLong(idCol) else -1L
                                val albumId = if (albumIdCol >= 0) c.getLong(albumIdCol) else -1L
                                val dataPath = if (dataCol >= 0) c.getString(dataCol) else null
                                val displayName = if (nameCol >= 0) c.getString(nameCol) else null

                                val ext = (dataPath?.substringAfterLast('.', "")
                                    ?: displayName?.substringAfterLast('.', "") ?: "").lowercase()
                                if (ext.isBlank() || ext in audioExtensions) {
                                    val matchesPath = if (paths.isNotEmpty() && dataPath != null) {
                                        paths.any { p -> dataPath.startsWith(p, ignoreCase = true) }
                                    } else true

                                    if (matchesPath) {
                                        val title =
                                            if (titleCol >= 0) c.getString(titleCol) else null
                                        val artist =
                                            if (artistCol >= 0) c.getString(artistCol) else null
                                        val album =
                                            if (albumCol >= 0) c.getString(albumCol) else null
                                        val durationMs =
                                            if (durationCol >= 0) c.getLong(durationCol) else 0L

                                        val safeTitle = title?.takeIf { it.isNotBlank() }
                                            ?: displayName?.substringBeforeLast('.')
                                            ?: (if (dataPath != null) File(dataPath).nameWithoutExtension else "Pista local")
                                        val safeArtist =
                                            artist?.takeIf { it.isNotBlank() && it != "<unknown>" }
                                                ?: "Artista Desconocido"
                                        val safeAlbum =
                                            album?.takeIf { it.isNotBlank() && it != "<unknown>" }
                                                ?: ""
                                        val artworkUrl = if (albumId >= 0) {
                                            ContentUris.withAppendedId(
                                                Uri.parse("content://media/external/audio/albumart"),
                                                albumId
                                            ).toString()
                                        } else null

                                        val trackId =
                                            if (dataPath != null && dataPath.isNotBlank()) {
                                                "local:$dataPath"
                                            } else if (id >= 0) {
                                                "local:content://media/external/audio/media/$id"
                                            } else null

                                        if (trackId != null) {
                                            results.add(
                                                Track(
                                                    id = trackId,
                                                    title = safeTitle,
                                                    artist = safeArtist,
                                                    album = safeAlbum,
                                                    durationMs = durationMs,
                                                    genres = emptyList(),
                                                    artworkUrl = artworkUrl,
                                                    sourceId = null
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val effectivePaths =
                if (paths.isNotEmpty()) paths else PlatformFileSystem.getDefaultMusicPaths()
            effectivePaths.forEach { path ->
                val dir = File(path)
                if (dir.exists() && dir.isDirectory) {
                    try {
                        dir.walkTopDown()
                            .maxDepth(12)
                            .filter { it.isFile && it.extension.lowercase() in audioExtensions }
                            .forEach { file ->
                                val localId = "local:${file.absolutePath}"
                                if (results.none { it.id == localId }) {
                                    val metadata = getFileMetadata(file) ?: return@forEach
                                    results.add(
                                        Track(
                                            id = localId,
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
        trackId: String,
        title: String?,
        artist: String?,
        album: String?
    ) {
        val path = if (trackId.startsWith("local:")) {
            trackId.removePrefix("local:")
        } else if (trackId.startsWith("file:")) {
            trackId.removePrefix("file:")
        } else {
            getOfflineTrack(trackId)?.localFilePath
        }

        withContext(dispatcher) {
            if (path != null && !path.startsWith("content://")) {
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