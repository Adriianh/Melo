package com.github.adriianh.cli.tui.util

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TuiLogGuardTest {

    @Test
    fun testWithLogGuardRedirectsAndRestoresSystemErr() {
        val originalErr = System.err
        var capturedInsideErr: PrintStream? = null

        val result = TuiLogGuard.withLogGuard {
            capturedInsideErr = System.err
            assertNotSame(originalErr, System.err, "System.err should be redirected inside withLogGuard")
            System.err.println("Test error message inside guard")
            "success"
        }

        assertEquals("success", result)
        assertSame(originalErr, System.err, "System.err must be restored after withLogGuard exits")
    }

    @Test
    fun testWithLogGuardSilencesJaudiotagger() {
        val jtLogger = Logger.getLogger("org.jaudiotagger")
        val initialLevel = jtLogger.level

        TuiLogGuard.withLogGuard {
            assertEquals(Level.OFF, jtLogger.level, "org.jaudiotagger should be Level.OFF inside guard")
            assertEquals(false, jtLogger.useParentHandlers, "useParentHandlers should be false inside guard")
        }

        assertEquals(initialLevel, jtLogger.level, "org.jaudiotagger level should be restored after guard")
    }

    @Test
    fun testWithLogGuardDetachesAndRestoresConsoleHandlers() {
        val rootLogger = Logger.getLogger("")
        val testHandler = ConsoleHandler()
        rootLogger.addHandler(testHandler)

        try {
            assertTrue(rootLogger.handlers.contains(testHandler), "Root logger must have testHandler initially")

            TuiLogGuard.withLogGuard {
                val hasConsoleHandler = rootLogger.handlers.any { it is ConsoleHandler }
                assertEquals(false, hasConsoleHandler, "ConsoleHandler should be removed inside guard")
            }

            assertTrue(rootLogger.handlers.contains(testHandler), "ConsoleHandler should be restored after guard")
        } finally {
            rootLogger.removeHandler(testHandler)
        }
    }
}
