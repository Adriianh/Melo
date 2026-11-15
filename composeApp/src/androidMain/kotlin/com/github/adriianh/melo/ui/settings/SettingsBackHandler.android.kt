@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.settings

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import kotlin.coroutines.cancellation.CancellationException

@Composable
internal actual fun SettingsPredictiveBackHandler(
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
