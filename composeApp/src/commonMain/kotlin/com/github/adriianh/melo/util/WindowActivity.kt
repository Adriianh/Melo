package com.github.adriianh.melo.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val ACTIVE_TICK_DELAY_MS = 100L
private const val ACTIVE_TICK_STEP_SEC = 0.10f
private const val UNFOCUSED_TICK_DELAY_MS = 333L
private const val UNFOCUSED_TICK_STEP_SEC = 0.333f
private const val SHARED_CLOCK_CYCLE_SEC = 2520f

enum class WindowActivityState {
    ACTIVE,
    UNFOCUSED,
    MINIMIZED,
    ;

    val isVisible: Boolean
        get() = this != MINIMIZED
}

val LocalWindowActivity = compositionLocalOf { WindowActivityState.ACTIVE }

class SharedAnimationClock {
    private val _elapsedSeconds = mutableFloatStateOf(0f)
    val elapsedSeconds: FloatState get() = _elapsedSeconds

    private val activeConsumerCount = mutableIntStateOf(0)
    internal val hasConsumers: Boolean get() = activeConsumerCount.intValue > 0

    internal fun acquire() {
        activeConsumerCount.intValue += 1
    }

    internal fun release() {
        activeConsumerCount.intValue = (activeConsumerCount.intValue - 1).coerceAtLeast(0)
    }

    internal fun advance(deltaSeconds: Float) {
        _elapsedSeconds.floatValue = (_elapsedSeconds.floatValue + deltaSeconds) % SHARED_CLOCK_CYCLE_SEC
    }
}

val LocalAnimationClock = compositionLocalOf { SharedAnimationClock() }

@Composable
fun rememberSharedAnimationTime(enabled: Boolean = true): FloatState {
    val clock = LocalAnimationClock.current
    DisposableEffect(clock, enabled) {
        if (enabled) {
            clock.acquire()
            onDispose { clock.release() }
        } else {
            onDispose {}
        }
    }
    return clock.elapsedSeconds
}

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
fun SharedAnimationClockDriver(
    clock: SharedAnimationClock,
    windowActivity: WindowActivityState = LocalWindowActivity.current,
) {
    val hasConsumers = clock.hasConsumers
    LaunchedEffect(clock, windowActivity, hasConsumers) {
        if (!hasConsumers || windowActivity == WindowActivityState.MINIMIZED) return@LaunchedEffect
        val delayMs =
            if (windowActivity == WindowActivityState.ACTIVE) {
                ACTIVE_TICK_DELAY_MS
            } else {
                UNFOCUSED_TICK_DELAY_MS
            }
        val stepSec =
            if (windowActivity == WindowActivityState.ACTIVE) {
                ACTIVE_TICK_STEP_SEC
            } else {
                UNFOCUSED_TICK_STEP_SEC
            }
        while (true) {
            delay(delayMs.milliseconds)
            clock.advance(stepSec)
        }
    }
}
