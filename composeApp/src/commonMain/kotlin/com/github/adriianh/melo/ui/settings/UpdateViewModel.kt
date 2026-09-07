package com.github.adriianh.melo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.ReleaseAsset
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.core.domain.repository.DownloadProgress
import com.github.adriianh.core.domain.usecase.update.CheckForUpdateUseCase
import com.github.adriianh.core.domain.usecase.update.DownloadUpdateUseCase
import com.github.adriianh.core.platform.PlatformFileSystem
import com.github.adriianh.core.platform.currentUpdatePlatform
import com.github.adriianh.core.util.MeloVersion
import com.github.adriianh.melo.util.PlatformUpdateInstaller
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val downloadUpdateUseCase: DownloadUpdateUseCase,
    private val installer: PlatformUpdateInstaller
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val uiState: StateFlow<UpdateState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    fun checkForUpdates() {
        if (_uiState.value is UpdateState.Checking || _uiState.value is UpdateState.Downloading) return

        viewModelScope.launch {
            _uiState.value = UpdateState.Checking
            val result = checkForUpdateUseCase(MeloVersion.CURRENT)
            result.onSuccess { release ->
                if (release != null) {
                    val platform = currentUpdatePlatform()
                    val targetAsset = release.getAssetForPlatform(platform)
                        ?: release.assets.firstOrNull()
                    _uiState.value = UpdateState.UpdateAvailable(
                        currentVersion = MeloVersion.CURRENT,
                        release = release,
                        targetAsset = targetAsset
                    )
                } else {
                    _uiState.value = UpdateState.UpToDate(MeloVersion.CURRENT)
                }
            }.onFailure { error ->
                _uiState.value = UpdateState.Error(
                    message = error.message ?: "No se pudo verificar actualizaciones"
                )
            }
        }
    }

    fun startDownload(release: AppRelease, asset: ReleaseAsset) {
        if (_uiState.value is UpdateState.Downloading) return

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.value = UpdateState.Downloading(
                release = release,
                targetAsset = asset,
                progressFraction = 0f,
                bytesDownloaded = 0L,
                totalBytes = asset.sizeBytes
            )

            val tempDir = PlatformFileSystem.getTempDirectory()
            val sanitizedName = asset.name.ifBlank { "melo-update-${release.version}" }
            val destinationPath = "$tempDir/$sanitizedName"

            try {
                downloadUpdateUseCase(asset, destinationPath).collect { progress ->
                    when (progress) {
                        is DownloadProgress.Progress -> {
                            val fraction = if (progress.totalBytes > 0) {
                                (progress.bytesDownloaded.toFloat() / progress.totalBytes.toFloat()).coerceIn(
                                    0f,
                                    1f
                                )
                            } else 0f

                            _uiState.value = UpdateState.Downloading(
                                release = release,
                                targetAsset = asset,
                                progressFraction = fraction,
                                bytesDownloaded = progress.bytesDownloaded,
                                totalBytes = progress.totalBytes
                            )
                        }

                        is DownloadProgress.Completed -> {
                            _uiState.value = UpdateState.ReadyToInstall(
                                release = release,
                                installerPath = progress.filePath
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UpdateState.Error(
                    message = "Error en la descarga: ${e.message ?: "Error desconocido"}"
                )
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _uiState.value = UpdateState.Idle
    }

    fun installUpdate(installerPath: String) {
        installer.installAndRestart(installerPath)
    }

    fun resetState() {
        _uiState.value = UpdateState.Idle
    }
}