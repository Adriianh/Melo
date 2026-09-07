package com.github.adriianh.melo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
import com.github.adriianh.melo.ui.titlebar.DesktopWindowManager
import com.github.adriianh.melo.ui.titlebar.MeloTitleBar
import com.github.adriianh.melo.ui.titlebar.WindowResizer
import com.github.adriianh.melo.util.DarkMeloColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Toolkit

fun main() {
    val koinApp = initKoin()

    CoroutineScope(Dispatchers.IO).launch {
        koinApp.koin.getOrNull<MeloPlayer>()
        koinApp.koin.getOrNull<MediaSessionManager>()
    }

    application {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val defaultDevice = ge.defaultScreenDevice
        val gc = defaultDevice?.defaultConfiguration
            ?: ge.screenDevices.firstOrNull()?.defaultConfiguration

        val (initialWidthDp, initialHeightDp) = if (gc != null) {
            val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
            val screenBounds = gc.bounds

            val scaleX = gc.defaultTransform.scaleX.toFloat().coerceAtLeast(1f)
            val scaleY = gc.defaultTransform.scaleY.toFloat().coerceAtLeast(1f)

            val usableWidthPx = screenBounds.width - insets.left - insets.right
            val usableHeightPx = screenBounds.height - insets.top - insets.bottom

            val initialWidthPx =
                (usableWidthPx * 0.85f).toInt().coerceIn(900.coerceAtMost(usableWidthPx), 1200)
            val initialHeightPx =
                (usableHeightPx * 0.85f).toInt().coerceIn(580.coerceAtMost(usableHeightPx), 750)

            Pair((initialWidthPx / scaleX).dp, (initialHeightPx / scaleY).dp)
        } else {
            Pair(1080.dp, 680.dp)
        }

        val windowState = rememberWindowState(
            size = DpSize(initialWidthDp, initialHeightDp),
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
            val windowManager = remember(window) { DesktopWindowManager(window) }

            DisposableEffect(window) {
                window.minimumSize = Dimension(800, 540)
                val resizer = WindowResizer(window, isMaximized = { windowManager.isMaximized })
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
                    windowManager = windowManager,
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