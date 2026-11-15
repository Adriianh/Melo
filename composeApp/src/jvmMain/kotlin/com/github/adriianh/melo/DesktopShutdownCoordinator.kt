package com.github.adriianh.melo

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities

internal const val DEFAULT_SHUTDOWN_TIMEOUT_MS = 1_500L

/**
 * Coordinates non-blocking desktop application shutdown off the AWT EDT.
 *
 * Ensures repeated close triggers are idempotent, hides the window immediately on the caller
 * thread, executes cleanup tasks concurrently on daemon threads bounded by [timeoutMs], and
 * dispatches [onExit] to the UI thread once cleanup completes or times out.
 */
internal class DesktopShutdownCoordinator(
    private val cleanupTasks: List<() -> Unit>,
    private val timeoutMs: Long = DEFAULT_SHUTDOWN_TIMEOUT_MS,
    private val dispatchToUi: (() -> Unit) -> Unit = { block -> SwingUtilities.invokeLater(block) },
    private val onAfterWait: () -> Unit = {},
    private val onExit: () -> Unit,
) {
    private val closing = AtomicBoolean(false)

    fun requestClose(hideWindow: () -> Unit = {}) {
        if (!closing.compareAndSet(false, true)) return
        runCatching { hideWindow() }

        Thread(
            {
                runBoundedCleanup()
                runCatching { onAfterWait() }
                dispatchToUi {
                    runCatching { onExit() }
                }
            },
            "Melo-Shutdown-Coordinator",
        ).apply {
            isDaemon = true
            start()
        }
    }

    private fun runBoundedCleanup() {
        if (cleanupTasks.isEmpty()) return
        val latch = CountDownLatch(cleanupTasks.size)
        cleanupTasks.forEachIndexed { index, task ->
            Thread(
                {
                    try {
                        runCatching { task() }
                    } finally {
                        latch.countDown()
                    }
                },
                "Melo-Shutdown-Task-$index",
            ).apply {
                isDaemon = true
                start()
            }
        }
        runCatching {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        }
    }
}
