@file:Suppress(
    "FunctionNaming",
    "LongMethod",
    "ktlint:standard:function-naming",
)

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.update.AppRelease
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
    val changelog = remember(release.releaseNotes) { ReleaseChangelogParser.parse(release.releaseNotes) }
    val scrollState = rememberScrollState()
    val isNightly = release.channel == UpdateChannel.NIGHTLY

    val titleText =
        if (isInstalledVersion) {
            stringResource(Res.string.update_changelog_installed_title, release.version)
        } else {
            stringResource(Res.string.update_changelog_title)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MeloColors.surface1,
        shape = RoundedCornerShape(18.dp),
        title = {
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
        },
        text = {
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
                    changelog.sections.forEachIndexed { index, section ->
                        if (index > 0) {
                            HorizontalDivider(color = MeloColors.border.copy(alpha = 0.5f))
                        }
                        ChangelogSectionBlock(section = section)
                    }
                }
            }
        },
        dismissButton =
            if (release.htmlUrl.isNotBlank()) {
                {
                    TextButton(onClick = { uriHandler.openUri(release.htmlUrl) }) {
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
            } else {
                null
            },
        confirmButton = {
            Button(onClick = onDismiss) {
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
        },
    )
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
