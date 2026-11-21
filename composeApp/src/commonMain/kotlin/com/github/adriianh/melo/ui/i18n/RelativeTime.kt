package com.github.adriianh.melo.ui.i18n

import androidx.compose.runtime.Composable
import com.github.adriianh.core.platform.currentTimeSeconds
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.time_days_ago
import com.github.adriianh.melo.resources.time_hours_ago
import com.github.adriianh.melo.resources.time_just_now
import com.github.adriianh.melo.resources.time_minutes_ago
import com.github.adriianh.melo.resources.time_months_ago
import com.github.adriianh.melo.resources.time_weeks_ago
import com.github.adriianh.melo.resources.time_yesterday
import org.jetbrains.compose.resources.stringResource

/**
 * Formats an epoch timestamp in milliseconds to a localized relative time string.
 */
@Composable
internal fun formatRelativeTime(timestampMs: Long): String {
    if (timestampMs <= 0) return ""
    val now = currentTimeSeconds() * 1000L
    val diffMs = (now - timestampMs).coerceAtLeast(0L)
    val diffSec = diffMs / 1000L
    val diffMin = diffSec / 60L
    val diffHours = diffMin / 60L
    val diffDays = diffHours / 24L

    return when {
        diffMin < 1 -> stringResource(Res.string.time_just_now)
        diffMin < 60 -> stringResource(Res.string.time_minutes_ago, diffMin)
        diffHours < 24 -> stringResource(Res.string.time_hours_ago, diffHours)
        diffDays == 1L -> stringResource(Res.string.time_yesterday)
        diffDays < 7 -> stringResource(Res.string.time_days_ago, diffDays)
        diffDays < 30 -> stringResource(Res.string.time_weeks_ago, diffDays / 7)
        else -> stringResource(Res.string.time_months_ago, diffDays / 30)
    }
}
