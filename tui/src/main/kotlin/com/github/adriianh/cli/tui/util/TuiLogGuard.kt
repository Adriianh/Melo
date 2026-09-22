package com.github.adriianh.cli.tui.util

import com.github.adriianh.cli.config.configDir
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.util.logging.ConsoleHandler
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Guards the terminal UI against rogue stderr writes and library logs that would corrupt
 * the alternate screen layout across all platforms.
 */
object TuiLogGuard {

    private const val MAX_LOG_SIZE_BYTES = 5 * 1024 * 1024L // 5 MB

    /**
     * Executes [block] while redirecting [System.err] to `melo.log` (or null stream)
     * and detaching console handlers from [java.util.logging], preventing third-party
     * or library logs from escaping to the terminal screen.
     */
    fun <R> withLogGuard(block: () -> R): R {
        val originalErr = System.err
        val rootLogger = Logger.getLogger("")
        val isDebug = System.getenv("MELO_DEBUG") == "1" || System.getProperty("melo.debug") == "true"

        val originalHandlers: Array<Handler> = try {
            rootLogger.handlers
        } catch (_: Throwable) {
            emptyArray()
        }

        // 1. Silence JAudioTagger logger hierarchy
        val jtLogger = Logger.getLogger("org.jaudiotagger")
        val originalJtLevel = jtLogger.level
        val originalJtUseParent = jtLogger.useParentHandlers
        try {
            jtLogger.level = Level.OFF
            jtLogger.useParentHandlers = false
            for (h in jtLogger.handlers) {
                jtLogger.removeHandler(h)
            }
        } catch (_: Throwable) {}

        // 2. Remove ConsoleHandlers from root logger unless in debug mode
        if (!isDebug) {
            try {
                for (handler in originalHandlers) {
                    if (handler is ConsoleHandler) {
                        rootLogger.removeHandler(handler)
                    }
                }
            } catch (_: Throwable) {}
        }

        // 3. Redirect System.err to melo.log
        var logFileOutputStream: FileOutputStream? = null
        val logStream: PrintStream = try {
            val logDir = File(configDir)
            if (!logDir.exists()) logDir.mkdirs()
            val logFile = File(logDir, "melo.log")
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE_BYTES) {
                val backup = File(logDir, "melo.log.1")
                if (backup.exists()) backup.delete()
                logFile.renameTo(backup)
            }
            val fos = FileOutputStream(logFile, true)
            logFileOutputStream = fos
            PrintStream(fos, true)
        } catch (_: Throwable) {
            PrintStream(OutputStream.nullOutputStream())
        }

        System.setErr(logStream)

        return try {
            block()
        } finally {
            System.setErr(originalErr)
            try {
                logStream.flush()
                logFileOutputStream?.close()
            } catch (_: Throwable) {}

            try {
                jtLogger.level = originalJtLevel
                jtLogger.useParentHandlers = originalJtUseParent
            } catch (_: Throwable) {}

            if (!isDebug) {
                try {
                    val currentHandlers = rootLogger.handlers.toSet()
                    for (handler in originalHandlers) {
                        if (handler is ConsoleHandler && handler !in currentHandlers) {
                            rootLogger.addHandler(handler)
                        }
                    }
                } catch (_: Throwable) {}
            }
        }
    }
}
