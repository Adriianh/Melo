package com.github.adriianh.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

@Serializable
data class OfflineTrack(
    val track: Track,
    val localFilePath: String? = null,
    val downloadStatus: DownloadStatus = DownloadStatus.PENDING,
    val downloadedAt: Long? = null,
    val lastAccessedAt: Long? = null,
    val fileSize: Long = 0L
)