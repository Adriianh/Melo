@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_change_language
import com.github.adriianh.melo.resources.a11y_lyrics_source
import com.github.adriianh.melo.resources.a11y_translate
import com.github.adriianh.melo.resources.player_lyrics_source_lrclib
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.jetbrains.compose.resources.stringResource

internal val SUPPORTED_LYRICS_LANGUAGES: List<Pair<String, String>> =
    listOf(
        "es" to "Español",
        "en" to "English",
        "pt" to "Português",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "ja" to "日本語",
    )

@Composable
internal fun LyricsTranslationChip(
    showLanguageMenu: Boolean,
    onShowLanguageMenuChange: (Boolean) -> Unit,
    showTranslation: Boolean,
    isTranslating: Boolean,
    targetLanguage: String,
    activeAccent: Color,
    onToggleTranslation: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier,
    useAccessibleTouchTarget: Boolean = false,
    inactiveContentColor: Color = MeloColors.textMuted,
    inactiveFillColor: Color = MeloColors.chromePillFill,
    inactiveBorderColor: Color = MeloColors.chromePillBorder,
) {
    val translateLabel = stringResource(Res.string.a11y_translate)
    val buttonBg by animateColorAsState(
        targetValue = if (showTranslation) activeAccent.copy(alpha = 0.24f) else inactiveFillColor,
        label = "TranslationBgAnim",
    )
    val iconTint by animateColorAsState(
        targetValue = if (showTranslation) activeAccent else inactiveContentColor,
        label = "TranslationTintAnim",
    )

    Box(modifier = modifier) {
        val targetModifier =
            if (useAccessibleTouchTarget) {
                Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            } else {
                Modifier
            }
        Box(
            modifier =
                targetModifier
                    .semantics {
                        selected = showTranslation
                        contentDescription = "$translateLabel (${targetLanguage.uppercase()})"
                    }.clip(CircleShape)
                    .clickable(role = Role.Switch, onClick = onToggleTranslation),
            contentAlignment = Alignment.Center,
        ) {
            LyricsTranslationChipPill(
                showTranslation = showTranslation,
                isTranslating = isTranslating,
                targetLanguage = targetLanguage,
                activeAccent = activeAccent,
                buttonBg = buttonBg,
                iconTint = iconTint,
                inactiveBorderColor = inactiveBorderColor,
            )
        }

        LyricsLanguageMenu(
            expanded = showLanguageMenu,
            onDismiss = { onShowLanguageMenuChange(false) },
            targetLanguage = targetLanguage,
            activeAccent = activeAccent,
            onSelectLanguage = { code ->
                onSelectLanguage(code)
                onShowLanguageMenuChange(false)
            },
            modifier = Modifier.background(MeloColors.surface1),
        )
    }
}

@Composable
private fun LyricsTranslationChipPill(
    showTranslation: Boolean,
    isTranslating: Boolean,
    targetLanguage: String,
    activeAccent: Color,
    buttonBg: Color,
    iconTint: Color,
    inactiveBorderColor: Color,
) {
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(buttonBg)
                .border(
                    width = 1.dp,
                    color = if (showTranslation) activeAccent.copy(alpha = 0.45f) else inactiveBorderColor,
                    shape = CircleShape,
                ).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isTranslating) {
            CircularProgressIndicator(
                strokeWidth = 1.5.dp,
                color = activeAccent,
                modifier = Modifier.size(14.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = targetLanguage.uppercase(),
            style = MeloType.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            color = iconTint,
        )
    }
}

@Composable
internal fun LyricsLanguagePickerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MeloColors.textMuted,
    useAccessibleTouchTarget: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = if (useAccessibleTouchTarget) modifier.size(48.dp) else modifier.size(28.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = stringResource(Res.string.a11y_change_language),
            tint = tint,
            modifier = Modifier.size(if (useAccessibleTouchTarget) 18.dp else 15.dp),
        )
    }
}

@Composable
internal fun LyricsSourceBadge(
    modifier: Modifier = Modifier,
    sourceName: String = stringResource(Res.string.player_lyrics_source_lrclib),
) {
    val a11yDescription = stringResource(Res.string.a11y_lyrics_source, sourceName)
    Row(
        modifier =
            modifier
                .semantics { contentDescription = a11yDescription }
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.36f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = sourceName,
            style = MeloType.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.82f),
        )
    }
}

@Composable
internal fun LyricsLanguageMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    targetLanguage: String,
    activeAccent: Color,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        SUPPORTED_LYRICS_LANGUAGES.forEach { (code, label) ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = "$label (${code.uppercase()})",
                        style = MeloType.body,
                        color = if (targetLanguage == code) activeAccent else MeloColors.textPrimary,
                    )
                },
                onClick = { onSelectLanguage(code) },
            )
        }
    }
}
