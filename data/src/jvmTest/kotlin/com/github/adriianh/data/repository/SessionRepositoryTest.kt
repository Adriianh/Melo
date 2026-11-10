package com.github.adriianh.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.SavedSession
import com.github.adriianh.data.local.MeloDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SessionRepositoryTest {
    private fun fakeTrack(id: String) =
        Track(
            id = id,
            title = "Track $id",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000,
            genres = emptyList(),
            artworkUrl = null,
            sourceId = null,
        )

    private fun session(
        trackId: String,
        queueIndex: Int = 0,
        positionMs: Long = 0L,
    ) = SavedSession(
        queue = listOf(fakeTrack(trackId)),
        queueIndex = queueIndex,
        positionMs = positionMs,
    )

    @Test
    fun `save and restore round-trips the queue for an instance`() =
        runTest {
            val db = createInMemoryDatabase()
            val repo = SessionRepositoryImpl(db, instanceKey = "gui")
            val saved =
                SavedSession(
                    queue = listOf(fakeTrack("1"), fakeTrack("2")),
                    queueIndex = 1,
                    positionMs = 42_000L,
                )

            repo.saveSession(saved)

            val restored = repo.restoreSession()
            assertNotNull(restored)
            assertEquals(listOf("1", "2"), restored.queue.map { it.id })
            assertEquals(1, restored.queueIndex)
            assertEquals(42_000L, restored.positionMs)
        }

    @Test
    fun `instances keep independent sessions`() =
        runTest {
            val db = createInMemoryDatabase()
            val gui = SessionRepositoryImpl(db, instanceKey = "gui")
            val tui = SessionRepositoryImpl(db, instanceKey = "tui")

            gui.saveSession(session("gui-track"))
            tui.saveSession(session("tui-track"))

            val guiRestored = assertNotNull(gui.restoreSession())
            val tuiRestored = assertNotNull(tui.restoreSession())
            val guiTrack = guiRestored.queue.single()
            val tuiTrack = tuiRestored.queue.single()
            assertEquals("gui-track", guiTrack.id)
            assertEquals("tui-track", tuiTrack.id)
        }

    @Test
    fun `clearSession only clears the calling instance`() =
        runTest {
            val db = createInMemoryDatabase()
            val gui = SessionRepositoryImpl(db, instanceKey = "gui")
            val tui = SessionRepositoryImpl(db, instanceKey = "tui")
            gui.saveSession(session("gui-track"))
            tui.saveSession(session("tui-track"))

            gui.clearSession()

            assertNull(gui.restoreSession())
            val tuiRestored = assertNotNull(tui.restoreSession())
            val tuiTrack = tuiRestored.queue.single()
            assertEquals("tui-track", tuiTrack.id)
        }

    @Test
    fun `saving an empty queue or a negative index does not persist`() =
        runTest {
            val db = createInMemoryDatabase()
            val repo = SessionRepositoryImpl(db, instanceKey = "gui")

            repo.saveSession(SavedSession(emptyList(), queueIndex = -1, positionMs = 0L))
            repo.saveSession(SavedSession(listOf(fakeTrack("1")), queueIndex = -1, positionMs = 0L))

            assertNull(repo.restoreSession())
        }

    @Test
    fun `role is derived from the instance key`() =
        runTest {
            val db = createInMemoryDatabase()
            val repo = SessionRepositoryImpl(db, instanceKey = "gui.instance123")

            repo.saveSession(session("1"))

            val row = db.sessionQueries.selectSession("gui.instance123").executeAsOne()
            assertEquals("gui", row.role)
        }

    @Test
    fun `migration moves the legacy single-row session to the gui instance`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """
            CREATE TABLE session (
                id          INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
                queue_json  TEXT    NOT NULL,
                queue_index INTEGER NOT NULL DEFAULT -1,
                position_ms INTEGER NOT NULL DEFAULT 0,
                saved_at    INTEGER NOT NULL
            )
            """.trimIndent(),
            0,
        )
        val legacyQueueJson =
            """[{"id":"legacy","title":"Legacy","artist":"Artist","album":"Album","durationMs":180000,"genres":[]}]"""
        driver.execute(
            null,
            """
            INSERT INTO session (id, queue_json, queue_index, position_ms, saved_at)
            VALUES (1, '$legacyQueueJson', 2, 45000, 123456)
            """.trimIndent(),
            0,
        )

        MeloDatabase.Schema.migrate(driver, 6, MeloDatabase.Schema.version)

        val db = MeloDatabase(driver)
        val row = db.sessionQueries.selectSession("gui").executeAsOne()
        assertEquals("gui", row.instance_key)
        assertEquals("gui", row.role)
        assertEquals(legacyQueueJson, row.queue_json)
        assertEquals(2L, row.queue_index)
        assertEquals(45_000L, row.position_ms)
        assertEquals(123_456L, row.saved_at)
        assertNull(row.pid)
        assertNull(row.host)
    }
}
