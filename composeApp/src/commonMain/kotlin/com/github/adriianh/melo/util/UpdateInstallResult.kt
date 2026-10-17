package com.github.adriianh.melo.util

/**
 * Outcome of handing a downloaded package over for installation.
 */
sealed interface UpdateInstallResult {
    /** The replacement was started; on desktop the app steps aside so it can take over. */
    data object Started : UpdateInstallResult

    /** The replacement could not be started; [message] is shown to the user. */
    data class Failed(
        val message: String,
    ) : UpdateInstallResult
}
