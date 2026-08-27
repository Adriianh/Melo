package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerUiState

@Composable
fun PlayerSlider(
    state: PlayerUiState,
    activeAccent: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = state.progressFraction,
            onValueChange = { fraction ->
                if (state.durationMs > 0) {
                    onSeekTo((fraction * state.durationMs).toLong())
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = activeAccent,
                activeTrackColor = activeAccent,
                inactiveTrackColor = MeloColors.borderStrong
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = state.elapsedLabel,
                style = MeloType.labelSmall,
                color = MeloColors.textMuted
            )
            Text(
                text = state.remainingLabel,
                style = MeloType.labelSmall,
                color = MeloColors.textMuted
            )
        }
    }
}