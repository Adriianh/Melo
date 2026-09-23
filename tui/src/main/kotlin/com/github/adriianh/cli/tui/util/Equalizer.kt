package com.github.adriianh.cli.tui.util

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pure logic for the state-driven player bar equalizer.
 *
 * The TUI plays audio through an `ffplay` subprocess that writes directly to the
 * sound device, so the app never sees the PCM/amplitude signal. Instead of real
 * audio, these bars are derived deterministically from live playback *state*
 * signals: [progress], [volume] and a monotonic animation [tick]. Keeping this
 * logic free of UI concerns makes it unit-testable in isolation.
 */

/** Block characters used to render the 8 bar levels, indexed by height 0..7. */
const val EQUALIZER_LEVEL_CHARS: String = "▁▂▃▄▅▆▇█"

/** Number of vertical bars rendered in the player bar equalizer slot. */
const val EQUALIZER_COLUMNS: Int = 4

/** Cadence (ms) at which the animation tick advances while music is playing. */
const val EQUALIZER_TICK_MS: Long = 150L

/** Highest level a bar can reach (index into [EQUALIZER_LEVEL_CHARS]). */
const val EQUALIZER_MAX_LEVEL: Int = EQUALIZER_LEVEL_CHARS.length - 1

private val COLUMN_PHASES = doubleArrayOf(0.0, 2.1, 3.7, 5.3)
private val COLUMN_SLOW_SPEED = doubleArrayOf(0.20, 0.31, 0.17, 0.43)
private val COLUMN_FAST_SPEED = doubleArrayOf(0.47, 0.35, 0.62, 0.29)

/**
 * Energy envelope over the track: quieter at the edges and loudest around the
 * middle, so the equalizer "breathes" along the song's arc rather than staying
 * static. Ranges from 0.45 (start/end) to 1.0 (mid-track).
 */
private fun energyEnvelope(progress: Float): Double {
    val p = progress.coerceIn(0f, 1f)
    return 0.45 + 0.55 * sin(p * PI)
}

/**
 * Computes the height (0..7) of each bar from live playback signals.
 *
 * Deterministic: identical inputs always produce identical bars, so rendering is
 * stable across re-renders. Each column oscillates on its own slow + fast sine
 * pair with a distinct phase, so bars move independently and never repeat in a
 * rigid loop.
 *
 * @param progress current track progress in [0,1]; shapes the energy envelope
 * @param volume   player volume in [0,100]; a muted player renders a flat line
 * @param tick     monotonic animation counter advanced every [EQUALIZER_TICK_MS] ms
 */
fun equalizerHeights(progress: Float, volume: Int, tick: Long): List<Int> {
    val volumeF = volume.coerceIn(0, 100) / 100f
    val amplitude = (0.15 + 0.85 * volumeF) * energyEnvelope(progress)
    val t = tick.toDouble()
    return (0 until EQUALIZER_COLUMNS).map { column ->
        val slow = 0.5 + 0.5 * sin(t * COLUMN_SLOW_SPEED[column] + COLUMN_PHASES[column])
        val fast = 0.5 + 0.5 * sin(t * COLUMN_FAST_SPEED[column] + COLUMN_PHASES[column] * 1.7)
        val combined = (slow + fast) / 2.0
        (combined * EQUALIZER_MAX_LEVEL * amplitude).roundToInt().coerceIn(0, EQUALIZER_MAX_LEVEL)
    }
}

/**
 * Renders the current bar heights as a fixed-width string of block characters,
 * ready to be placed in the player bar's [EQUALIZER_COLUMNS]-column slot.
 */
fun equalizerFrame(progress: Float, volume: Int, tick: Long): String =
    equalizerHeights(progress, volume, tick)
        .joinToString("") { EQUALIZER_LEVEL_CHARS[it].toString() }