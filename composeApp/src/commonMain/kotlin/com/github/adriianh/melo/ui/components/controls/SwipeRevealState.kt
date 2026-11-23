@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.controls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.swipe_action_like
import com.github.adriianh.melo.resources.swipe_action_queue
import com.github.adriianh.melo.resources.swipe_action_remove
import com.github.adriianh.melo.resources.swipe_action_unlike
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Immutable
data class SwipeAction(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val commitColor: Color,
    val onCommitColor: Color = Color.White,
)

object MeloSwipeActions {
    private val LikeContainer = Color(0xFF4A2236)
    private val LikeContent = Color(0xFFFFA8C9)
    private val LikeCommit = Color(0xFFD6336C)

    private val QueueContainer = Color(0xFF1F3A5A)
    private val QueueContent = Color(0xFF9FC9F5)
    private val QueueCommit = Color(0xFF2B6CB0)

    private val RemoveContainer = Color(0xFF55231F)
    private val RemoveContent = Color(0xFFFFB4A8)
    private val RemoveCommit = Color(0xFFC0392B)

    @Composable
    fun like(isLiked: Boolean = false): SwipeAction =
        SwipeAction(
            icon = if (isLiked) Icons.Default.HeartBroken else Icons.Default.Favorite,
            label =
                stringResource(
                    if (isLiked) Res.string.swipe_action_unlike else Res.string.swipe_action_like,
                ),
            containerColor = LikeContainer,
            contentColor = LikeContent,
            commitColor = LikeCommit,
        )

    @Composable
    fun queue(): SwipeAction =
        SwipeAction(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            label = stringResource(Res.string.swipe_action_queue),
            containerColor = QueueContainer,
            contentColor = QueueContent,
            commitColor = QueueCommit,
        )

    @Composable
    fun remove(): SwipeAction =
        SwipeAction(
            icon = Icons.Default.Delete,
            label = stringResource(Res.string.swipe_action_remove),
            containerColor = RemoveContainer,
            contentColor = RemoveContent,
            commitColor = RemoveCommit,
        )
}

@Stable
class SwipeRevealGroup {
    private var openState: SwipeRevealState? by mutableStateOf(null)

    fun registerOpen(state: SwipeRevealState) {
        val current = openState
        if (current != null && current !== state) {
            current.close()
        }
        openState = state
    }

    fun clearIfCurrent(state: SwipeRevealState) {
        if (openState === state) {
            openState = null
        }
    }

    fun closeAll() {
        openState?.close()
        openState = null
    }
}

val LocalSwipeRevealGroup = compositionLocalOf<SwipeRevealGroup?> { null }

@Composable
fun rememberSwipeRevealGroup(): SwipeRevealGroup = remember { SwipeRevealGroup() }

@Stable
class SwipeRevealState internal constructor(
    initialOffsetPx: Float,
    internal val group: SwipeRevealGroup?,
) {
    internal val offsetX = Animatable(initialOffsetPx)
    internal var scope: CoroutineScope? = null
    internal var maxRevealPx: Float = 0f

    var isOpen by mutableStateOf(initialOffsetPx < -1f)
        internal set

    fun open() {
        val target = -maxRevealPx
        if (target >= 0f) return
        group?.registerOpen(this)
        isOpen = true
        scope?.launch {
            offsetX.animateTo(
                targetValue = target,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
            )
        }
    }

    fun close() {
        isOpen = false
        group?.clearIfCurrent(this)
        scope?.launch {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
            )
        }
    }
}

@Composable
fun rememberSwipeRevealState(
    initialOffsetDp: Float = 0f,
    group: SwipeRevealGroup? = LocalSwipeRevealGroup.current,
): SwipeRevealState {
    val initialOffsetPx = with(LocalDensity.current) { initialOffsetDp.dp.toPx() }
    return remember(initialOffsetPx, group) {
        SwipeRevealState(initialOffsetPx = initialOffsetPx, group = group)
    }
}
