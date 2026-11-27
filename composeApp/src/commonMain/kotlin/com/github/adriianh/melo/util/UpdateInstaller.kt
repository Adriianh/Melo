package com.github.adriianh.melo.util

expect class PlatformUpdateInstaller() {
    /**
     * Whether applying [installerPath] on this installation will automatically close
     * and relaunch Melo once the update completes.
     */
    fun supportsAutoRestart(installerPath: String): Boolean

    /**
     * Applies [installerPath] using the method that matches how Melo was installed.
     *
     * On success the desktop app exits so the replacement can take over; the returned
     * result is therefore only meaningful on failure.
     */
    fun installAndRestart(installerPath: String): UpdateInstallResult
}
