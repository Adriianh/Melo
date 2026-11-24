package com.github.adriianh.core.domain.usecase.update

import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.core.domain.repository.UpdateRepository
import com.github.adriianh.core.util.MeloVersion

class GetReleaseForVersionUseCase(
    private val repository: UpdateRepository,
) {
    suspend operator fun invoke(
        version: String = MeloVersion.CURRENT,
        channel: UpdateChannel =
            if (version.contains("nightly", ignoreCase = true)) {
                UpdateChannel.NIGHTLY
            } else {
                UpdateChannel.STABLE
            },
    ): Result<AppRelease?> = repository.getReleaseForVersion(version, channel)
}
