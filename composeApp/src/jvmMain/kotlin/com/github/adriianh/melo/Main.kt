package com.github.adriianh.melo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.github.adriianh.core.domain.player.MediaSessionManager
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.melo.di.initKoin
import com.github.adriianh.melo.ui.titlebar.MeloTitleBar
import com.github.adriianh.melo.ui.titlebar.WindowResizer
import com.github.adriianh.melo.util.DarkMeloColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Dimension

fun main() {
    val koinApp = initKoin()

    CoroutineScope(Dispatchers.IO).launch {
        koinApp.koin.getOrNull<MeloPlayer>()
        koinApp.koin.getOrNull<MediaSessionManager>()
    }

    application {
        val windowState = rememberWindowState(
            size = DpSize(1200.dp, 800.dp),
            position = WindowPosition.Aligned(Alignment.Center)
        )

        Window(
            onCloseRequest = {
                koinApp.koin.getOrNull<MediaSessionManager>()?.release()
                exitApplication()
            },
            state = windowState,
            title = "Melo",
            undecorated = true
        ) {
            DisposableEffect(window) {
                window.minimumSize = Dimension(900, 600)
                val resizer = WindowResizer(window, minWidth = 900, minHeight = 600)
                window.addMouseListener(resizer)
                window.addMouseMotionListener(resizer)
                onDispose {
                    window.removeMouseListener(resizer)
                    window.removeMouseMotionListener(resizer)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkMeloColors.surface0)
            ) {
                MeloTitleBar(
                    windowState = windowState,
                    onClose = {
                        koinApp.koin.getOrNull<MediaSessionManager>()?.release()
                        exitApplication()
                    }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    App()
                }
            }
        }
    }
}