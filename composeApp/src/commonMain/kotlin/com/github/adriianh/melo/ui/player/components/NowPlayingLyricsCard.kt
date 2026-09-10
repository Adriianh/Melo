package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

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
        LyricsEnvironmentVignette(activeAccent)

        Column(modifier = Modifier.fillMaxSize()) {
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

            LyricsBody(
                trackLyrics = trackLyrics,
                activeLyricIndex = activeLyricIndex,
                isLoading = isLoading,
                showTranslation = showTranslation,
                activeAccent = activeAccent,
                onSeekTo = onSeekTo,
                onToggleArtwork = onToggleArtwork,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LyricsEnvironmentVignette(activeAccent: Color) {
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
}

@Composable
private fun LyricsBody(
    trackLyrics: TrackLyrics?,
    activeLyricIndex: Int,
    isLoading: Boolean,
    showTranslation: Boolean,
    activeAccent: Color,
    onSeekTo: (Long) -> Unit,
    onToggleArtwork: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when {
            isLoading -> LyricsLoading(activeAccent)

            trackLyrics == null || (!trackLyrics.hasSync && trackLyrics.plainLyrics.isNullOrBlank()) ->
                LyricsUnavailable(trackLyrics, onToggleArtwork)

            trackLyrics.hasSync -> SyncedLyricsList(
                lines = trackLyrics.syncedLyrics,
                activeIndex = activeLyricIndex,
                showTranslation = showTranslation,
                activeAccent = activeAccent,
                onSeekTo = onSeekTo,
                modifier = Modifier.fillMaxSize()
            )

            else -> PlainLyricsView(
                text = trackLyrics.plainLyrics ?: "",
                modifier = Modifier.fillMaxSize()
            )
        }

        LibraryShaderOverlay()
    }
}

@Composable
private fun LyricsLoading(activeAccent: Color) {
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

@Composable
private fun LyricsUnavailable(
    trackLyrics: TrackLyrics?,
    onToggleArtwork: (() -> Unit)?,
) {
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
                text = trackLyrics?.error ?: "Letra no disponible para esta canción",
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

@Composable
private fun BoxScope.LibraryShaderOverlay() {
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
            LyricsHeaderActions(
                showLanguageMenu = showLanguageMenu,
                onShowLanguageMenuChange = { showLanguageMenu = it },
                showTranslation = showTranslation,
                isTranslating = isTranslating,
                targetLanguage = targetLanguage,
                activeAccent = activeAccent,
                onToggleTranslation = onToggleTranslation,
                onSelectLanguage = onSelectLanguage,
                onToggleArtwork = onToggleArtwork
            )
        }
    }
}

@Composable
private fun LyricsHeaderActions(
    showLanguageMenu: Boolean,
    onShowLanguageMenuChange: (Boolean) -> Unit,
    showTranslation: Boolean,
    isTranslating: Boolean,
    targetLanguage: String,
    activeAccent: Color,
    onToggleTranslation: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onToggleArtwork: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TranslationButton(
            showLanguageMenu = showLanguageMenu,
            onShowLanguageMenuChange = onShowLanguageMenuChange,
            showTranslation = showTranslation,
            isTranslating = isTranslating,
            targetLanguage = targetLanguage,
            activeAccent = activeAccent,
            onToggleTranslation = onToggleTranslation,
            onSelectLanguage = onSelectLanguage
        )

        IconButton(
            onClick = { onShowLanguageMenuChange(true) },
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

@Composable
private fun TranslationButton(
    showLanguageMenu: Boolean,
    onShowLanguageMenuChange: (Boolean) -> Unit,
    showTranslation: Boolean,
    isTranslating: Boolean,
    targetLanguage: String,
    activeAccent: Color,
    onToggleTranslation: () -> Unit,
    onSelectLanguage: (String) -> Unit,
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

        LanguageMenu(
            expanded = showLanguageMenu,
            onDismiss = { onShowLanguageMenuChange(false) },
            targetLanguage = targetLanguage,
            activeAccent = activeAccent,
            onSelectLanguage = { code ->
                onSelectLanguage(code)
                onShowLanguageMenuChange(false)
            },
            modifier = Modifier.background(MeloColors.surface1)
        )
    }
}

@Composable
private fun LanguageMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    targetLanguage: String,
    activeAccent: Color,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
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
                onClick = { onSelectLanguage(code) }
            )
        }
    }
}