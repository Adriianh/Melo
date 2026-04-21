package com.github.adriianh.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.sqlite.JDBC
import java.io.File
import java.sql.DriverManager
import java.util.*

object DatabaseFactory {
    fun create(): MeloDatabase {
        try {
            DriverManager.registerDriver(JDBC())
        } catch (_: Exception) {
            // Driver might already be registered
        }
        val dbDir = File(System.getProperty("user.home"), ".melo")
        dbDir.mkdirs()
        val dbFile = File(dbDir, "melo.db")
        val url = "jdbc:sqlite:${dbFile.absolutePath}"

        val currentVersion = readUserVersion(url)
        val driver = JdbcSqliteDriver(
            url = url,
            properties = Properties().apply { put("foreign_keys", "true") },
        )
        val schemaVersion = MeloDatabase.Schema.version

        if (currentVersion == 0L) {
            MeloDatabase.Schema.create(driver)
            writeUserVersion(url, schemaVersion)
        } else if (currentVersion < schemaVersion) {
            MeloDatabase.Schema.migrate(driver, currentVersion, schemaVersion)
            writeUserVersion(url, schemaVersion)
        }

        return MeloDatabase(driver)
    }

    private fun readUserVersion(url: String): Long {
        getConnection(url).use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("PRAGMA user_version")
                return if (rs.next()) rs.getLong(1) else 0L
            }
        }
    }

    private fun writeUserVersion(url: String, version: Long) {
        getConnection(url).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA user_version = $version")
            }
        }
    }

    private fun getConnection(url: String): java.sql.Connection {
        return try {
            DriverManager.getConnection(url)
        } catch (e: Exception) {
            // Fallback for GraalVM Native Image where DriverManager discovery might fail
            JDBC().connect(url, Properties())
        }
    }
}