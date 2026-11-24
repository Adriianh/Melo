@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_close_queue
import com.github.adriianh.melo.resources.player_sheet_queue_title
import com.github.adriianh.melo.util.MeloType
import org.jetbrains.compose.resources.stringResource

private val QUEUE_PANEL_WIDTH = 460.dp
private val QUEUE_PANEL_RADIUS = 24.dp

@Composable
internal fun FullscreenQueueSidePanel(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    queueContent: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(QUEUE_PANEL_RADIUS)
    Column(
        modifier =
            modifier
                .width(QUEUE_PANEL_WIDTH)
                .fillMaxHeight()
                .clip(shape)
                .background(Color.Black.copy(alpha = 0.62f))
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.12f), shape = shape)
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.player_sheet_queue_title),
                style = MeloType.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.a11y_close_queue),
                    tint = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            queueContent()
        }
    }
}
