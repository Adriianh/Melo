package com.github.adriianh.melo

import android.app.Application
import com.github.adriianh.core.domain.player.MediaSessionManager
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.data.local.ContextHolder
import com.github.adriianh.melo.di.initKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext

class MeloApp : Application() {
    override fun onCreate() {
        super.onCreate()

        ContextHolder.context = this

        val koinApp = initKoin {
            androidContext(this@MeloApp)
        }

        CoroutineScope(Dispatchers.IO).launch {
            koinApp.koin.getOrNull<MeloPlayer>()
            koinApp.koin.getOrNull<MediaSessionManager>()
        }
    }
}
