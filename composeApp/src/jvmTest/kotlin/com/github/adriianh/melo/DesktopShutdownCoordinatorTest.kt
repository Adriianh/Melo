package com.github.adriianh.melo

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopShutdownCoordinatorTest {
    @Test
    fun `requestClose hides window immediately and runs cleanup before onExit`() {
        val hideCount = AtomicInteger(0)
        val cleanupCount = AtomicInteger(0)
        val afterWaitCount = AtomicInteger(0)
        val exitLatch = CountDownLatch(1)

        val coordinator =
            DesktopShutdownCoordinator(
                cleanupTasks =
                    listOf(
                        { cleanupCount.incrementAndGet() },
                        { cleanupCount.incrementAndGet() },
                    ),
                timeoutMs = 1_000L,
                dispatchToUi = { block -> block() },
                onAfterWait = { afterWaitCount.incrementAndGet() },
                onExit = { exitLatch.countDown() },
            )

        coordinator.requestClose { hideCount.incrementAndGet() }

        assertEquals(1, hideCount.get())
        assertTrue(exitLatch.await(1, TimeUnit.SECONDS))
        assertEquals(2, cleanupCount.get())
        assertEquals(1, afterWaitCount.get())
    }

    @Test
    fun `requestClose is idempotent across repeated triggers`() {
        val hideCount = AtomicInteger(0)
        val cleanupCount = AtomicInteger(0)
        val exitCount = AtomicInteger(0)
        val exitLatch = CountDownLatch(1)

        val coordinator =
            DesktopShutdownCoordinator(
                cleanupTasks = listOf { cleanupCount.incrementAndGet() },
                timeoutMs = 1_000L,
                dispatchToUi = { block -> block() },
                onExit = {
                    exitCount.incrementAndGet()
                    exitLatch.countDown()
                },
            )

        coordinator.requestClose { hideCount.incrementAndGet() }
        coordinator.requestClose { hideCount.incrementAndGet() }

        assertTrue(exitLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, hideCount.get())
        assertEquals(1, cleanupCount.get())
        assertEquals(1, exitCount.get())
    }

    @Test
    fun `hanging cleanup task does not block caller or prevent onExit after timeout`() {
        val hangingRelease = CountDownLatch(1)
        val fastTaskDone = CountDownLatch(1)
        val exitLatch = CountDownLatch(1)

        val coordinator =
            DesktopShutdownCoordinator(
                cleanupTasks =
                    listOf(
                        { hangingRelease.await(5, TimeUnit.SECONDS) },
                        { fastTaskDone.countDown() },
                    ),
                timeoutMs = 120L,
                dispatchToUi = { block -> block() },
                onExit = { exitLatch.countDown() },
            )

        val startNanos = System.nanoTime()
        coordinator.requestClose()
        val callerElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)

        assertTrue(callerElapsedMs < 100L, "requestClose blocked caller for ${callerElapsedMs}ms")
        assertTrue(fastTaskDone.await(1, TimeUnit.SECONDS))
        assertTrue(exitLatch.await(1, TimeUnit.SECONDS))
        hangingRelease.countDown()
    }

    @Test
    fun `throwing cleanup task does not prevent other tasks or onExit`() {
        val healthyTaskDone = CountDownLatch(1)
        val exitLatch = CountDownLatch(1)

        val coordinator =
            DesktopShutdownCoordinator(
                cleanupTasks =
                    listOf(
                        { error("boom") },
                        { healthyTaskDone.countDown() },
                    ),
                timeoutMs = 1_000L,
                dispatchToUi = { block -> block() },
                onExit = { exitLatch.countDown() },
            )

        coordinator.requestClose()

        assertTrue(healthyTaskDone.await(1, TimeUnit.SECONDS))
        assertTrue(exitLatch.await(1, TimeUnit.SECONDS))
    }
}
