package com.github.adriianh.core.domain.model.update

/**
 * Resolves the state shown after a successful update check.
 *
 * Returns [UpdateState.UpdateAvailable] with the asset that matches [method] when the
 * current install can be replaced in place. Package-managed installs (and releases
 * without a matching asset) become [UpdateState.ManualUpdate], so the UI can point
 * the user at the release page instead of silently doing nothing.
 */
fun resolveUpdateState(
    release: AppRelease,
    platform: UpdatePlatform,
    method: UpdateInstallMethod,
    currentVersion: String,
): UpdateState {
    if (method == UpdateInstallMethod.PACKAGE_MANAGER) {
        return UpdateState.ManualUpdate(release = release, method = method)
    }

    val asset = release.getAssetForPlatform(platform, method)
    return if (asset == null) {
        UpdateState.ManualUpdate(release = release, method = method)
    } else {
        UpdateState.UpdateAvailable(
            currentVersion = currentVersion,
            release = release,
            targetAsset = asset,
        )
    }
}
