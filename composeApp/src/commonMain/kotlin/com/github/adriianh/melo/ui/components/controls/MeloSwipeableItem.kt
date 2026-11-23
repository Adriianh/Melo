@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.controls

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val TrailingButtonWidth = 76.dp
private val LeadingThreshold = 56.dp
private val LeadingMaxDrag = 80.dp
private val TrailingCommitExtra = 8.dp
private val TrailingMaxExtra = 8.dp
private const val TRAILING_SNAP_FRACTION = 0.5f
private const val MAX_TRAILING_ACTIONS = 2

@Composable
fun MeloSwipeableItem(
    onSwipeRight: () -> Unit,
    trailingActions: List<Pair<SwipeAction, () -> Unit>> = emptyList(),
    modifier: Modifier = Modifier,
    leadingAction: SwipeAction = MeloSwipeActions.like(),
    state: SwipeRevealState = rememberSwipeRevealState(initialOffsetDp = 0f),
    enabled: Boolean = true,
    cornerRadius: Int = 8,
    initialOffsetDp: Float = 0f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val coroutineScope = rememberCoroutineScope()

    val effectiveTrailing = trailingActions.take(MAX_TRAILING_ACTIONS)
    val maxLeadingPx = with(density) { LeadingMaxDrag.toPx() }
    val thresholdPx = with(density) { LeadingThreshold.toPx() }
    val maxRevealPx = with(density) { TrailingButtonWidth.toPx() } * effectiveTrailing.size
    val trailingCommitPx = maxRevealPx + with(density) { TrailingCommitExtra.toPx() }
    val trailingMaxDragPx = trailingCommitPx + with(density) { TrailingMaxExtra.toPx() }
    val resolvedState = resolveSwipeState(state, initialOffsetDp, coroutineScope, maxRevealPx)
    val hasVibrated = remember { mutableStateOf(false) }

    SwipeRevealBackHandler(enabled = resolvedState.isOpen, onBack = { resolvedState.close() })

    val logicalOffset = resolvedState.offsetX.value
    val isLeadingCommitted = logicalOffset >= thresholdPx
    val isTrailingCommitted = effectiveTrailing.isNotEmpty() && -logicalOffset >= trailingCommitPx
    val leadingBgColor by animateColorAsState(
        targetValue = if (isLeadingCommitted) leadingAction.commitColor else leadingAction.containerColor,
        label = "leadingBg",
    )
    val leadingContentColor by animateColorAsState(
        targetValue = if (isLeadingCommitted) leadingAction.onCommitColor else leadingAction.contentColor,
        label = "leadingContent",
    )
    val physicalOffsetPx = if (isRtl) -logicalOffset else logicalOffset

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius.dp))
                .nestedScroll(rememberSwipeNestedScrollConnection(resolvedState))
                .semantics {
                    if (enabled) {
                        customActions =
                            buildSwipeCustomActions(
                                leadingAction,
                                effectiveTrailing,
                                resolvedState,
                                onSwipeRight,
                            )
                    }
                }.swipeItemGestures(
                    enabled = enabled,
                    isRtl = isRtl,
                    maxRevealPx = maxRevealPx,
                    trailingCommitPx = if (effectiveTrailing.isEmpty()) 0f else trailingCommitPx,
                    trailingMaxDragPx = if (effectiveTrailing.isEmpty()) 0f else trailingMaxDragPx,
                    maxLeadingPx = maxLeadingPx,
                    thresholdPx = thresholdPx,
                    state = resolvedState,
                    coroutineScope = coroutineScope,
                    hasVibrated = hasVibrated,
                    haptic = haptic,
                    onSwipeRight = onSwipeRight,
                    onTrailingCommit = { effectiveTrailing.lastOrNull()?.second?.invoke() },
                ),
    ) {
        SwipeBackgroundLayer(
            logicalOffset = logicalOffset,
            revealWidthDp = with(density) { abs(logicalOffset).toDp() },
            leadingAction = leadingAction,
            leadingBgColor = leadingBgColor,
            leadingContentColor = leadingContentColor,
            trailingActions = effectiveTrailing,
            isTrailingCommitted = isTrailingCommitted,
            onTrailingClick = { callback ->
                resolvedState.close()
                callback()
            },
        )

        SwipeForegroundContent(
            physicalOffsetPx = physicalOffsetPx,
            isOpen = resolvedState.isOpen,
            onClose = { resolvedState.close() },
            content = content,
        )
    }
}

@Composable
private fun SwipeForegroundContent(
    physicalOffsetPx: Float,
    isOpen: Boolean,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .offset { IntOffset(physicalOffsetPx.roundToInt(), 0) }
                .fillMaxWidth(),
    ) {
        content()
        if (isOpen) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onClose,
                        ),
            )
        }
    }
}

@Composable
private fun BoxScope.SwipeBackgroundLayer(
    logicalOffset: Float,
    revealWidthDp: Dp,
    leadingAction: SwipeAction,
    leadingBgColor: Color,
    leadingContentColor: Color,
    trailingActions: List<Pair<SwipeAction, () -> Unit>>,
    isTrailingCommitted: Boolean,
    onTrailingClick: (() -> Unit) -> Unit,
) {
    if (logicalOffset > 0f) {
        LeadingSwipeBackground(
            revealWidthDp = revealWidthDp,
            action = leadingAction,
            bgColor = leadingBgColor,
            contentColor = leadingContentColor,
        )
    } else if (logicalOffset < 0f && trailingActions.isNotEmpty()) {
        TrailingSwipeActions(
            revealWidthDp = revealWidthDp,
            actions = trailingActions,
            isTrailingCommitted = isTrailingCommitted,
            onActionClick = onTrailingClick,
        )
    }
}

