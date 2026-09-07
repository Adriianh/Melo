package com.github.adriianh.melo.util

import java.io.File
import kotlin.system.exitProcess

actual class PlatformUpdateInstaller actual constructor() {
    actual fun installAndRestart(installerPath: String) {
        val file = File(installerPath)
        if (!file.exists()) return

        val os = System.getProperty("os.name", "").lowercase()
        when {
            os.contains("win") -> {
                val pb = ProcessBuilder(
                    file.absolutePath,
                    "/SILENT",
                    "/CLOSEAPPLICATIONS",
                    "/RESTARTAPPLICATIONS"
                )
                pb.start()
                exitProcess(0)
            }

            os.contains("mac") -> {
                ProcessBuilder("open", file.absolutePath).start()
            }

            else -> {
                ProcessBuilder("xdg-open", file.absolutePath).start()
            }
        }
    }
}