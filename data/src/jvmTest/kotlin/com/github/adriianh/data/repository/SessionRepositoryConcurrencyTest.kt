package com.github.adriianh.data.repository

import com.github.adriianh.data.local.DatabaseFactory
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionRepositoryConcurrencyTest {
    private val tempDir: File = Files.createTempDirectory("melo-session-concurrency").toFile()
    private val dbFile = File(tempDir, "melo.db")

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `database uses WAL and a busy timeout`() {
        DatabaseFactory.create(dbFile)
        val url = DatabaseFactory.databaseUrl(dbFile)

        DriverManager.getConnection(url, DatabaseFactory.sqliteProperties()).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA journal_mode").use { rs ->
                    assertTrue(rs.next())
                    assertEquals("wal", rs.getString(1).lowercase())
                }
                statement.executeQuery("PRAGMA busy_timeout").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(5_000, rs.getInt(1))
                }
                statement.executeQuery("PRAGMA foreign_keys").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `two connections can write concurrently without SQLITE_BUSY`() {
        val first = DatabaseFactory.create(dbFile)
        val second = DatabaseFactory.openExisting(dbFile)
        val firstQueries = first.sessionQueries
        val secondQueries = second.sessionQueries

        val writes = 200
        val start = CyclicBarrier(2)
        val errors = ConcurrentLinkedQueue<Throwable>()

        val firstWriter =
            thread(name = "melo-db-writer-1") {
                try {
                    start.await()
                    repeat(writes) { index ->
                        firstQueries.upsertSession(
                            instance_key = "gui",
                            role = "gui",
                            queue_json = "[]",
                            queue_index = index.toLong(),
                            position_ms = 0L,
                            saved_at = index.toLong(),
                            pid = null,
                            host = null,
                        )
                    }
                } catch (t: Throwable) {
                    errors.add(t)
                }
            }
        val secondWriter =
            thread(name = "melo-db-writer-2") {
                try {
                    start.await()
                    repeat(writes) { index ->
                        secondQueries.upsertSession(
                            instance_key = "tui",
                            role = "tui",
                            queue_json = "[]",
                            queue_index = index.toLong(),
                            position_ms = 0L,
                            saved_at = index.toLong(),
                            pid = null,
                            host = null,
                        )
                    }
                } catch (t: Throwable) {
                    errors.add(t)
                }
            }

        firstWriter.join(30_000)
        secondWriter.join(30_000)

        assertTrue(errors.isEmpty(), "Concurrent writes failed: ${errors.firstOrNull()}")
        assertEquals(
            (writes - 1).toLong(),
            firstQueries.selectSession("gui").executeAsOne().queue_index,
        )
        assertEquals(
            (writes - 1).toLong(),
            secondQueries.selectSession("tui").executeAsOne().queue_index,
        )
    }
}
