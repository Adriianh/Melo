package com.github.adriianh.data.local

import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual object DatabaseFactory {
    actual fun create(): MeloDatabase {
        val context = ContextHolder.context ?: error("Android Context must be provided before creating database")
        return MeloDatabase(
            driver = AndroidSqliteDriver(
                schema = MeloDatabase.Schema,
                context = context,
                name = "melo.db"
            )
        )
    }
}