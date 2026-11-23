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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
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
private const val TRAILING_SNAP_FRACTION = 0.5f
private const val MAX_TRAILING_ACTIONS = 2

@Composable
fun MeloSwipeableItem(
    modifier: Modifier = Modifier,
    onSwipeRight: () -> Unit,
    trailingActions: List<Pair<SwipeAction, () -> Unit>> = emptyList(),
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
    val resolvedState = resolveSwipeState(state, initialOffsetDp, coroutineScope, maxRevealPx)
    val hasVibrated = remember { mutableStateOf(false) }

    SwipeRevealBackHandler(
        enabled = resolvedState.isOpen,
        onBack = { resolvedState.close() },
    )

    val logicalOffset = resolvedState.offsetX.value
    val isLeadingCommitted = logicalOffset >= thresholdPx
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
                    hasTrailing = effectiveTrailing.isNotEmpty(),
                    maxRevealPx = maxRevealPx,
                    maxLeadingPx = maxLeadingPx,
                    thresholdPx = thresholdPx,
                    state = resolvedState,
                    coroutineScope = coroutineScope,
                    hasVibrated = hasVibrated,
                    haptic = haptic,
                    onSwipeRight = onSwipeRight,
                ),
    ) {
        SwipeBackgroundLayer(
            logicalOffset = logicalOffset,
            revealWidthDp = with(density) { abs(logicalOffset).toDp() },
            leadingAction = leadingAction,
            leadingBgColor = leadingBgColor,
            leadingContentColor = leadingContentColor,
            trailingActions = effectiveTrailing,
            onTrailingClick = { callback ->
                resolvedState.close()
                callback()
            },
        )

        Box(
            modifier =
                Modifier
                    .offset { IntOffset(physicalOffsetPx.roundToInt(), 0) }
                    .fillMaxWidth(),
        ) {
            content()
            if (resolvedState.isOpen) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = { resolvedState.close() },
                            ),
                )
            }
        }
    }
}

@Composable
private fun resolveSwipeState(
    state: SwipeRevealState,
    initialOffsetDp: Float,
    coroutineScope: CoroutineScope,
    maxRevealPx: Float,
): SwipeRevealState {
    val initialOffsetPx = with(LocalDensity.current) { initialOffsetDp.dp.toPx() }
    val resolved =
        if (initialOffsetDp != 0f && state.offsetX.value == 0f && !state.isOpen) {
            remember(initialOffsetPx, state.group) {
                SwipeRevealState(initialOffsetPx = initialOffsetPx, group = state.group)
            }
        } else {
            state
        }
    resolved.scope = coroutineScope
    resolved.maxRevealPx = maxRevealPx
    return resolved
}

@Composable
private fun rememberSwipeNestedScrollConnection(state: SwipeRevealState): NestedScrollConnection =
    remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (state.isOpen && abs(available.y) > 0.5f) {
                    state.close()
                }
                return Offset.Zero
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
    hasTrailing: Boolean,
    maxRevealPx: Float,
    maxLeadingPx: Float,
    thresholdPx: Float,
    state: SwipeRevealState,
    coroutineScope: CoroutineScope,
    hasVibrated: MutableState<Boolean>,
    haptic: HapticFeedback,
    onSwipeRight: () -> Unit,
): Modifier =
    pointerInput(enabled, isRtl, maxRevealPx, maxLeadingPx, thresholdPx) {
        if (!enabled) return@pointerInput
        detectHorizontalDragGestures(
            onDragStart = {
                state.group?.registerOpen(state)
            },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                val logicalDelta = if (isRtl) -dragAmount else dragAmount
                coroutineScope.launch {
                    val minOffset = if (hasTrailing) -maxRevealPx else 0f
                    val newOffset = (state.offsetX.value + logicalDelta).coerceIn(minOffset, maxLeadingPx)
                    state.offsetX.snapTo(newOffset)
                    if (newOffset >= thresholdPx && !hasVibrated.value) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        hasVibrated.value = true
                    } else if (newOffset < thresholdPx && hasVibrated.value) {
                        hasVibrated.value = false
                    }
                }
            },
            onDragEnd = {
                coroutineScope.launch {
                    hasVibrated.value = false
                    settleSwipeOffset(state, thresholdPx, maxRevealPx, onSwipeRight)
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
    onSwipeRight: () -> Unit,
) {
    val current = state.offsetX.value
    when {
        current >= thresholdPx -> {
            onSwipeRight()
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

        current <= -maxRevealPx * TRAILING_SNAP_FRACTION && maxRevealPx > 0f -> state.open()
        else -> state.close()
    }
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
    onActionClick: (() -> Unit) -> Unit,
) {
    Box(
        modifier = Modifier.matchParentSize(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            modifier =
                Modifier
                    .width(revealWidthDp)
                    .fillMaxHeight()
                    .clipToBounds(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { (action, callback) ->
                SwipeTrailingActionButton(
                    action = action,
                    onClick = { onActionClick(callback) },
                )
            }
        }
    }
}

@Composable
private fun SwipeTrailingActionButton(
    action: SwipeAction,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .width(TrailingButtonWidth)
                .fillMaxHeight()
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .background(action.containerColor)
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
                tint = action.contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelSmall,
                color = action.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal expect fun SwipeRevealBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
