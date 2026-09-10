package com.github.adriianh.melo.util

expect class PlatformUpdateInstaller() {
    fun installAndRestart(installerPath: String)
}