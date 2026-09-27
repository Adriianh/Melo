package com.github.adriianh.core.platform

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformFileSystemTest {
    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "pfs_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `writeStream correctly streams chunks to disk and creates parent directories`() =
        runTest {
            val nestedFile = File(tempDir, "sub/dir/test_stream.bin")
            val chunk1 = ByteArray(1024) { 1 }
            val chunk2 = ByteArray(2048) { 2 }
            val chunk3 = ByteArray(512) { 3 }

            val bytesWritten =
                PlatformFileSystem.writeStream(nestedFile.absolutePath) { writeChunk ->
                    writeChunk(chunk1, 0, chunk1.size)
                    writeChunk(chunk2, 0, chunk2.size)
                    writeChunk(chunk3, 0, chunk3.size)
                }

            assertEquals(1024L + 2048L + 512L, bytesWritten)
            assertTrue(nestedFile.exists())
            assertEquals(3584L, nestedFile.length())

            val readBytes = PlatformFileSystem.readBytes(nestedFile.absolutePath)
            assertEquals(3584, readBytes?.size)
            assertEquals(1.toByte(), readBytes?.get(0))
            assertEquals(2.toByte(), readBytes?.get(1024))
            assertEquals(3.toByte(), readBytes?.get(1024 + 2048))
        }

    @Test
    fun `fileSize returns correct length and fileExists reflects existence`() {
        val file = File(tempDir, "exists.txt")
        file.writeBytes(ByteArray(256) { 9 })

        assertTrue(PlatformFileSystem.fileExists(file.absolutePath))
        assertEquals(256L, PlatformFileSystem.fileSize(file.absolutePath))
    }
}
