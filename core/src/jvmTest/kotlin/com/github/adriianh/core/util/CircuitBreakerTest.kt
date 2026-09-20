package com.github.adriianh.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class CircuitBreakerTest {

    @Test
    fun `successful executions keep circuit CLOSED`() = runTest {
        val breaker = CircuitBreaker("test", failureThreshold = 3, cooldownDuration = 1.seconds)
        val result = breaker.execute { "success" }

        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState())
    }

    @Test
    fun `failures below threshold keep circuit CLOSED`() = runTest {
        val breaker = CircuitBreaker("test", failureThreshold = 3, cooldownDuration = 1.seconds)

        breaker.execute { throw RuntimeException("Error 1") }
        breaker.execute { throw RuntimeException("Error 2") }

        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState())
    }

    @Test
    fun `reaching failure threshold opens the circuit`() = runTest {
        val breaker = CircuitBreaker("test", failureThreshold = 3, cooldownDuration = 1.seconds)

        repeat(3) {
            breaker.execute { throw RuntimeException("Error") }
        }

        assertEquals(CircuitBreaker.State.OPEN, breaker.getState())
        val result = breaker.execute { "should fail fast" }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CircuitBreakerOpenException)
        assertNull(breaker.executeOrNull { "should return null" })
    }

    @Test
    fun `CancellationException is rethrown and does NOT increment failure count`() = runTest {
        val breaker = CircuitBreaker("test", failureThreshold = 2, cooldownDuration = 1.seconds)

        repeat(5) {
            assertFailsWith<CancellationException> {
                breaker.execute { throw CancellationException("Coroutine cancelled") }
            }
        }

        // Circuit must remain CLOSED because cancellation is not an operational failure
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState())

        val result = breaker.execute { "still working" }
        assertTrue(result.isSuccess)
        assertEquals("still working", result.getOrNull())
    }

    @Test
    fun `success resets failure count`() = runTest {
        val breaker = CircuitBreaker("test", failureThreshold = 3, cooldownDuration = 1.seconds)

        breaker.execute { throw RuntimeException("Error 1") }
        breaker.execute { throw RuntimeException("Error 2") }
        breaker.execute { "success" }

        // Two more errors should not trip it because count was reset by success
        breaker.execute { throw RuntimeException("Error 3") }
        breaker.execute { throw RuntimeException("Error 4") }

        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState())
    }

    @Test
    fun `retryWithBackoff rethrows CancellationException immediately without retrying`() = runTest {
        var attempts = 0
        assertFailsWith<CancellationException> {
            retryWithBackoff(maxRetries = 3, initialDelayMs = 10L) {
                attempts++
                throw CancellationException("Cancelled")
            }
        }

        assertEquals(1, attempts)
    }
}
