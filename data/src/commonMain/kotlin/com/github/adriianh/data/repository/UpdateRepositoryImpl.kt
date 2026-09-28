package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.ReleaseAsset
import com.github.adriianh.core.domain.model.update.UpdatePlatform
import com.github.adriianh.core.domain.repository.DownloadProgress
import com.github.adriianh.core.domain.repository.UpdateRepository
import com.github.adriianh.core.platform.PlatformFileSystem
import com.github.adriianh.data.remote.dto.GitHubReleaseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class UpdateRepositoryImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val repoOwnerAndName: String = "Adriianh/Melo",
) : UpdateRepository {
    override suspend fun checkForUpdate(currentVersion: String): Result<AppRelease?> =
        withContext(dispatcher) {
            runCatching {
                val url = "https://api.github.com/repos/$repoOwnerAndName/releases/latest"
                val dto: GitHubReleaseDto =
                    httpClient
                        .get(url) {
                            header(HttpHeaders.Accept, "application/vnd.github.v3+json")
                            header(HttpHeaders.UserAgent, "Melo-Music-Player")
                        }.body()

                val remoteVersion =
                    dto.tagName
                        .trim()
                        .removePrefix("v")
                        .removePrefix("V")
                val isNewer = isNewerVersion(remoteVersion, currentVersion)

                if (!isNewer) {
                    return@runCatching null
                }

                val assets =
                    dto.assets.map { assetDto ->
                        ReleaseAsset(
                            name = assetDto.name,
                            downloadUrl = assetDto.browserDownloadUrl,
                            sizeBytes = assetDto.size,
                            platform = detectPlatformFromFileName(assetDto.name),
                        )
                    }

                AppRelease(
                    version = remoteVersion,
                    tagName = dto.tagName,
                    name = dto.name ?: dto.tagName,
                    releaseNotes = dto.body.orEmpty(),
                    htmlUrl = dto.htmlUrl,
                    publishedAt = dto.publishedAt.orEmpty(),
                    assets = assets,
                )
            }
        }

    override fun downloadAsset(
        asset: ReleaseAsset,
        destinationFilePath: String,
    ): Flow<DownloadProgress> =
        channelFlow {
            send(DownloadProgress.Progress(0L, asset.sizeBytes))

            val tempFilePath = "$destinationFilePath.tmp"
            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
            var totalBytesWritten = 0L

            try {
                httpClient
                    .prepareGet(asset.downloadUrl) {
                        timeout {
                            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                            socketTimeoutMillis = SOCKET_TIMEOUT_MS
                            connectTimeoutMillis = CONNECT_TIMEOUT_MS
                        }
                    }.execute { response ->
                        if (response.status.value !in 200..299) {
                            throw IllegalStateException(
                                "Download failed with HTTP status " +
                                    "${response.status.value}: ${response.status.description}",
                            )
                        }

                        val contentLength = response.contentLength()
                        val totalExpected =
                            if (contentLength != null && contentLength > 0L) {
                                contentLength
                            } else {
                                asset.sizeBytes
                            }

                        val channel: ByteReadChannel = response.bodyAsChannel()

                        val written =
                            PlatformFileSystem.writeStream(tempFilePath) { writeChunk ->
                                while (!channel.isClosedForRead) {
                                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                                    if (bytesRead <= 0) break
                                    writeChunk(buffer, 0, bytesRead)
                                    totalBytesWritten += bytesRead
                                    send(DownloadProgress.Progress(totalBytesWritten, totalExpected))
                                }
                            }

                        if (written <= 0L && totalBytesWritten == 0L) {
                            throw IllegalStateException("Downloaded file is empty")
                        }
                    }

                if (totalBytesWritten == 0L) {
                    throw IllegalStateException("Downloaded file is empty")
                }

                PlatformFileSystem.deleteFile(destinationFilePath)
                if (!PlatformFileSystem.copyFile(tempFilePath, destinationFilePath)) {
                    throw IllegalStateException(
                        "Failed to move downloaded file to destination: $destinationFilePath",
                    )
                }

                send(DownloadProgress.Completed(destinationFilePath))
            } finally {
                PlatformFileSystem.deleteFile(tempFilePath)
            }
        }.flowOn(dispatcher)

    companion object {
        private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
        private const val SOCKET_TIMEOUT_MS = 60_000L
        private const val CONNECT_TIMEOUT_MS = 30_000L

        fun detectPlatformFromFileName(fileName: String): UpdatePlatform {
            val lower = fileName.lowercase()
            return when {
                isWindowsAsset(lower) -> UpdatePlatform.WINDOWS
                isLinuxAsset(lower) -> UpdatePlatform.LINUX
                isMacAsset(lower) -> UpdatePlatform.MACOS
                lower.endsWith(".apk") -> UpdatePlatform.ANDROID
                else -> UpdatePlatform.UNKNOWN
            }
        }

        private fun isWindowsAsset(name: String): Boolean =
            name.endsWith(".exe") ||
                name.endsWith(".msi") ||
                (name.contains("windows") && name.endsWith(".zip"))

        private fun isLinuxAsset(name: String): Boolean =
            name.endsWith(".appimage") ||
                name.endsWith(".deb") ||
                name.endsWith(".rpm") ||
                (name.contains("linux") && name.endsWith(".tar.gz"))

        private fun isMacAsset(name: String): Boolean =
            name.endsWith(".dmg") ||
                name.endsWith(".pkg") ||
                (name.contains("mac") && (name.endsWith(".zip") || name.endsWith(".tar.gz")))

        fun isNewerVersion(
            remote: String,
            current: String,
        ): Boolean {
            val remoteParts = remote.substringBefore('-').split('.').mapNotNull { it.toIntOrNull() }
            val currentParts =
                current.substringBefore('-').split('.').mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }
    }
}
