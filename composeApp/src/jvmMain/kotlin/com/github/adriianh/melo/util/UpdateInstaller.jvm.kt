package com.github.adriianh.melo.util

import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

actual class PlatformUpdateInstaller actual constructor() {
    actual fun installAndRestart(installerPath: String) {
        val file = File(installerPath)
        if (!file.exists()) return

        val os = System.getProperty("os.name", "").lowercase()
        val lowerName = file.name.lowercase()

        try {
            when {
                os.contains("win") -> {
                    when {
                        lowerName.endsWith(".msi") -> {
                            ProcessBuilder("msiexec.exe", "/i", file.absolutePath, "/passive").start()
                            exitProcess(0)
                        }

                        lowerName.endsWith(".exe") -> {
                            ProcessBuilder(
                                file.absolutePath,
                                "/SILENT",
                                "/CLOSEAPPLICATIONS",
                                "/RESTARTAPPLICATIONS",
                            ).start()
                            exitProcess(0)
                        }

                        else -> {
                            ProcessBuilder("explorer.exe", file.absolutePath).start()
                        }
                    }
                }

                os.contains("mac") -> {
                    ProcessBuilder("open", file.absolutePath).start()
                }

                else -> {
                    when {
                        lowerName.endsWith(".appimage") -> {
                            file.setExecutable(true, false)
                            ProcessBuilder(file.absolutePath).start()
                            exitProcess(0)
                        }

                        else -> {
                            ProcessBuilder("xdg-open", file.absolutePath).start()
                        }
                    }
                }
            }
        } catch (_: IOException) {
            // Process launch failed
        }
    }
}
