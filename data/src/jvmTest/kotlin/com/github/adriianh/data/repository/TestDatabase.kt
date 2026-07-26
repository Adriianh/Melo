package com.github.adriianh.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.github.adriianh.data.local.MeloDatabase

fun createInMemoryDatabase(): MeloDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    MeloDatabase.Schema.create(driver)
    return MeloDatabase(driver)
}