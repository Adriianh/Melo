package com.github.adriianh.melo.util

import com.github.adriianh.core.platform.currentTimeSeconds

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

/**
 * Formats an epoch timestamp in milliseconds to a localized relative time string (e.g. "Hace 15 min").
 */
fun formatRelativeTime(timestampMs: Long): String {
    if (timestampMs <= 0) return ""
    val now = currentTimeSeconds() * 1000L
    val diffMs = (now - timestampMs).coerceAtLeast(0)
    val diffSec = diffMs / 1000
    val diffMin = diffSec / 60
    val diffHours = diffMin / 60
    val diffDays = diffHours / 24

    return when {
        diffMin < 1 -> "Hace un momento"
        diffMin < 60 -> "Hace $diffMin min"
        diffHours < 24 -> if (diffHours == 1L) "Hace 1 h" else "Hace $diffHours h"
        diffDays == 1L -> "Ayer"
        diffDays < 7 -> "Hace $diffDays d"
        diffDays < 30 -> "Hace ${diffDays / 7} sem"
        else -> "Hace ${diffDays / 30} m"
    }
}