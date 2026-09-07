package com.github.adriianh.melo

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.adriianh.core.domain.player.MediaSessionManager
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.melo.di.initKoin
import com.github.adriianh.melo.util.WindowsTitleBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() {
    val koinApp = initKoin()

    CoroutineScope(Dispatchers.IO).launch {
        koinApp.koin.getOrNull<MeloPlayer>()
        koinApp.koin.getOrNull<MediaSessionManager>()
    }

    application {
        Window(
            onCloseRequest = {
                koinApp.koin.getOrNull<MediaSessionManager>()?.release()
                exitApplication()
            },
            title = "Melo"
        ) {
            LaunchedEffect(window) {
                WindowsTitleBar.applyDarkTheme(window)
            }
            App()
        }
    }
}