package com.github.adriianh.core.platform

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MeloDataDirectoryTest {
    private lateinit var testBaseDir: File
    private lateinit var legacyDir: File
    private lateinit var targetDir: File

    @BeforeTest
    fun setUp() {
        testBaseDir = File(System.getProperty("java.io.tmpdir"), "melo_data_dir_test_${System.currentTimeMillis()}")
        testBaseDir.mkdirs()
        legacyDir = File(testBaseDir, "legacy_home/.melo")
        targetDir = File(testBaseDir, "appdata/Melo")
    }

    @AfterTest
    fun tearDown() {
        testBaseDir.deleteRecursively()
    }

    @Test
    fun `resolveWindowsDirectory returns APPDATA Melo when APPDATA is provided`() {
        val appData = File(testBaseDir, "custom_appdata").absolutePath
        val resolved =
            MeloDataDirectory.resolveWindowsDirectory(
                appData = appData,
                userHome = testBaseDir.absolutePath,
            )

        assertEquals(File(appData, "Melo").canonicalPath, resolved.canonicalPath)
        assertTrue(resolved.exists())
    }

    @Test
    fun `resolveWindowsDirectory falls back to userHome dot melo when APPDATA is null or blank`() {
        val userHome = File(testBaseDir, "fallback_home").absolutePath
        val resolvedWithNull =
            MeloDataDirectory.resolveWindowsDirectory(
                appData = null,
                userHome = userHome,
            )
        val resolvedWithBlank =
            MeloDataDirectory.resolveWindowsDirectory(
                appData = "   ",
                userHome = userHome,
            )

        val expected = File(userHome, ".melo").canonicalPath
        assertEquals(expected, resolvedWithNull.canonicalPath)
        assertEquals(expected, resolvedWithBlank.canonicalPath)
    }

    @Test
    fun `migrateWindowsLegacyDirIfNeeded copies entire folder when target is empty or non-existent`() {
        legacyDir.mkdirs()
        File(legacyDir, "melo.db").writeText("sqlite db binary placeholder")
        File(legacyDir, "settings.json").writeText("""{"volume":80}""")
        val subDir = File(legacyDir, "cache/images").apply { mkdirs() }
        File(subDir, "cover.png").writeBytes(byteArrayOf(1, 2, 3))

        MeloDataDirectory.migrateWindowsLegacyDirIfNeeded(legacyDir, targetDir)

        assertTrue(targetDir.exists())
        assertEquals("sqlite db binary placeholder", File(targetDir, "melo.db").readText())
        assertEquals("""{"volume":80}""", File(targetDir, "settings.json").readText())
        assertTrue(File(targetDir, "cache/images/cover.png").exists())
    }

    @Test
    fun `migrateWindowsLegacyDirIfNeeded preserves existing files and copies missing key files`() {
        legacyDir.mkdirs()
        File(legacyDir, "melo.db").writeText("legacy db")
        File(legacyDir, "settings.json").writeText("legacy settings")
        File(legacyDir, "sts.txt").writeText("legacy sts")

        targetDir.mkdirs()
        File(targetDir, "settings.json").writeText("existing new settings")

        MeloDataDirectory.migrateWindowsLegacyDirIfNeeded(legacyDir, targetDir)

        assertEquals("existing new settings", File(targetDir, "settings.json").readText())
        assertEquals("legacy db", File(targetDir, "melo.db").readText())
        assertEquals("legacy sts", File(targetDir, "sts.txt").readText())
    }

    @Test
    fun `migrateWindowsLegacyDirIfNeeded does nothing when legacy directory does not exist`() {
        assertFalse(legacyDir.exists())
        MeloDataDirectory.migrateWindowsLegacyDirIfNeeded(legacyDir, targetDir)
        assertFalse(targetDir.exists())
    }

    @Test
    fun `migrateWindowsLegacyDirIfNeeded does nothing when source and target are the same`() {
        legacyDir.mkdirs()
        File(legacyDir, "settings.json").writeText("settings")
        MeloDataDirectory.migrateWindowsLegacyDirIfNeeded(legacyDir, legacyDir)
        assertEquals("settings", File(legacyDir, "settings.json").readText())
    }
}
