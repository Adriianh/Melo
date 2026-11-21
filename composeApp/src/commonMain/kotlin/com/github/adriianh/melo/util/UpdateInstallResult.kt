package com.github.adriianh.melo.util

/**
 * Machine-readable cause of an update installation failure.
 */
enum class InstallErrorReason {
    NO_ANDROID_CONTEXT,
    PACKAGE_NOT_FOUND,
    ANDROID_INSTALLER_OPEN_FAILED,
    APPIMAGE_NOT_FOUND,
    NO_WRITE_PERMISSION,
    PORTABLE_INSTALL_NOT_FOUND,
    PORTABLE_NO_PERMISSION,
    HELPER_PREPARE_FAILED,
    START_SYSTEM_FAILED,
    START_HELPER_FAILED,
    UNSUPPORTED_PLATFORM,
}

/**
 * Outcome of handing a downloaded package over for installation.
 */
sealed interface UpdateInstallResult {
    /** The replacement was started; on desktop the app steps aside so it can take over. */
    data object Started : UpdateInstallResult

    /** The replacement could not be started; [message] or [reason] is shown to the user. */
    data class Failed(
        val message: String = "",
        val reason: InstallErrorReason? = null,
        val detail: String? = null,
    ) : UpdateInstallResult
}
