package com.github.adriianh.core.domain.usecase.update

import com.github.adriianh.core.domain.model.update.ReleaseAsset
import com.github.adriianh.core.domain.repository.DownloadProgress
import com.github.adriianh.core.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.Flow

class DownloadUpdateUseCase(
    private val repository: UpdateRepository
) {
    operator fun invoke(
        asset: ReleaseAsset,
        destinationFilePath: String
    ): Flow<DownloadProgress> {
        return repository.downloadAsset(asset, destinationFilePath)
    }
}