private fun buildSwipeCustomActions(
    leadingAction: SwipeAction,
    trailingActions: List<Pair<SwipeAction, () -> Unit>>,
    state: SwipeRevealState,
    onSwipeRight: () -> Unit,
): List<CustomAccessibilityAction> =
    buildList {
        add(
            CustomAccessibilityAction(leadingAction.label) {
                onSwipeRight()
                true
            },
        )
        trailingActions.forEach { (action, callback) ->
            add(
                CustomAccessibilityAction(action.label) {
                    state.close()
                    callback()
                    true
                },
            )
        }
    }

private fun Modifier.swipeItemGestures(
    enabled: Boolean,
    isRtl: Boolean,
    maxRevealPx: Float,
    trailingCommitPx: Float,
    trailingMaxDragPx: Float,
    maxLeadingPx: Float,
    thresholdPx: Float,
    state: SwipeRevealState,
    coroutineScope: CoroutineScope,
    hasVibrated: MutableState<Boolean>,
    haptic: HapticFeedback,
    onSwipeRight: () -> Unit,
    onTrailingCommit: () -> Unit,
): Modifier =
    pointerInput(enabled, isRtl, maxRevealPx, trailingCommitPx, trailingMaxDragPx, maxLeadingPx, thresholdPx) {
        if (!enabled) return@pointerInput
        detectHorizontalDragGestures(
            onDragStart = {
                state.group?.registerOpen(state)
            },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                val logicalDelta = if (isRtl) -dragAmount else dragAmount
                coroutineScope.launch {
                    val newOffset = (state.offsetX.value + logicalDelta).coerceIn(-trailingMaxDragPx, maxLeadingPx)
                    state.offsetX.snapTo(newOffset)
                    val pastCommit =
                        newOffset >= thresholdPx || (trailingCommitPx > 0f && -newOffset >= trailingCommitPx)
                    if (pastCommit && !hasVibrated.value) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        hasVibrated.value = true
                    } else if (!pastCommit && hasVibrated.value) {
                        hasVibrated.value = false
                    }
                }
            },
            onDragEnd = {
                coroutineScope.launch {
                    hasVibrated.value = false
                    settleSwipeOffset(state, thresholdPx, maxRevealPx, trailingCommitPx, onSwipeRight, onTrailingCommit)
                }
            },
            onDragCancel = {
                coroutineScope.launch {
                    hasVibrated.value = false
                    val shouldOpen = state.offsetX.value <= -maxRevealPx * TRAILING_SNAP_FRACTION && maxRevealPx > 0f
                    if (shouldOpen) state.open() else state.close()
                }
            },
        )
    }

private suspend fun settleSwipeOffset(
    state: SwipeRevealState,
    thresholdPx: Float,
    maxRevealPx: Float,
    trailingCommitPx: Float,
    onSwipeRight: () -> Unit,
    onTrailingCommit: () -> Unit,
) {
    val current = state.offsetX.value
    when {
        current >= thresholdPx -> {
            onSwipeRight()
            resetSwipeToIdle(state)
        }

        trailingCommitPx > 0f && -current >= trailingCommitPx -> {
            onTrailingCommit()
            resetSwipeToIdle(state)
        }

        current <= -maxRevealPx * TRAILING_SNAP_FRACTION && maxRevealPx > 0f -> state.open()
        else -> state.close()
    }
}

private suspend fun resetSwipeToIdle(state: SwipeRevealState) {
    state.isOpen = false
    state.group?.clearIfCurrent(state)
    state.offsetX.animateTo(
        targetValue = 0f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
    )
}

@Composable
private fun BoxScope.LeadingSwipeBackground(
    revealWidthDp: Dp,
    action: SwipeAction,
    bgColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier.matchParentSize(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .width(revealWidthDp)
                    .fillMaxHeight()
                    .background(bgColor)
                    .clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
                if (revealWidthDp >= 44.dp) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.TrailingSwipeActions(
    revealWidthDp: Dp,
    actions: List<Pair<SwipeAction, () -> Unit>>,
    isTrailingCommitted: Boolean,
    onActionClick: (() -> Unit) -> Unit,
) {
    val outerAction = actions.last().first
    val outerCommitBg by animateColorAsState(
        targetValue = if (isTrailingCommitted) outerAction.commitColor else outerAction.containerColor,
        label = "trailingCommitBg",
    )
    Box(
        modifier = Modifier.matchParentSize(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            modifier =
                Modifier
                    .width(revealWidthDp)
                    .fillMaxHeight()
                    .background(outerCommitBg)
                    .clipToBounds(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEachIndexed { index, (action, callback) ->
                val isOuterCommitted = isTrailingCommitted && index == actions.lastIndex
                SwipeTrailingActionButton(
                    action = action,
                    isCommitted = isOuterCommitted,
                    onClick = { onActionClick(callback) },
                )
            }
        }
    }
}

@Composable
private fun SwipeTrailingActionButton(
    action: SwipeAction,
    isCommitted: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isCommitted) action.commitColor else action.containerColor,
        label = "trailingBtnBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isCommitted) action.onCommitColor else action.contentColor,
        label = "trailingBtnContent",
    )
    Box(
        modifier =
            Modifier
                .width(TrailingButtonWidth)
                .fillMaxHeight()
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .background(bgColor)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ).semantics {
                    contentDescription = action.label
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
