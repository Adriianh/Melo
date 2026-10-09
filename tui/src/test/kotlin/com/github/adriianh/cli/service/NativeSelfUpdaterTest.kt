package com.github.adriianh.cli.service

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NativeSelfUpdaterTest {
    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("melo-self-updater-test-").toFile()
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `extractZip extracts files safely`() {
        val zipFile = File(tempDir, "archive.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("melo-2.2.1/melo"))
            zos.write("binary-content".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("melo-2.2.1/uninstall.sh"))
            zos.write("echo uninstall".toByteArray())
            zos.closeEntry()
        }

        val extractTarget = File(tempDir, "extracted")
        val updater = NativeSelfUpdater(isWindows = false)
        updater.extractArchive(zipFile, extractTarget)

        val binary = updater.findBinaryInExtracted(extractTarget)
        assertNotNull(binary)
        assertEquals("melo", binary.name)
        assertEquals("binary-content", binary.readText())

        val uninstall = File(extractTarget, "melo-2.2.1/uninstall.sh")
        assertTrue(uninstall.exists())
        assertEquals("echo uninstall", uninstall.readText())
    }

    @Test
    fun `extractZip prevents zip slip vulnerability`() {
        val maliciousZip = File(tempDir, "slip.zip")
        ZipOutputStream(FileOutputStream(maliciousZip)).use { zos ->
            zos.putNextEntry(ZipEntry("../../escaped.txt"))
            zos.write("malicious".toByteArray())
            zos.closeEntry()
        }

        val extractTarget = File(tempDir, "extracted-slip")
        val updater = NativeSelfUpdater(isWindows = false)

        assertFailsWith<SecurityException> {
            updater.extractArchive(maliciousZip, extractTarget)
        }
    }

    @Test
    fun `findBinaryInExtracted finds melo on Unix and melo exe on Windows`() {
        val unixRoot = File(tempDir, "unix")
        unixRoot.mkdirs()
        val unixBinary = File(unixRoot, "melo")
        unixBinary.writeText("unix-bin")

        val unixUpdater = NativeSelfUpdater(isWindows = false)
        val foundUnix = unixUpdater.findBinaryInExtracted(unixRoot)
        assertNotNull(foundUnix)
        assertEquals("melo", foundUnix.name)

        val winRoot = File(tempDir, "windows")
        winRoot.mkdirs()
        val winBinary = File(winRoot, "melo.exe")
        winBinary.writeText("win-bin")

        val winUpdater = NativeSelfUpdater(isWindows = true)
        val foundWin = winUpdater.findBinaryInExtracted(winRoot)
        assertNotNull(foundWin)
        assertEquals("melo.exe", foundWin.name)
    }

    @Test
    fun `findBinaryInExtracted prefers root native binary over bin wrapper script on Unix`() {
        val root = File(tempDir, "archive-root")
        val pkg = File(root, "melo-2.3.0").apply { mkdirs() }
        val binDir = File(pkg, "bin").apply { mkdirs() }

        val wrapperScript = File(binDir, "melo")
        wrapperScript.writeText("#!/usr/bin/env sh\nexec melo\n")

        val realBinary = File(pkg, "melo")
        val elfHeader = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte(), 2, 1, 1, 0)
        realBinary.writeBytes(elfHeader + ByteArray(1024))

        val updater = NativeSelfUpdater(isWindows = false)
        val found = updater.findBinaryInExtracted(root)
        assertNotNull(found)
        assertEquals(realBinary.canonicalPath, found.canonicalPath)
    }

    @Test
    fun `binary and script detection helpers identify formats accurately`() {
        val updater = NativeSelfUpdater(isWindows = false)

        val scriptFile = File(tempDir, "script.sh")
        scriptFile.writeText("#!/bin/sh\necho hi")
        assertTrue(updater.isShebangScript(scriptFile))
        assertFalse(updater.isNativeBinary(scriptFile))

        val elfFile = File(tempDir, "sample.elf")
        elfFile.writeBytes(byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte()))
        assertFalse(updater.isShebangScript(elfFile))
        assertTrue(updater.isNativeBinary(elfFile))

        val peFile = File(tempDir, "sample.exe")
        peFile.writeBytes(byteArrayOf('M'.code.toByte(), 'Z'.code.toByte(), 0, 0))
        assertFalse(updater.isShebangScript(peFile))
        assertTrue(updater.isNativeBinary(peFile))

        val textFile = File(tempDir, "plain.txt")
        textFile.writeText("plain text")
        assertFalse(updater.isShebangScript(textFile))
        assertFalse(updater.isNativeBinary(textFile))
    }

    @Test
    fun `applyUpdate replaces binary and cleans old files on Unix`() {
        val installDir = File(tempDir, "install-unix")
        installDir.mkdirs()
        val oldBinary = File(installDir, "melo")
        oldBinary.writeText("old-binary-v1")

        val extracted = File(tempDir, "extracted-unix")
        extracted.mkdirs()
        val newBinary = File(extracted, "melo")
        newBinary.writeText("new-binary-v2")

        val companionScript = File(extracted, "uninstall.sh")
        companionScript.writeText("echo uninstall v2")

        val updater = NativeSelfUpdater(isWindows = false)
        updater.applyUpdate(extracted, oldBinary)

        assertEquals("new-binary-v2", oldBinary.readText())
        assertTrue(oldBinary.canExecute())

        val targetUninstall = File(installDir, "uninstall.sh")
        assertTrue(targetUninstall.exists())
        assertEquals("echo uninstall v2", targetUninstall.readText())
    }

    @Test
    fun `applyUpdate on Windows renames old executable and copies companions`() {
        val installDir = File(tempDir, "install-win")
        installDir.mkdirs()
        val oldBinary = File(installDir, "melo.exe")
        oldBinary.writeText("old-binary.exe")

        val oldDll = File(installDir, "SMTCAdapter.dll")
        oldDll.writeText("old-dll")

        val extracted = File(tempDir, "extracted-win")
        extracted.mkdirs()
        val newBinary = File(extracted, "melo.exe")
        newBinary.writeText("new-binary.exe")

        val newDll = File(extracted, "SMTCAdapter.dll")
        newDll.writeText("new-dll")

        val updater = NativeSelfUpdater(isWindows = true)
        updater.applyUpdate(extracted, oldBinary)

        assertEquals("new-binary.exe", oldBinary.readText())
        assertEquals("new-dll", oldDll.readText())
    }

    @Test
    fun `cleanupOldBackups deletes dot old and dot bak files`() {
        val installDir = File(tempDir, "install-clean")
        installDir.mkdirs()

        val active = File(installDir, "melo")
        active.writeText("active")

        val old1 = File(installDir, "melo.old")
        old1.writeText("old1")

        val old2 = File(installDir, "melo.old.1234567")
        old2.writeText("old2")

        val bak = File(installDir, "SMTCAdapter.dll.bak")
        bak.writeText("bak")

        val updater = NativeSelfUpdater(isWindows = false)
        updater.cleanupOldBackups(installDir)

        assertTrue(active.exists())
        assertFalse(old1.exists())
        assertFalse(old2.exists())
        assertFalse(bak.exists())
    }

    @Test
    fun `getDefaultExecutable returns path ending with melo`() {
        val unixUpdater = NativeSelfUpdater(isWindows = false)
        val defaultUnix = unixUpdater.getDefaultExecutable()
        assertTrue(defaultUnix.absolutePath.endsWith("melo"))

        val winUpdater = NativeSelfUpdater(isWindows = true)
        val defaultWin = winUpdater.getDefaultExecutable()
        assertTrue(defaultWin.absolutePath.endsWith("melo.exe"))
    }
}
