package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.ReleaseAsset
import kotlinx.coroutines.flow.Flow

sealed interface DownloadProgress {
    data class Progress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadProgress
    data class Completed(val filePath: String) : DownloadProgress
}

interface UpdateRepository {
    suspend fun checkForUpdate(currentVersion: String): Result<AppRelease?>
    fun downloadAsset(
        asset: ReleaseAsset,
        destinationFilePath: String
    ): Flow<DownloadProgress>
}