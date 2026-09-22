package com.github.adriianh.cli.tui.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToastQueueTest {

    private fun toast(id: Long, shownAtMs: Long = 0L, ttlMs: Long = TOAST_TTL_MS) =
        ToastMessage(id = id, message = "msg$id", kind = ToastKind.INFO, shownAtMs = shownAtMs, ttlMs = ttlMs)

    @Test
    fun testPushAppendsNewestAsLast() {
        val first = pushToast(emptyList(), "A", ToastKind.INFO, nowMs = 1000L, id = 1L)
        val second = pushToast(first, "B", ToastKind.SUCCESS, nowMs = 2000L, id = 2L)

        assertEquals(listOf(1L, 2L), second.map { it.id })
        assertEquals("A", second[0].message)
        assertEquals("B", second[1].message)
        assertEquals(ToastKind.SUCCESS, second[1].kind)
        assertEquals(2000L, second[1].shownAtMs)
    }

    @Test
    fun testPushDropsOldestWhenCapacityExceeded() {
        var queue: List<ToastMessage> = emptyList()
        for (i in 1L..5L) {
            queue = pushToast(queue, "msg$i", ToastKind.INFO, nowMs = i * 1000L, id = i)
        }
        // Capacity is 3: only the three most recent survive, oldest dropped.
        assertEquals(listOf(3L, 4L, 5L), queue.map { it.id })
    }

    @Test
    fun testPushKeepsNewestAtBottomSlot() {
        var queue: List<ToastMessage> = emptyList()
        for (i in 1L..3L) {
            queue = pushToast(queue, "msg$i", ToastKind.INFO, nowMs = i * 1000L, id = i)
        }
        // Oldest-first ordering: last entry is the newest, drawn nearest the player bar.
        assertEquals(3L, queue.last().id)
        assertEquals(1L, queue.first().id)
    }

    @Test
    fun testPruneKeepsActiveAndLeavingToasts() {
        val now = 4_650L

        // Shown 3.5s ago: age == TTL, starts leaving but still within fade-out window.
        val leaving = toast(id = 1L, shownAtMs = now - TOAST_TTL_MS)
        // Shown 3.1s ago: still inside its TTL.
        val active = toast(id = 2L, shownAtMs = now - 3_100L)
        // Shown past TTL + fade-out: fully expired, must be dropped.
        val expired = toast(id = 3L, shownAtMs = now - TOAST_TTL_MS - TOAST_FADE_OUT_MS - 1L)

        val kept = pruneExpiredToasts(listOf(active, leaving, expired), now)

        assertEquals(listOf(2L, 1L), kept.map { it.id })
        assertTrue(kept.none { it.id == 3L })
    }

    @Test
    fun testPruneRemovesAfterTtlPlusFadeOut() {
        val now = 10_000L
        val toast = toast(id = 1L, shownAtMs = 0L, ttlMs = 3_000L)
        val fullyExpired = now >= 0L + 3_000L + TOAST_FADE_OUT_MS
        assertTrue(fullyExpired)
        assertEquals(emptyList(), pruneExpiredToasts(listOf(toast), now))
    }

    @Test
    fun testPhaseJumpingForNewestOnly() {
        val now = 10_000L
        val youngest = toast(id = 2L, shownAtMs = now - 100L)

        // Youngest is the newest: within the entrance window -> ENTERING.
        assertEquals(ToastPhase.ENTERING, toastPhase(youngest, now, isNewest = true))
        // Same age but not the newest -> ACTIVE (older toasts never re-animate their entrance).
        assertEquals(ToastPhase.ACTIVE, toastPhase(youngest, now, isNewest = false))

        // Oldest is past TTL -> LEAVING regardless of newest flag.
        val old = toast(id = 1L, shownAtMs = now - TOAST_TTL_MS - 1L)
        assertEquals(ToastPhase.LEAVING, toastPhase(old, now, isNewest = false))

        // A toast well inside its TTL and not the newest -> ACTIVE.
        val mid = toast(id = 3L, shownAtMs = now - 1_000L)
        assertEquals(ToastPhase.ACTIVE, toastPhase(mid, now, isNewest = false))
    }

    @Test
    fun testFadeWindowsAreShorterThanTtl() {
        assertTrue(TOAST_FADE_IN_MS < TOAST_TTL_MS)
        assertTrue(TOAST_FADE_OUT_MS < TOAST_TTL_MS)
        assertFalse(MAX_VISIBLE_TOASTS <= 0)
    }
}