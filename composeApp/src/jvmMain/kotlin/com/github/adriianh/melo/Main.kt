package com.github.adriianh.melo

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.github.adriianh.core.domain.player.DiscordRpcManager
import com.github.adriianh.core.domain.player.InstanceIdentity
import com.github.adriianh.core.domain.player.MediaSessionManager
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.melo.di.initKoin
import com.github.adriianh.melo.ui.LocalInstanceLabel
import com.github.adriianh.melo.ui.titlebar.DesktopWindowManager
import com.github.adriianh.melo.ui.titlebar.MeloTitleBar
import com.github.adriianh.melo.ui.titlebar.WindowResizer
import com.github.adriianh.melo.util.LocalWindowActivity
import com.github.adriianh.melo.util.WindowActivityState
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.util.concurrent.atomic.AtomicReference
import java.awt.Window as AwtWindow

private fun configureDesktopRenderApi() {
    val isWindows = System.getProperty("os.name")?.contains("win", ignoreCase = true) == true
    if (isWindows &&
        System.getProperty("skiko.renderApi").isNullOrBlank() &&
        System.getenv("SKIKO_RENDER_API").isNullOrBlank()
    ) {
        System.setProperty("skiko.renderApi", "OPENGL")
    }
}

fun main() {
    configureDesktopRenderApi()
    val koinApp = initKoin()
    val instanceIdentity = koinApp.koin.getOrNull<InstanceIdentity>()
    val instanceKey = instanceIdentity?.instanceKey
    val windowTitle =
        instanceIdentity
            ?.takeUnless { it.isPrimary }
            ?.let { "Melo — ${it.instanceKey}" }
            ?: "Melo"

    val warmupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    warmupScope.launch {
        koinApp.koin.getOrNull<MeloPlayer>()
        koinApp.koin.getOrNull<MediaSessionManager>()
        koinApp.koin.getOrNull<DiscordRpcManager>()
    }

    application {
        val activeWindowRef = remember { AtomicReference<AwtWindow?>(null) }
        val shutdownCoordinator =
            remember {
                DesktopShutdownCoordinator(
                    cleanupTasks =
                        listOf(
                            { warmupScope.cancel() },
                            { koinApp.koin.getOrNull<PlaybackManager>()?.release() },
                            { koinApp.koin.getOrNull<MediaSessionManager>()?.release() },
                            { koinApp.koin.getOrNull<DiscordRpcManager>()?.release() },
                            { koinApp.koin.getOrNull<HttpClient>()?.close() },
                        ),
                    onAfterWait = { koinApp.close() },
                    onExit = ::exitApplication,
                )
            }
        val requestClose: () -> Unit = {
            shutdownCoordinator.requestClose {
                activeWindowRef.get()?.isVisible = false
            }
        }
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val defaultDevice = ge.defaultScreenDevice
        val gc =
            defaultDevice?.defaultConfiguration
                ?: ge.screenDevices.firstOrNull()?.defaultConfiguration

        val (initialWidthDp, initialHeightDp) =
            if (gc != null) {
                val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
                val screenBounds = gc.bounds

                val scaleX =
                    gc.defaultTransform
                        .scaleX
                        .toFloat()
                        .coerceAtLeast(1f)
                val scaleY =
                    gc.defaultTransform
                        .scaleY
                        .toFloat()
                        .coerceAtLeast(1f)

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

        val windowState =
            rememberWindowState(
                size = DpSize(initialWidthDp, initialHeightDp),
                position = WindowPosition.Aligned(Alignment.Center),
            )

        Window(
            onCloseRequest = requestClose,
            state = windowState,
            title = windowTitle,
            undecorated = true,
        ) {
            val windowManager = remember(window) { DesktopWindowManager(window) }

            DisposableEffect(window) {
                activeWindowRef.set(window)
                window.minimumSize = Dimension(800, 540)
                windowManager.attach()
                val resizer = WindowResizer(window, isMaximized = { windowManager.isMaximized })
                window.addMouseListener(resizer)
                window.addMouseMotionListener(resizer)
                onDispose {
                    activeWindowRef.compareAndSet(window, null)
                    window.removeMouseListener(resizer)
                    window.removeMouseMotionListener(resizer)
                    windowManager.detach()
                }
            }

            val windowActivity =
                when {
                    windowManager.isMinimized -> WindowActivityState.MINIMIZED
                    windowManager.isFocused -> WindowActivityState.ACTIVE
                    else -> WindowActivityState.UNFOCUSED
                }

            CompositionLocalProvider(
                LocalInstanceLabel provides instanceKey,
                LocalWindowActivity provides windowActivity,
            ) {
                App(
                    titleBar = {
                        MeloTitleBar(
                            windowManager = windowManager,
                            onClose = requestClose,
                        )
                    },
                )
            }
        }
    }
}
