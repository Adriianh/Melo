package com.github.adriianh.core.domain.model.update

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val currentVersion: String) : UpdateState
    data class UpdateAvailable(
        val currentVersion: String,
        val release: AppRelease,
        val targetAsset: ReleaseAsset?
    ) : UpdateState

    data class Downloading(
        val release: AppRelease,
        val targetAsset: ReleaseAsset,
        val progressFraction: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateState

    data class ReadyToInstall(
        val release: AppRelease,
        val installerPath: String
    ) : UpdateState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : UpdateState
}