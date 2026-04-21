package com.github.adriianh.data.local

import com.github.adriianh.data.local.MeloDatabase

expect object DatabaseFactory {
    fun create(): MeloDatabase
}
