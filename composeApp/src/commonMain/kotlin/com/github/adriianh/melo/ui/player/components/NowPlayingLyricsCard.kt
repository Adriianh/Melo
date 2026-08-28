package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.SyncedLine
import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun LiveLyricsPreviewCard(
    trackLyrics: TrackLyrics?,
    activeLyricIndex: Int,
    showTranslation: Boolean,
    activeAccent: Color,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (trackLyrics == null || (!trackLyrics.hasSync && trackLyrics.plainLyrics.isNullOrBlank())) {
        return
    }

    val currentLine =
        if (trackLyrics.hasSync && activeLyricIndex in trackLyrics.syncedLyrics.indices) {
            trackLyrics.syncedLyrics[activeLyricIndex]
        } else null

    val nextLine =
        if (trackLyrics.hasSync && activeLyricIndex + 1 in trackLyrics.syncedLyrics.indices) {
            trackLyrics.syncedLyrics[activeLyricIndex + 1]
        } else null

    val cardShape = RoundedCornerShape(18.dp)
    val glassBg = Brush.verticalGradient(
        colors = listOf(
            activeAccent.copy(alpha = 0.18f),
            MeloColors.glassFill.copy(alpha = 0.50f)
        )
    )
    val glassBorder = Brush.horizontalGradient(
        colors = listOf(
            activeAccent.copy(alpha = 0.40f),
            MeloColors.glassBorder.copy(alpha = 0.25f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(122.dp)
            .clip(cardShape)
            .background(glassBg)
            .border(width = 1.dp, brush = glassBorder, shape = cardShape)
            .clickable { onExpand() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = activeAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LETRA",
                        style = MeloType.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = activeAccent
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "EXPANDIR",
                        style = MeloType.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = MeloColors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Expandir letra",
                        tint = MeloColors.textSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (trackLyrics.hasSync) {
                    AnimatedContent(
                        targetState = activeLyricIndex to currentLine,
                        transitionSpec = {
                            (slideInVertically(
                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                initialOffsetY = { it / 2 }
                            ) + fadeIn(tween(280))).togetherWith(
                                slideOutVertically(
                                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                                    targetOffsetY = { -it / 2 }
                                ) + fadeOut(tween(200))
                            )
                        },
                        label = "LiveLyricsTransition"
                    ) { (_, line) ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (line != null && line.text.isNotBlank()) {
                                Text(
                                    text = line.text,
                                    style = MeloType.titleMedium.copy(
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 21.sp
                                    ),
                                    color = MeloColors.textPrimary,
                                    maxLines = if (showTranslation && !line.translation.isNullOrBlank()) 1 else 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val translation = line.translation
                                if (showTranslation && !translation.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = translation,
                                        style = MeloType.body.copy(
                                            fontStyle = FontStyle.Italic,
                                            fontSize = 13.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = activeAccent.copy(alpha = 0.90f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (nextLine != null && (!showTranslation || translation.isNullOrBlank())) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = nextLine.text.ifBlank { "♪ ♫ ♪" },
                                        style = MeloType.body.copy(
                                            fontSize = 13.5.sp,
                                            lineHeight = 17.sp
                                        ),
                                        color = MeloColors.textMuted.copy(alpha = 0.55f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                DancingMelodyIndicator(
                                    isActive = true,
                                    tint = activeAccent,
                                    baseFontSize = 17.sp
                                )
                            }
                        }
                    }
                } else {
                    val plain = trackLyrics.plainLyrics
                    if (!plain.isNullOrBlank()) {
                        val previewSnippet =
                            plain.lines().filter { it.isNotBlank() }.take(2).joinToString("\n")
                        Text(
                            text = previewSnippet,
                            style = MeloType.body.copy(
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp
                            ),
                            color = MeloColors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NowPlayingLyricsCard(
    trackLyrics: TrackLyrics?,
    activeLyricIndex: Int,
    isLoading: Boolean,
    showTranslation: Boolean,
    isTranslating: Boolean,
    targetLanguage: String,
    activeAccent: Color,
    onSeekTo: (Long) -> Unit,
    onToggleTranslation: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onToggleArtwork: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(24.dp)
    val glassFillBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            MeloColors.glassSurface.copy(alpha = 0.82f),
            Color.Black.copy(alpha = 0.65f)
        )
    )
    val glassBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.42f),
            activeAccent.copy(alpha = 0.50f),
            MeloColors.glassBorder.copy(alpha = 0.40f),
            Color.White.copy(alpha = 0.12f)
        )
    )

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(glassFillBrush)
            .border(width = 1.2.dp, brush = glassBorderBrush, shape = cardShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            activeAccent.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            LyricsHeaderBar(
                hasLyrics = trackLyrics != null && (trackLyrics.hasSync || !trackLyrics.plainLyrics.isNullOrBlank()),
                showTranslation = showTranslation,
                isTranslating = isTranslating,
                targetLanguage = targetLanguage,
                activeAccent = activeAccent,
                onToggleTranslation = onToggleTranslation,
                onSelectLanguage = onSelectLanguage,
                onToggleArtwork = onToggleArtwork
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = activeAccent,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Buscando letras...",
                                    style = MeloType.body,
                                    color = MeloColors.textMuted
                                )
                            }
                        }
                    }

                    trackLyrics == null || (!trackLyrics.hasSync && trackLyrics.plainLyrics.isNullOrBlank()) -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MeloColors.textMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = trackLyrics?.error
                                        ?: "Letra no disponible para esta canción",
                                    style = MeloType.body,
                                    color = MeloColors.textMuted,
                                    textAlign = TextAlign.Center
                                )
                                if (onToggleArtwork != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Toca para volver a la portada",
                                        style = MeloType.labelSmall.copy(fontSize = 11.sp),
                                        color = MeloColors.textMuted.copy(alpha = 0.7f),
                                        modifier = Modifier.clickable { onToggleArtwork() }
                                    )
                                }
                            }
                        }
                    }

                    trackLyrics.hasSync -> {
                        SyncedLyricsList(
                            lines = trackLyrics.syncedLyrics,
                            activeIndex = activeLyricIndex,
                            showTranslation = showTranslation,
                            activeAccent = activeAccent,
                            onSeekTo = onSeekTo,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        PlainLyricsView(
                            text = trackLyrics.plainLyrics ?: "",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MeloColors.glassSurface.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MeloColors.glassSurface.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun LyricsHeaderBar(
    hasLyrics: Boolean,
    showTranslation: Boolean,
    isTranslating: Boolean,
    targetLanguage: String,
    activeAccent: Color,
    onToggleTranslation: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onToggleArtwork: (() -> Unit)? = null
) {
    var showLanguageMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = activeAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Letras",
                style = MeloType.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MeloColors.textPrimary
            )
        }

        if (hasLyrics) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box {
                    val buttonBg by animateColorAsState(
                        targetValue = if (showTranslation) activeAccent.copy(alpha = 0.22f) else MeloColors.chromePillFill,
                        label = "TranslationBgAnim"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (showTranslation) activeAccent else MeloColors.textMuted,
                        label = "TranslationTintAnim"
                    )

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(buttonBg)
                            .border(
                                width = 1.dp,
                                color = if (showTranslation) activeAccent.copy(alpha = 0.4f) else MeloColors.chromePillBorder,
                                shape = CircleShape
                            )
                            .clickable { onToggleTranslation() }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(
                                strokeWidth = 1.5.dp,
                                color = activeAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Traducir",
                                tint = iconTint,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = targetLanguage.uppercase(),
                            style = MeloType.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = iconTint
                        )
                    }

                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                        modifier = Modifier.background(MeloColors.surface1)
                    ) {
                        val languages = listOf(
                            "es" to "Español",
                            "en" to "English",
                            "pt" to "Português",
                            "fr" to "Français",
                            "de" to "Deutsch",
                            "it" to "Italiano",
                            "ja" to "日本語"
                        )
                        languages.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$label (${code.uppercase()})",
                                        style = MeloType.body,
                                        color = if (targetLanguage == code) activeAccent else MeloColors.textPrimary
                                    )
                                },
                                onClick = {
                                    onSelectLanguage(code)
                                    showLanguageMenu = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showLanguageMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Cambiar idioma",
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }

                if (onToggleArtwork != null) {
                    IconButton(
                        onClick = onToggleArtwork,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar letras",
                            tint = MeloColors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsList(
    lines: List<SyncedLine>,
    activeIndex: Int,
    showTranslation: Boolean,
    activeAccent: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < lines.size) {
            val targetIndex = (activeIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(
                index = targetIndex,
                scrollOffset = 0
            )
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 20.dp, bottom = 80.dp, start = 18.dp, end = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeIndex
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.45f,
                animationSpec = spring(stiffness = 320f),
                label = "LyricAlphaAnim"
            )
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.02f else 1.0f,
                animationSpec = spring(stiffness = 320f),
                label = "LyricScaleAnim"
            )
            val textColor by animateColorAsState(
                targetValue = if (isActive) activeAccent else MeloColors.textPrimary,
                animationSpec = tween(220),
                label = "LyricColorAnim"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSeekTo(line.timeMs) }
                    )
                    .padding(vertical = 4.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if (line.text.isNotBlank()) {
                    Text(
                        text = line.text,
                        style = MeloType.body.copy(
                            fontSize = if (isActive) 18.sp else 15.5.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            lineHeight = if (isActive) 24.sp else 21.sp
                        ),
                        color = textColor
                    )
                } else {
                    DancingMelodyIndicator(
                        isActive = isActive,
                        tint = textColor,
                        baseFontSize = if (isActive) 18.sp else 15.5.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                val translation = line.translation
                if (showTranslation && !translation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = translation,
                        style = MeloType.body.copy(
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        ),
                        color = if (isActive) activeAccent.copy(alpha = 0.88f) else MeloColors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun PlainLyricsView(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = text,
            style = MeloType.body.copy(
                fontSize = 14.5.sp,
                lineHeight = 22.sp
            ),
            color = MeloColors.textPrimary
        )
    }
}

@Composable
fun DancingMelodyIndicator(
    isActive: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    baseFontSize: androidx.compose.ui.unit.TextUnit = 17.sp,
    noteCount: Int = 3
) {
    val notes = listOf("♪", "♫", "♩", "♬")

    if (isActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "DancingMelodyTransition")

        val iconScale by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "IconScaleAnim"
        )

        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Melodía",
                tint = tint,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
            )

            val delays = listOf(0, 160, 320, 480)
            for (i in 0 until noteCount.coerceAtMost(notes.size)) {
                val delay = delays.getOrElse(i) { i * 160 }
                val bounceY by infiniteTransition.animateFloat(
                    initialValue = 1.5f,
                    targetValue = -5.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 420,
                            delayMillis = delay,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "NoteBounce_$i"
                )
                val rotation by infiniteTransition.animateFloat(
                    initialValue = -10f,
                    targetValue = 10f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 480,
                            delayMillis = delay,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "NoteRotate_$i"
                )
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.90f,
                    targetValue = 1.20f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 380,
                            delayMillis = delay,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "NoteScale_$i"
                )

                Text(
                    text = notes[i % notes.size],
                    style = MeloType.body.copy(
                        fontSize = baseFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = tint,
                    modifier = Modifier
                        .offset(y = bounceY.dp)
                        .graphicsLayer(
                            rotationZ = rotation,
                            scaleX = scale,
                            scaleY = scale
                        )
                )
            }
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Melodía",
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "♪ ♫ ♪",
                style = MeloType.body.copy(
                    fontSize = baseFontSize,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                ),
                color = tint
            )
        }
    }
}