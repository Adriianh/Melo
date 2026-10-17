package com.github.adriianh.melo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val headerModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 12.dp)

    if (action == null) {
        Text(
            text = title,
            style = MeloType.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MeloColors.textPrimary,
            modifier = headerModifier,
        )
    } else {
        Row(
            modifier = headerModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MeloType.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MeloColors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            action()
        }
    }
}
