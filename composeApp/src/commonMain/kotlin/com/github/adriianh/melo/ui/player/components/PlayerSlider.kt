package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_playback_progress
import com.github.adriianh.melo.ui.components.controls.MeloSlider
import com.github.adriianh.melo.util.LocalWindowActivity
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerProgressState
import com.github.adriianh.melo.util.PlayerUiState
import com.github.adriianh.melo.util.formatTime
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

@Composable
private fun rememberLiveProgress(
    state: PlayerUiState,
    progressState: StateFlow<PlayerProgressState>?,
): PlayerProgressState {
    val isWindowVisible = LocalWindowActivity.current.isVisible
    val liveProgress =
        if (progressState != null) {
            if (isWindowVisible) progressState.collectAsState().value else progressState.value
        } else {
            null
        }
    return liveProgress
        ?: PlayerProgressState(
            progressFraction = state.progressFraction,
            progressMs = state.progressMs,
            durationMs = state.durationMs,
            elapsedLabel = state.elapsedLabel,
            remainingLabel = state.remainingLabel,
        )
}

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
fun PlayerSlider(
    state: PlayerUiState,
    activeAccent: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    progressState: StateFlow<PlayerProgressState>? = null,
    timeLabelColor: Color = MeloColors.textMuted,
    inactiveTrackColor: Color = MeloColors.borderStrong,
    showRemainingTime: Boolean = false,
    onToggleShowRemainingTime: (() -> Unit)? = null,
) {
    val progress = rememberLiveProgress(state = state, progressState = progressState)
    val durationMs = progress.durationMs
    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableStateOf(0f) }

    val displayFraction = if (isSeeking) seekFraction else progress.progressFraction
    val (elapsedText, remainingText) = resolveSliderLabels(isSeeking, seekFraction, durationMs, progress)
    val totalText = if (durationMs > 0) formatTime(durationMs) else progress.remainingLabel
    val rightLabel =
        if (showRemainingTime) {
            if (remainingText == "0:00") remainingText else "-$remainingText"
        } else {
            totalText
        }
    val a11yProgressLabel = stringResource(Res.string.a11y_playback_progress)

    Column(modifier = modifier.fillMaxWidth()) {
        MeloSlider(
            value = displayFraction,
            onValueChange = { fraction ->
                isSeeking = true
                seekFraction = fraction
            },
            onValueChangeFinished = {
                if (durationMs > 0) {
                    onSeekTo((seekFraction * durationMs).toLong())
                }
                isSeeking = false
            },
            activeColor = activeAccent,
            inactiveColor = inactiveTrackColor,
            thumbColor = Color.White,
            trackHeight = 4.dp,
            hoverTrackHeight = 6.dp,
            thumbRadius = 6.dp,
            hitTargetHeight = 28.dp,
            a11yLabel = a11yProgressLabel,
            a11yValueDescription = "$elapsedText / $totalText",
            modifier = Modifier.fillMaxWidth(),
        )

        PlayerSliderTimeLabels(
            elapsedText = elapsedText,
            rightLabel = rightLabel,
            timeLabelColor = timeLabelColor,
            onToggleShowRemainingTime = onToggleShowRemainingTime,
        )
    }
}

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
private fun PlayerSliderTimeLabels(
    elapsedText: String,
    rightLabel: String,
    timeLabelColor: Color,
    onToggleShowRemainingTime: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = elapsedText,
            style = MeloType.labelSmall,
            color = timeLabelColor,
        )
        val rightLabelModifier =
            if (onToggleShowRemainingTime != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleShowRemainingTime,
                )
            } else {
                Modifier
            }
        Text(
            text = rightLabel,
            style = MeloType.labelSmall,
            color = timeLabelColor,
            modifier = rightLabelModifier,
        )
    }
}

private fun resolveSliderLabels(
    isSeeking: Boolean,
    seekFraction: Float,
    durationMs: Long,
    progress: PlayerProgressState,
): Pair<String, String> =
    if (isSeeking && durationMs > 0) {
        val elapsed = formatTime((seekFraction * durationMs).toLong())
        val remaining = formatTime(((1f - seekFraction) * durationMs).toLong().coerceAtLeast(0L))
        elapsed to remaining
    } else {
        progress.elapsedLabel to progress.remainingLabel
    }
