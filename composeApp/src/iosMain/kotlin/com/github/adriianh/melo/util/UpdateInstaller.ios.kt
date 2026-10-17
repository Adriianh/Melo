package com.github.adriianh.melo.util

actual class PlatformUpdateInstaller actual constructor() {
    actual fun installAndRestart(installerPath: String): UpdateInstallResult =
        UpdateInstallResult.Failed("La actualización integrada no está disponible en esta plataforma")
}
