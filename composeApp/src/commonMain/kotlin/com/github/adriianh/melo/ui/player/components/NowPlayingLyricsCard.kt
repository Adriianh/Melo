package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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

    val cardBg = activeAccent.copy(alpha = 0.18f)
    val borderColor = activeAccent.copy(alpha = 0.35f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onExpand() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        modifier = Modifier.size(16.dp)
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
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (currentLine != null) {
                Text(
                    text = currentLine.text,
                    style = MeloType.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    ),
                    color = MeloColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val translation = currentLine.translation
                if (showTranslation && !translation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = translation,
                        style = MeloType.body.copy(
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        ),
                        color = activeAccent.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (nextLine != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = nextLine.text,
                        style = MeloType.body.copy(
                            fontSize = 14.sp,
                            lineHeight = 18.sp
                        ),
                        color = MeloColors.textMuted.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                val plain = trackLyrics.plainLyrics
                if (!plain.isNullOrBlank()) {
                    val previewSnippet =
                        plain.lines().filter { it.isNotBlank() }.take(3).joinToString("\n")
                    Text(
                        text = previewSnippet,
                        style = MeloType.body.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = MeloColors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
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
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MeloColors.surface1.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MeloColors.borderStrong),
        shadowElevation = 16.dp,
        modifier = modifier
    ) {
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
                                    MeloColors.surface1.copy(alpha = 0.9f),
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
                                    MeloColors.surface1.copy(alpha = 0.95f)
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
                        targetValue = if (showTranslation) activeAccent.copy(alpha = 0.22f) else MeloColors.surface2,
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
        contentPadding = PaddingValues(top = 28.dp, bottom = 100.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeIndex
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.38f,
                animationSpec = spring(stiffness = 300f),
                label = "LyricAlphaAnim"
            )
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.03f else 1.0f,
                animationSpec = spring(stiffness = 300f),
                label = "LyricScaleAnim"
            )
            val textColor by animateColorAsState(
                targetValue = if (isActive) activeAccent else MeloColors.textPrimary,
                label = "LyricColorAnim"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSeekTo(line.timeMs) }
                    )
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = line.text,
                    style = MeloType.body.copy(
                        fontSize = if (isActive) 19.sp else 16.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        lineHeight = 26.sp
                    ),
                    color = textColor
                )

                val translation = line.translation
                if (showTranslation && !translation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = translation,
                        style = MeloType.body.copy(
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.5.sp,
                            lineHeight = 18.sp
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
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = text,
            style = MeloType.body.copy(
                fontSize = 15.sp,
                lineHeight = 26.sp
            ),
            color = MeloColors.textPrimary
        )
    }
}

@Composable
fun NowPlayingLyricsSection(
    lyrics: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (!lyrics.isNullOrBlank()) lyrics else "Letras no disponibles",
            style = MeloType.body,
            color = MeloColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.verticalScroll(rememberScrollState())
        )
    }
}