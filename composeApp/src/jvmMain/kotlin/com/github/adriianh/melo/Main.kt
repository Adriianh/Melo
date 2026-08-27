package com.github.adriianh.melo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.adriianh.core.domain.player.MediaSessionManager
import com.github.adriianh.melo.di.initKoin

fun main() {
    val koinApp = initKoin()
    val mediaSessionManager: MediaSessionManager? = koinApp.koin.getOrNull()

    application {
        Window(
            onCloseRequest = {
                mediaSessionManager?.release()
                exitApplication()
            },
            title = "Melo"
        ) {
            App()
        }
    }
}