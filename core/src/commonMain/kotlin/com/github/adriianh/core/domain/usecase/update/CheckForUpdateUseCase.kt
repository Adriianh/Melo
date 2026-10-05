package com.github.adriianh.core.domain.usecase.update

import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.core.domain.repository.UpdateRepository
import com.github.adriianh.core.util.MeloVersion

class CheckForUpdateUseCase(
    private val repository: UpdateRepository,
) {
    suspend operator fun invoke(
        currentVersion: String = MeloVersion.CURRENT,
        channel: UpdateChannel = UpdateChannel.STABLE,
    ): Result<AppRelease?> = repository.checkForUpdate(currentVersion, channel)
}
