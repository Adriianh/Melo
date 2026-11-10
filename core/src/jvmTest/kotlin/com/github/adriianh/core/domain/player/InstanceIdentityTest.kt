package com.github.adriianh.core.domain.player

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstanceIdentityTest {
    private val lockDir: File = Files.createTempDirectory("melo-instance-identity").toFile()

    @AfterTest
    fun cleanup() {
        lockDir.deleteRecursively()
    }

    @Test
    fun `first instance owns the canonical key and mpris name`() {
        val identity = InstanceIdentity.resolve("gui", "Melo", lockDir, isLinux = true, pid = 1_000L)
        try {
            assertTrue(identity.isPrimary)
            assertEquals("gui", identity.role)
            assertEquals("gui", identity.instanceKey)
            assertEquals("Melo", identity.mprisName)
        } finally {
            identity.release()
        }
    }

    @Test
    fun `second instance of the same role falls back to the pid suffix`() {
        val first = InstanceIdentity.resolve("gui", "Melo", lockDir, isLinux = true, pid = 1_000L)
        val second = InstanceIdentity.resolve("gui", "Melo", lockDir, isLinux = true, pid = 2_000L)
        try {
            assertTrue(first.isPrimary)
            assertFalse(second.isPrimary)
            assertEquals("gui", second.role)
            assertEquals("gui.instance2000", second.instanceKey)
            assertEquals("Melo.instance2000", second.mprisName)
        } finally {
            second.release()
            first.release()
        }
    }

    @Test
    fun `releasing the lock lets the next instance become primary`() {
        InstanceIdentity.resolve("gui", "Melo", lockDir, isLinux = true, pid = 1_000L).release()

        val next = InstanceIdentity.resolve("gui", "Melo", lockDir, isLinux = true, pid = 2_000L)
        try {
            assertTrue(next.isPrimary)
            assertEquals("gui", next.instanceKey)
            assertEquals("Melo", next.mprisName)
        } finally {
            next.release()
        }
    }

    @Test
    fun `different roles do not contend for the same lock`() {
        val gui = InstanceIdentity.resolve("gui", "Melo", lockDir, isLinux = true, pid = 1_000L)
        val tui = InstanceIdentity.resolve("tui", "Melo.tui", lockDir, isLinux = true, pid = 2_000L)
        try {
            assertTrue(gui.isPrimary)
            assertTrue(tui.isPrimary)
            assertEquals("gui", gui.instanceKey)
            assertEquals("tui", tui.instanceKey)
        } finally {
            tui.release()
            gui.release()
        }
    }

    @Test
    fun `non-linux platforms resolve as primary without a lock`() {
        val identity = InstanceIdentity.resolve("gui", "Melo", lockDir, isLinux = false, pid = 1_000L)
        try {
            assertTrue(identity.isPrimary)
            assertEquals("gui", identity.instanceKey)
            assertEquals("Melo", identity.mprisName)
        } finally {
            identity.release()
        }
    }
}
