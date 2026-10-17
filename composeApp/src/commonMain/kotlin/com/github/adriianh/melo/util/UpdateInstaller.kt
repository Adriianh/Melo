package com.github.adriianh.melo.util

expect class PlatformUpdateInstaller() {
    /**
     * Applies [installerPath] using the method that matches how Melo was installed.
     *
     * On success the desktop app exits so the replacement can take over; the returned
     * result is therefore only meaningful on failure.
     */
    fun installAndRestart(installerPath: String): UpdateInstallResult
}
