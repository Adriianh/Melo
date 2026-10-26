package com.github.adriianh.data.auth

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrowserAuthManagerTest {
    private val tempDir: File = Files.createTempDirectory("melo_browser_auth_test_").toFile()

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `selectAvailableBrowsers keeps detection order and skips missing paths`() {
        val firefox = executableFile("firefox")
        val chrome = executableFile("chrome")
        val missing = File(tempDir, "missing-browser")

        val selected =
            BrowserAuthManager.selectAvailableBrowsers(
                listOf(
                    BrowserCandidate("Mozilla Firefox", firefox.path, BrowserEngine.GECKO),
                    BrowserCandidate("Missing", missing.path, BrowserEngine.CHROMIUM),
                    BrowserCandidate("Google Chrome", chrome.path, BrowserEngine.CHROMIUM),
                ),
            )

        assertEquals(listOf(firefox.path, chrome.path), selected.map { it.path })
    }

    @Test
    fun `selectAvailableBrowsers skips files without execute permission`() {
        // Windows reports every existing file as executable, so the condition cannot be reproduced there.
        if (HostOs.current == HostOs.WINDOWS) return

        val notExecutable =
            File(tempDir, "not-executable").apply {
                writeText("#!/bin/sh\n")
                setExecutable(false, false)
            }
        assertFalse(notExecutable.canExecute())

        val selected =
            BrowserAuthManager.selectAvailableBrowsers(
                listOf(BrowserCandidate("Broken Browser", notExecutable.path, BrowserEngine.GECKO)),
            )

        assertTrue(selected.isEmpty())
    }

    private fun executableFile(name: String): File =
        File(tempDir, name).apply {
            writeText("#!/bin/sh\n")
            setExecutable(true, false)
        }
}
