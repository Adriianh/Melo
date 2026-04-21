package com.github.adriianh.data.local

import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual object DatabaseFactory {
    actual fun create(): MeloDatabase {
        return MeloDatabase(
            driver = NativeSqliteDriver(
                schema = MeloDatabase.Schema,
                name = "melo.db"
            )
        )
    }
}
