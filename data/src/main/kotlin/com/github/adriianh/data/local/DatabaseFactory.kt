package com.github.adriianh.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

object DatabaseFactory {
    fun create(): MeloDatabase {
        val dbDir = File(System.getProperty("user.home"), ".melo")
        dbDir.mkdirs()
        val dbFile = File(dbDir, "melo.db")

        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        MeloDatabase.Schema.create(driver)

        return MeloDatabase(driver)
    }
}