package com.github.adriianh.melo

import android.app.Application
import com.github.adriianh.data.local.ContextHolder
import com.github.adriianh.melo.di.initKoin
import org.koin.android.ext.koin.androidContext

class MeloApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize legacy context holder for the moment
        ContextHolder.context = this

        initKoin {
            androidContext(this@MeloApp)
        }
    }
}
