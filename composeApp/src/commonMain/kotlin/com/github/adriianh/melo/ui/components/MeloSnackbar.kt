package com.github.adriianh.melo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.LocalMeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.getPlatform
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class MeloSnackbarState {
    var isVisible = mutableStateOf(false)
        private set
    var message = mutableStateOf("")
        private set
    var icon = mutableStateOf<ImageVector?>(null)
        private set

    var actionLabel = mutableStateOf<String?>(null)
        private set
    var actionColor = mutableStateOf<androidx.compose.ui.graphics.Color?>(null)
        private set
    var onActionClick = mutableStateOf<(() -> Unit)?>(null)
        private set

    var showCount = mutableStateOf(0)
        private set

    fun show(
        msg: String,
        vector: ImageVector? = null,
        action: String? = null,
        actionColor: androidx.compose.ui.graphics.Color? = null,
        onAction: (() -> Unit)? = null
    ) {
        message.value = msg
        icon.value = vector
        actionLabel.value = action
        this.actionColor.value = actionColor
        onActionClick.value = onAction
        isVisible.value = true
        showCount.value++
    }

    fun dismiss() {
        isVisible.value = false
    }
}

val LocalMeloSnackbar = compositionLocalOf<MeloSnackbarState> { error("No Snackbar provided") }

@Composable
fun MeloSnackbarHost(
    state: MeloSnackbarState,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp? = null
) {
    val platform = remember { getPlatform() }

    LaunchedEffect(state.showCount.value, state.isVisible.value) {
        if (state.isVisible.value) {
            delay(3500.milliseconds)
            state.dismiss()
        }
    }

    AnimatedVisibility(
        visible = state.isVisible.value,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        val colors = LocalMeloColors.current
        val actualBottomPadding =
            bottomPadding ?: if (platform.type == PlatformType.DESKTOP) 120.dp else 180.dp

        Row(
            modifier = Modifier
                .padding(bottom = actualBottomPadding, start = 24.dp, end = 24.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(colors.surface2.copy(alpha = 0.95f))
                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val vector = state.icon.value
            if (vector != null) {
                Icon(
                    imageVector = vector,
                    contentDescription = null,
                    tint = colors.textPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = state.message.value,
                style = MeloType.labelMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f, fill = false)
            )

            val actionLabel = state.actionLabel.value
            val onActionClick = state.onActionClick.value
            val actionColor = state.actionColor.value ?: colors.brandAccent
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = actionLabel,
                    style = MeloType.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = actionColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onActionClick()
                            state.dismiss()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}