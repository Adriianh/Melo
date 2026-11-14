@file:Suppress(
    "FunctionNaming",
    "LongMethod",
    "MagicNumber",
    "ktlint:standard:function-naming",
)

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

internal val LocalSettingsSearchQuery = compositionLocalOf { "" }

private const val SEARCH_HIGHLIGHT_BG_ALPHA = 0.16f

/**
 * Maps a single character to lowercase folded ASCII (1-to-1) so highlight indices
 * align directly with the original string.
 */
private fun foldSearchChar(ch: Char): Char =
    when (ch.lowercaseChar()) {
        'á', 'à', 'â', 'ä', 'ã', 'å' -> 'a'
        'é', 'è', 'ê', 'ë' -> 'e'
        'í', 'ì', 'î', 'ï' -> 'i'
        'ó', 'ò', 'ô', 'ö', 'õ', 'ø' -> 'o'
        'ú', 'ù', 'û', 'ü' -> 'u'
        'ñ' -> 'n'
        'ç' -> 'c'
        'ý', 'ÿ' -> 'y'
        else -> ch.lowercaseChar()
    }

internal fun foldForSearch(input: String): String =
    buildString(input.length) {
        input.forEach { ch -> append(foldSearchChar(ch)) }
    }

internal fun tokenizeSearchQuery(query: String): List<String> =
    foldForSearch(query)
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .distinct()

internal fun filterSettingEntries(
    entries: List<SettingEntry>,
    query: String,
): List<SettingEntry> {
    val tokens = tokenizeSearchQuery(query)
    if (tokens.isEmpty()) return emptyList()

    return entries.filter { entry ->
        if (!entry.visible) return@filter false
        val haystack =
            foldForSearch(
                buildString {
                    append(entry.title)
                    append(' ')
                    if (!entry.description.isNullOrBlank()) {
                        append(entry.description)
                        append(' ')
                    }
                    if (!entry.section.isNullOrBlank()) {
                        append(entry.section)
                        append(' ')
                    }
                    append(entry.category.title)
                    entry.keywords.forEach { keyword ->
                        append(' ')
                        append(keyword)
                    }
                },
            )
        tokens.all { token -> haystack.contains(token) }
    }
}

internal fun buildHighlightedTitle(
    title: String,
    query: String,
    highlightColor: Color,
): AnnotatedString {
    val tokens = tokenizeSearchQuery(query)
    val foldedTitle = if (tokens.isEmpty()) "" else foldForSearch(title)
    val rawRanges = if (tokens.isEmpty()) emptyList() else collectMatchedRanges(foldedTitle, tokens)
    if (rawRanges.isEmpty()) return AnnotatedString(title)

    val mergedRanges = mutableListOf<IntRange>()
    rawRanges.sortedBy { it.first }.forEach { range ->
        val previous = mergedRanges.lastOrNull()
        if (previous != null && range.first <= previous.last + 1) {
            mergedRanges[mergedRanges.lastIndex] = previous.first..maxOf(previous.last, range.last)
        } else {
            mergedRanges.add(range)
        }
    }

    val highlightStyle =
        SpanStyle(
            color = highlightColor,
            background = highlightColor.copy(alpha = SEARCH_HIGHLIGHT_BG_ALPHA),
            fontWeight = FontWeight.SemiBold,
        )

    return buildAnnotatedString {
        append(title)
        mergedRanges.forEach { range ->
            addStyle(highlightStyle, start = range.first, end = range.last + 1)
        }
    }
}

private fun collectMatchedRanges(
    foldedTitle: String,
    tokens: List<String>,
): List<IntRange> =
    buildList {
        tokens.forEach { token ->
            var startIndex = 0
            while (startIndex <= foldedTitle.length - token.length) {
                val matchIndex = foldedTitle.indexOf(token, startIndex = startIndex)
                if (matchIndex < 0) break
                add(matchIndex until (matchIndex + token.length))
                startIndex = matchIndex + token.length
            }
        }
    }

@Composable
internal fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    showInternalClearButton: Boolean = true,
    onClearOrEscape: (() -> Unit)? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)

    Surface(
        modifier = modifier.heightIn(min = 48.dp),
        shape = shape,
        color = MeloColors.surface2.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, if (isFocused) primary else MeloColors.border),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(start = 12.dp, end = if (showInternalClearButton && query.isNotEmpty()) 4.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isFocused) primary else MeloColors.textSecondary,
                modifier = Modifier.size(18.dp),
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Buscar en ajustes",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MeloColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle =
                        TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MeloColors.textPrimary,
                        ),
                    cursorBrush = SolidColor(primary),
                    interactionSource = interactionSource,
                    keyboardOptions =
                        KeyboardOptions(
                            imeAction = ImeAction.Search,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onSearch = { focusManager.clearFocus() },
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .semantics { contentDescription = "Buscar en ajustes" }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                                    if (query.isNotEmpty()) {
                                        onQueryChange("")
                                        true
                                    } else if (onClearOrEscape != null) {
                                        onClearOrEscape()
                                        true
                                    } else {
                                        false
                                    }
                                } else {
                                    false
                                }
                            },
                )
            }

            if (showInternalClearButton && query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpiar búsqueda",
                        tint = MeloColors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SettingSearchResultsPane(
    entries: List<SettingEntry>,
    query: String,
    onOpenCategory: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val matches = remember(entries, query) { filterSettingEntries(entries, query) }
    val grouped =
        remember(matches) {
            SettingsCategory.entries.mapNotNull { category ->
                val categoryMatches = matches.filter { it.category == category }
                categoryMatches.takeIf { it.isNotEmpty() }?.let { category to it }
            }
        }

    CompositionLocalProvider(LocalSettingsSearchQuery provides query) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val countText =
                when (matches.size) {
                    0 -> "0 resultados"
                    1 -> "1 resultado"
                    else -> "${matches.size} resultados"
                }

            Text(
                text = countText,
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary,
                modifier =
                    Modifier
                        .padding(horizontal = 6.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
            )

            if (grouped.isEmpty()) {
                SettingsSearchEmptyState(query = query.trim())
            } else {
                grouped.forEach { (category, categoryEntries) ->
                    SettingsSection(caption = category.title) {
                        categoryEntries.forEachIndexed { index, entry ->
                            if (index > 0) {
                                SettingsRowDivider()
                            }
                            if (entry.searchDisplay == SettingSearchDisplay.LINK) {
                                SettingSearchLinkRow(
                                    entry = entry,
                                    query = query,
                                    onClick = { onOpenCategory(entry.category) },
                                )
                            } else {
                                entry.content()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSearchLinkRow(
    entry: SettingEntry,
    query: String,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val titleText =
        remember(entry.title, query, primary) {
            buildHighlightedTitle(
                title = entry.title,
                query = query,
                highlightColor = primary,
            )
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = titleText,
                style = MeloType.body,
                fontWeight = FontWeight.Medium,
                color = MeloColors.textPrimary,
            )
            if (!entry.description.isNullOrBlank()) {
                Text(
                    text = entry.description,
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary,
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Ir a ${entry.category.title}",
            tint = MeloColors.textMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsSearchEmptyState(query: String) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = primary.copy(alpha = 0.12f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        Text(
            text = "Sin resultados para “$query”",
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MeloColors.textPrimary,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Prueba con otra palabra",
            style = MeloType.labelSmall,
            color = MeloColors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
