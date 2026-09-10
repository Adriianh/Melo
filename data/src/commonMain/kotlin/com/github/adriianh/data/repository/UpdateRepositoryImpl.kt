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
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class UpdateRepositoryImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val repoOwnerAndName: String = "Adriianh/Melo"
) : UpdateRepository {

    override suspend fun checkForUpdate(currentVersion: String): Result<AppRelease?> =
        withContext(dispatcher) {
            runCatching {
                val url = "https://api.github.com/repos/$repoOwnerAndName/releases/latest"
                val dto: GitHubReleaseDto = httpClient.get(url) {
                    header(HttpHeaders.Accept, "application/vnd.github.v3+json")
                    header(HttpHeaders.UserAgent, "Melo-Music-Player")
                }.body()

                val remoteVersion = dto.tagName.trim().removePrefix("v").removePrefix("V")
                val isNewer = isNewerVersion(remoteVersion, currentVersion)

                if (!isNewer) {
                    return@runCatching null
                }

                val assets = dto.assets.map { assetDto ->
                    ReleaseAsset(
                        name = assetDto.name,
                        downloadUrl = assetDto.browserDownloadUrl,
                        sizeBytes = assetDto.size,
                        platform = detectPlatformFromFileName(assetDto.name)
                    )
                }

                AppRelease(
                    version = remoteVersion,
                    tagName = dto.tagName,
                    name = dto.name ?: dto.tagName,
                    releaseNotes = dto.body.orEmpty(),
                    htmlUrl = dto.htmlUrl,
                    publishedAt = dto.publishedAt.orEmpty(),
                    assets = assets
                )
            }
        }

    override fun downloadAsset(
        asset: ReleaseAsset,
        destinationFilePath: String
    ): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Progress(0L, asset.sizeBytes))

        val tempFilePath = "$destinationFilePath.tmp"

        val response = httpClient.get(asset.downloadUrl) {
            onDownload { bytesSentTotal, contentLength ->
                val total =
                    if (contentLength != null && contentLength > 0L) contentLength else asset.sizeBytes
                emit(DownloadProgress.Progress(bytesSentTotal, total))
            }
        }

        val bytes = response.bodyAsBytes()
        if (bytes.isEmpty()) {
            throw IllegalStateException("Downloaded file is empty")
        }

        PlatformFileSystem.writeBytes(tempFilePath, bytes)
        PlatformFileSystem.copyFile(tempFilePath, destinationFilePath)
        PlatformFileSystem.deleteFile(tempFilePath)

        emit(DownloadProgress.Completed(destinationFilePath))
    }.flowOn(dispatcher)

    private fun detectPlatformFromFileName(fileName: String): UpdatePlatform {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".exe") || lower.endsWith(".msi") -> UpdatePlatform.WINDOWS
            lower.endsWith(".deb") || (lower.contains("linux") && (lower.endsWith(".tar.gz") || lower.endsWith(
                ".AppImage"
            ))) -> UpdatePlatform.LINUX

            lower.endsWith(".dmg") || (lower.contains("mac") && lower.endsWith(".zip")) -> UpdatePlatform.MACOS
            lower.endsWith(".apk") -> UpdatePlatform.ANDROID
            else -> UpdatePlatform.UNKNOWN
        }
    }

    companion object {
        fun isNewerVersion(remote: String, current: String): Boolean {
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