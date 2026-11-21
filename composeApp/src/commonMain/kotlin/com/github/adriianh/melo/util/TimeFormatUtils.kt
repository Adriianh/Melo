package com.github.adriianh.melo.util

/**
 * Formats time (e.g. elapsed or remaining playback time) in milliseconds to mm:ss or hh:mm:ss string.
 * For zero or negative values, returns "0:00".
 */
fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

/**
 * Formats duration in milliseconds to a human-readable mm:ss or hh:mm:ss string.
 * Returns an empty string for zero or negative durations.
 */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return ""
    return formatTime(durationMs)
}

/**
 * Formats collection (album, playlist) duration in milliseconds to "X h Y min" or "X min".
 * Returns an empty string for zero or negative durations.
 */
fun formatCollectionDuration(durationMs: Long): String {
    if (durationMs <= 0) return ""
    val minutes = (durationMs / 1000) / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) "$hours h $remainingMinutes min" else "$minutes min"
}
