package com.github.adriianh.melo.ui.player.components

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
import com.github.adriianh.melo.ui.components.MeloSlider
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerProgressState
import com.github.adriianh.melo.util.PlayerUiState
import com.github.adriianh.melo.util.formatTime
import kotlinx.coroutines.flow.StateFlow

@Composable
fun PlayerSlider(
    state: PlayerUiState,
    activeAccent: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    progressState: StateFlow<PlayerProgressState>? = null,
) {
    val liveProgress = progressState?.collectAsState()?.value
    val progressFraction = liveProgress?.progressFraction ?: state.progressFraction
    val durationMs = liveProgress?.durationMs ?: state.durationMs
    val elapsedLabel = liveProgress?.elapsedLabel ?: state.elapsedLabel
    val remainingLabel = liveProgress?.remainingLabel ?: state.remainingLabel

    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableStateOf(0f) }

    val displayFraction = if (isSeeking) seekFraction else progressFraction

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
            inactiveColor = MeloColors.borderStrong,
            thumbColor = Color.White,
            trackHeight = 4.dp,
            hoverTrackHeight = 6.dp,
            thumbRadius = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text =
                    if (isSeeking && durationMs > 0) {
                        val seekMs = (seekFraction * durationMs).toLong()
                        formatTime(seekMs)
                    } else {
                        elapsedLabel
                    },
                style = MeloType.labelSmall,
                color = MeloColors.textMuted,
            )
            Text(
                text =
                    if (isSeeking && durationMs > 0) {
                        val remainingMs =
                            ((1f - seekFraction) * durationMs).toLong().coerceAtLeast(0L)
                        formatTime(remainingMs)
                    } else {
                        remainingLabel
                    },
                style = MeloType.labelSmall,
                color = MeloColors.textMuted,
            )
        }
    }
}