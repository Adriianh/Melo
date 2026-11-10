package com.github.adriianh.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.github.adriianh.core.platform.MeloDataDirectory
import org.sqlite.JDBC
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

actual object DatabaseFactory {
    actual fun create(): MeloDatabase = create(File(MeloDataDirectory.resolve(), "melo.db"))

    /**
     * Opens the database at [dbFile], creating or migrating the schema as needed.
     *
     * Internal so tests can exercise a throwaway database instead of the user's one.
     */
    internal fun create(dbFile: File): MeloDatabase {
        registerDriver()
        val url = databaseUrl(dbFile)

        val currentVersion = readUserVersion(url)
        val driver = openDriver(url)
        val schemaVersion = MeloDatabase.Schema.version

        if (currentVersion == 0L) {
            MeloDatabase.Schema.create(driver)
            writeUserVersion(url, schemaVersion)
        } else if (currentVersion < schemaVersion) {
            MeloDatabase.Schema.migrate(driver, currentVersion, schemaVersion)
            writeUserVersion(url, schemaVersion)
        }

        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS favorite_entities (
                id          TEXT    NOT NULL PRIMARY KEY,
                type        TEXT    NOT NULL,
                title       TEXT    NOT NULL,
                subtitle    TEXT,
                artwork_url TEXT,
                track_count INTEGER,
                added_at    INTEGER NOT NULL
            )
            """.trimIndent(),
            0,
        )

        return MeloDatabase(driver)
    }

    /** Opens another connection to a database that already has its schema. Internal for tests. */
    internal fun openExisting(dbFile: File): MeloDatabase {
        registerDriver()
        return MeloDatabase(openDriver(databaseUrl(dbFile)))
    }

    internal fun databaseUrl(dbFile: File): String = "jdbc:sqlite:${dbFile.absolutePath}"

    /**
     * SQLite connection settings shared by every connection.
     *
     * The JDBC driver opens one connection per thread (`ThreadedConnectionManager`), so the
     * PRAGMAs are passed as connection properties instead of being executed once: that way
     * WAL, the busy timeout and NORMAL sync apply to every connection, not just the first.
     * This keeps concurrent Melo instances from failing with `SQLITE_BUSY` and lets readers
     * and writers proceed without blocking each other.
     */
    internal fun sqliteProperties(): Properties =
        Properties().apply {
            put("foreign_keys", "true")
            put("busy_timeout", "5000")
            put("journal_mode", "WAL")
            put("synchronous", "NORMAL")
        }

    internal fun openDriver(url: String): JdbcSqliteDriver {
        val properties = sqliteProperties()
        return JdbcSqliteDriver(url = url, properties = properties)
    }

    private fun registerDriver() {
        try {
            DriverManager.registerDriver(JDBC())
        } catch (_: Exception) {
            // Driver might already be registered
        }
    }

    private fun readUserVersion(url: String): Long =
        getConnection(url).use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("PRAGMA user_version")
                if (rs.next()) rs.getLong(1) else 0L
            }
        }

    private fun writeUserVersion(
        url: String,
        version: Long,
    ) {
        getConnection(url).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA user_version = $version")
            }
        }
    }

    private fun getConnection(url: String): Connection =
        try {
            DriverManager.getConnection(url, sqliteProperties())
        } catch (_: Exception) {
            // Fallback for GraalVM Native Image where DriverManager discovery might fail
            JDBC().connect(url, sqliteProperties())
        }
}
