@file:Suppress(
    "FunctionNaming",
    "LongMethod",
    "ktlint:standard:function-naming",
)

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.ChangelogGroup
import com.github.adriianh.core.domain.model.update.ChangelogItem
import com.github.adriianh.core.domain.model.update.ChangelogSection
import com.github.adriianh.core.domain.model.update.ChangelogSpan
import com.github.adriianh.core.domain.model.update.ReleaseChangelogParser
import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.common_action_close
import com.github.adriianh.melo.resources.common_action_got_it
import com.github.adriianh.melo.resources.update_changelog_dialog_subtitle
import com.github.adriianh.melo.resources.update_changelog_empty
import com.github.adriianh.melo.resources.update_changelog_installed_title
import com.github.adriianh.melo.resources.update_changelog_open_github
import com.github.adriianh.melo.resources.update_changelog_tab_since_nightly
import com.github.adriianh.melo.resources.update_changelog_tab_since_stable
import com.github.adriianh.melo.resources.update_changelog_tab_since_stable_version
import com.github.adriianh.melo.resources.update_changelog_title
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChangelogDialog(
    release: AppRelease,
    isInstalledVersion: Boolean = false,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Dialog(onDismissRequest = onDismiss) {
        ChangelogDialogCard(
            release = release,
            isInstalledVersion = isInstalledVersion,
            onOpenGitHub = {
                if (release.htmlUrl.isNotBlank()) {
                    uriHandler.openUri(release.htmlUrl)
                }
            },
            onDismiss = onDismiss,
        )
    }
}

