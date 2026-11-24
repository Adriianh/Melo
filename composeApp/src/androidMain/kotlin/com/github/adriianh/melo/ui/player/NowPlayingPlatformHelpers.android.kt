@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player

import android.provider.Settings
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlin.coroutines.cancellation.CancellationException

@Composable
internal actual fun PlayerPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float?) -> Unit,
    onBack: () -> Unit,
) {
    PredictiveBackHandler(enabled = enabled) { progress ->
        try {
            progress.collect { backEvent ->
                onProgress(backEvent.progress.coerceIn(0f, 1f))
            }
            onProgress(null)
            onBack()
        } catch (_: CancellationException) {
            onProgress(null)
        }
    }
}

@Composable
internal actual fun rememberPrefersReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val scale =
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                )
            scale == 0f
        }.getOrDefault(false)
    }
}

internal actual fun Modifier.idleCursorHidden(hidden: Boolean): Modifier = this
