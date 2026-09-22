package com.github.adriianh.cli

import com.github.adriianh.cli.command.MeloCommand
import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.tui.player.FfplayProcessManager
import com.github.ajalt.clikt.core.main
import java.awt.color.ColorSpace
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.time.Instant
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            val logDir = File(configDir)
            if (!logDir.exists()) logDir.mkdirs()
            val crashFile = File(logDir, "crash.log")
            FileOutputStream(crashFile, true).use { fos ->
                PrintStream(fos, true).use { ps ->
                    ps.println("=== UNCAUGHT EXCEPTION on thread '${thread.name}' ===")
                    ps.println("Timestamp: ${Instant.now()}")
                    throwable.printStackTrace(ps)
                    ps.println()
                }
            }
        } catch (_: Throwable) {
        }
        try {
            throwable.printStackTrace()
        } catch (_: Throwable) {
        }
    }

    try {
        System.setProperty("java.awt.headless", "true")
    } catch (_: Throwable) {
    }

    try {
        ColorSpace.getInstance(ColorSpace.CS_sRGB)
    } catch (_: Throwable) {
    }

    try {
        MeloCommand().main(args)
    } finally {
        FfplayProcessManager.killAll()
    }
    exitProcess(0)
}