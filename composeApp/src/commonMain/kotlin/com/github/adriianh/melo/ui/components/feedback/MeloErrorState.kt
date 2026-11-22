package com.github.adriianh.melo.ui.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.common_action_retry
import com.github.adriianh.melo.resources.common_error_unknown
import com.github.adriianh.melo.resources.common_error_with_detail
import com.github.adriianh.melo.util.MeloColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun MeloErrorState(
    error: String?,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val detail = error ?: stringResource(Res.string.common_error_unknown)
            Text(
                text = stringResource(Res.string.common_error_with_detail, detail),
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MeloColors.surface2,
                        contentColor = MeloColors.textPrimary,
                    ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(stringResource(Res.string.common_action_retry))
            }
        }
    }
}
