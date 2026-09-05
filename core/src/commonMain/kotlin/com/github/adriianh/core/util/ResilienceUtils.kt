package com.github.adriianh.core.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Lightweight Circuit Breaker for multiplatform Kotlin (KMP).
 *
 * Tracks failure counts over rolling windows. If failures exceed [failureThreshold],
 * the circuit transitions to OPEN and calls fail-fast with [CircuitBreakerOpenException]
 * until [cooldownDuration] elapses, after which it enters HALF-OPEN state to probe recovery.
 */
class CircuitBreaker(
    val name: String,
    private val failureThreshold: Int = 3,
    private val cooldownDuration: Duration = 60.seconds,
) {
    enum class State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private val mutex = Mutex()
    private var failureCount = 0
    private var lastFailureTimestampMs = 0L
    private var state = State.CLOSED

    suspend fun getState(): State = mutex.withLock {
        updateStateLocked()
        state
    }

    suspend fun <T> execute(block: suspend () -> T): Result<T> {
        mutex.withLock {
            updateStateLocked()
            if (state == State.OPEN) {
                return Result.failure(CircuitBreakerOpenException(name))
            }
        }

        return try {
            val result = block()
            onSuccess()
            Result.success(result)
        } catch (e: Exception) {
            onFailure()
            Result.failure(e)
        }
    }

    suspend fun <T> executeOrNull(block: suspend () -> T): T? {
        return execute(block).getOrNull()
    }

    private suspend fun onSuccess() {
        mutex.withLock {
            failureCount = 0
            state = State.CLOSED
        }
    }

    private suspend fun onFailure() {
        mutex.withLock {
            lastFailureTimestampMs = Clock.System.now().toEpochMilliseconds()
            failureCount++
            if (failureCount >= failureThreshold) {
                state = State.OPEN
            }
        }
    }

    private fun updateStateLocked() {
        if (state == State.OPEN) {
            val now = Clock.System.now().toEpochMilliseconds()
            if (now - lastFailureTimestampMs >= cooldownDuration.inWholeMilliseconds) {
                state = State.HALF_OPEN
            }
        }
    }

    suspend fun reset() {
        mutex.withLock {
            failureCount = 0
            lastFailureTimestampMs = 0L
            state = State.CLOSED
        }
    }
}

class CircuitBreakerOpenException(circuitName: String) :
    Exception("Circuit breaker '$circuitName' is OPEN and failing fast.")

/**
 * Retries a suspending [block] up to [maxRetries] times with exponential backoff.
 */
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 2,
    initialDelayMs: Long = 300L,
    maxDelayMs: Long = 2000L,
    factor: Double = 2.0,
    predicate: (Throwable) -> Boolean = { true },
    block: suspend () -> T,
): T {
    var currentDelay = initialDelayMs
    var lastException: Throwable? = null

    for (attempt in 0..maxRetries) {
        try {
            return block()
        } catch (e: Throwable) {
            lastException = e
            if (attempt == maxRetries || !predicate(e)) {
                throw e
            }
            delay(currentDelay.milliseconds)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
    }

    throw lastException ?: IllegalStateException("Retry failed with no exception captured")
}