@Composable
internal fun ChangelogDialogCard(
    release: AppRelease,
    isInstalledVersion: Boolean = false,
    onOpenGitHub: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val changelog = remember(release.releaseNotes) { ReleaseChangelogParser.parse(release.releaseNotes) }
    val scrollState = rememberScrollState()
    val isNightly = release.channel == UpdateChannel.NIGHTLY

    val titleText =
        if (isInstalledVersion) {
            stringResource(Res.string.update_changelog_installed_title, release.version)
        } else {
            stringResource(Res.string.update_changelog_title)
        }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MeloColors.surface1,
        border = BorderStroke(1.dp, MeloColors.border),
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = titleText,
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MeloColors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isNightly) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = "Nightly",
                                style = MeloType.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (!isInstalledVersion) {
                    Text(
                        text = stringResource(Res.string.update_changelog_dialog_subtitle, release.version),
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary,
                    )
                }
            }

            val hasMultipleGroups = changelog.groups.size > 1
            var selectedGroupIndex by remember(release.releaseNotes) { mutableIntStateOf(0) }
            val activeSections =
                if (hasMultipleGroups) {
                    changelog.groups[selectedGroupIndex.coerceIn(0, changelog.groups.lastIndex)].sections
                } else {
                    changelog.sections
                }

            LaunchedEffect(selectedGroupIndex) {
                scrollState.scrollTo(0)
            }

            if (hasMultipleGroups) {
                ChangelogGroupTabs(
                    groups = changelog.groups,
                    selectedIndex = selectedGroupIndex.coerceIn(0, changelog.groups.lastIndex),
                    onSelect = { selectedGroupIndex = it },
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (changelog.isEmpty) {
                    Text(
                        text = stringResource(Res.string.update_changelog_empty),
                        style = MeloType.body,
                        color = MeloColors.textSecondary,
                    )
                } else {
                    activeSections.forEachIndexed { index, section ->
                        if (index > 0) {
                            HorizontalDivider(color = MeloColors.border.copy(alpha = 0.5f))
                        }
                        ChangelogSectionBlock(section = section)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (release.htmlUrl.isNotBlank()) {
                    TextButton(onClick = onOpenGitHub) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(Res.string.update_changelog_open_github),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(
                        text =
                            stringResource(
                                if (isInstalledVersion) {
                                    Res.string.common_action_got_it
                                } else {
                                    Res.string.common_action_close
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangelogGroupTabs(
    groups: List<ChangelogGroup>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MeloColors.surface2,
        border = BorderStroke(1.dp, MeloColors.border.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            groups.forEachIndexed { index, group ->
                val isSelected = index == selectedIndex
                val label = resolveGroupTabLabel(group.title)
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            Color.Transparent
                        },
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(7.dp))
                            .clickable { onSelect(index) },
                ) {
                    Text(
                        text = label,
                        style = MeloType.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MeloColors.textSecondary
                            },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

private val STABLE_VERSION_IN_HEADING_REGEX = Regex("""\(`?([^`)]+)`?\)""")

@Composable
private fun resolveGroupTabLabel(groupTitle: String): String =
    when {
        groupTitle.contains("since the last nightly", ignoreCase = true) ->
            stringResource(Res.string.update_changelog_tab_since_nightly)

        groupTitle.contains("since the last stable release", ignoreCase = true) -> {
            val version =
                STABLE_VERSION_IN_HEADING_REGEX
                    .find(groupTitle)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.trim()
            if (!version.isNullOrBlank()) {
                stringResource(Res.string.update_changelog_tab_since_stable_version, version)
            } else {
                stringResource(Res.string.update_changelog_tab_since_stable)
            }
        }

        else -> groupTitle
    }

@Composable
private fun ChangelogSectionBlock(section: ChangelogSection) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (section.title.isNotBlank()) {
            Text(
                text = section.title,
                style = MeloType.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        val emptyNote = section.emptyNote
        if (section.items.isEmpty() && emptyNote != null) {
            Text(
                text = emptyNote,
                style = MeloType.labelSmall,
                color = MeloColors.textMuted,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                section.items.forEach { item ->
                    ChangelogItemRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun ChangelogItemRow(item: ChangelogItem) {
    val codeBg = MeloColors.surface2
    val annotated =
        remember(item.spans, codeBg) {
            buildChangelogAnnotatedString(item.spans, codeBg)
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (item.tag != null) {
            ChangelogTypeBadge(
                tag = item.tag!!,
                modifier = Modifier.padding(top = 2.dp),
            )
        } else {
            Text(
                text = "•",
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary,
                modifier = Modifier.padding(top = 1.dp),
            )
        }

        Text(
            text = annotated,
            style = MeloType.labelSmall,
            color = MeloColors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ChangelogTypeBadge(
    tag: String,
    modifier: Modifier = Modifier,
) {
    val badgeColor =
        when (tag.lowercase()) {
            "feat" -> MaterialTheme.colorScheme.primary
            "fix" -> MaterialTheme.colorScheme.error
            "perf" -> MaterialTheme.colorScheme.tertiary
            else -> MeloColors.textSecondary
        }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = badgeColor.copy(alpha = 0.14f),
        modifier = modifier,
    ) {
        Text(
            text = tag.lowercase(),
            style = MeloType.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

private fun buildChangelogAnnotatedString(
    spans: List<ChangelogSpan>,
    codeBackground: Color,
): AnnotatedString =
    buildAnnotatedString {
        spans.forEach { span ->
            when (span) {
                is ChangelogSpan.Plain -> append(span.text)
                is ChangelogSpan.Bold ->
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(span.text)
                    }
                is ChangelogSpan.Code ->
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground,
                        ),
                    ) {
                        append(" ${span.text} ")
                    }
            }
        }
    }

@Preview(widthDp = 380)
@Composable
private fun PreviewChangelogDialogNightly() {
    val sampleNotes =
        """
        ## Build information
        | **Version** | `2.5.0-nightly.20261123.0100` |

        > [!WARNING]
        > Automated development build.

        ## Changes since the last nightly
        - [`a1b2c3d`](https://github.com/Adriianh/Melo/commit/a1b2c3d) feat(player): soporte para reproducción sin pausas
        - fix(ui): corrige superposición en el mini reproductor

        ## Changes since the last stable release (`v2.5.0`)
        - feat(update): nuevo diálogo de novedades con parser propio
        - perf(cache): mejora carga inicial de portadas `LRU`

        ### CLI (TUI)
        ```bash
        curl -fsSL https://example.com/install.sh | bash
        ```
        """.trimIndent()

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ChangelogDialogCard(
                release =
                    AppRelease(
                        version = "2.5.0-nightly.20261123.0100",
                        tagName = "nightly",
                        name = "Melo Nightly",
                        releaseNotes = sampleNotes,
                        htmlUrl = "https://github.com/Adriianh/Melo/releases/tag/nightly",
                        publishedAt = "2026-11-23T01:00:00Z",
                        assets = emptyList(),
                        channel = UpdateChannel.NIGHTLY,
                    ),
                isInstalledVersion = false,
            )
        }
    }
}

@Preview(widthDp = 380)
@Composable
private fun PreviewChangelogDialogStableInstalled() {
    val sampleNotes =
        """
        ## What's Changed
        * feat(library): sincronización de playlists offline by @Adriianh in https://github.com/Adriianh/Melo/pull/42
        * Soporte para etiquetas `FLAC` en biblioteca local by @Adriianh in https://github.com/Adriianh/Melo/pull/43

        **Full Changelog**: https://github.com/Adriianh/Melo/compare/v2.4.0...v2.5.0
        """.trimIndent()

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ChangelogDialogCard(
                release =
                    AppRelease(
                        version = "2.5.0",
                        tagName = "v2.5.0",
                        name = "Melo 2.5.0",
                        releaseNotes = sampleNotes,
                        htmlUrl = "https://github.com/Adriianh/Melo/releases/tag/v2.5.0",
                        publishedAt = "2026-11-20T12:00:00Z",
                        assets = emptyList(),
                        channel = UpdateChannel.STABLE,
                    ),
                isInstalledVersion = true,
            )
        }
    }
}

@Preview(widthDp = 380)
@Composable
private fun PreviewChangelogDialogEmpty() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ChangelogDialogCard(
                release =
                    AppRelease(
                        version = "2.5.0",
                        tagName = "v2.5.0",
                        name = "Melo 2.5.0",
                        releaseNotes = "",
                        htmlUrl = "https://github.com/Adriianh/Melo/releases/tag/v2.5.0",
                        publishedAt = "2026-11-20T12:00:00Z",
                        assets = emptyList(),
                        channel = UpdateChannel.STABLE,
                    ),
                isInstalledVersion = true,
            )
        }
    }
}
