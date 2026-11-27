package com.github.adriianh.core.domain.model.update

/**
 * Inline formatting span within a parsed changelog item.
 */
sealed interface ChangelogSpan {
    data class Plain(
        val text: String,
    ) : ChangelogSpan

    data class Bold(
        val text: String,
    ) : ChangelogSpan

    data class Code(
        val text: String,
    ) : ChangelogSpan
}

/**
 * A single bullet entry or note line in a [ChangelogSection].
 */
data class ChangelogItem(
    val spans: List<ChangelogSpan>,
    val tag: String? = null,
)

/**
 * A titled group of changes extracted from a GitHub release body.
 */
data class ChangelogSection(
    val title: String,
    val items: List<ChangelogItem>,
    val emptyNote: String? = null,
)

/**
 * A titled group of [ChangelogSection] entries (typically `##` headings such as
 * "Changes since the last nightly" vs "Changes since the last stable release").
 */
data class ChangelogGroup(
    val title: String,
    val sections: List<ChangelogSection>,
)

/**
 * Structured representation of release notes after stripping CI boilerplate.
 */
data class ReleaseChangelog(
    val sections: List<ChangelogSection>,
    val groups: List<ChangelogGroup> = emptyList(),
) {
    val isEmpty: Boolean
        get() =
            if (groups.isNotEmpty()) {
                groups.all { group ->
                    group.sections.all { it.items.isEmpty() && it.emptyNote == null }
                }
            } else {
                sections.all { it.items.isEmpty() && it.emptyNote == null }
            }
}

/**
 * Lightweight Markdown parser tailored for Melo's GitHub release notes (both
 * Stable auto-generated notes and Nightly CI-generated bodies).
 */
object ReleaseChangelogParser {
    private val HEADING_REGEX = Regex("""^(#{1,6})\s+(.+)$""")
    private val BULLET_REGEX = Regex("""^(?:[-*+]|•)\s+(.+)$""")
    private val MARKDOWN_LINK_REGEX = Regex("""\[([^\]]+)]\((https?://[^)]+)\)""")
    private val COMMIT_HASH_LINK_PREFIX_REGEX = Regex("""^`?[0-9a-f]{7,12}`?\s*[-–—:]?\s*""")
    private val PR_AUTHOR_SUFFIX_REGEX = Regex("""\s+by\s+@[\w-]+\s+in\s+https?://\S+$""")
    private val FULL_CHANGELOG_LINE_REGEX = Regex("""^\*{0,2}Full Changelog\*{0,2}\s*:.*$""", RegexOption.IGNORE_CASE)
    private val CONVENTIONAL_PREFIX_REGEX =
        Regex("""^(feat|fix|perf|refactor|docs|style|test|build|ci|chore)(?:\(([^)]+)\))?!?:\s*(.+)$""")
    private val INLINE_TOKEN_REGEX = Regex("""\*\*([^*]+)\*\*|`([^`]+)`""")

    private val IGNORED_SECTION_PREFIXES =
        listOf(
            "build information",
            "cli (tui)",
            "downloads & assets",
            "downloads and assets",
        )

    private class ParserState {
        val groups = mutableListOf<ChangelogGroup>()
        var currentGroupTitle: String? = null
        var currentGroupSections = mutableListOf<ChangelogSection>()
        var currentTitle: String? = null
        var currentItems = mutableListOf<ChangelogItem>()
        var currentEmptyNote: String? = null
        var ignoringGroup = false
        var ignoringSection = false
        var inCodeBlock = false

        fun flushSection() {
            val title = currentTitle ?: return
            val hasContent = currentItems.isNotEmpty() || currentEmptyNote != null
            if (!ignoringGroup && !ignoringSection && hasContent) {
                currentGroupSections +=
                    ChangelogSection(
                        title = title,
                        items = currentItems.toList(),
                        emptyNote = currentEmptyNote,
                    )
            }
            currentItems = mutableListOf()
            currentEmptyNote = null
        }

        fun flushGroup() {
            flushSection()
            val groupTitle = currentGroupTitle
            if (!ignoringGroup && groupTitle != null && currentGroupSections.isNotEmpty()) {
                groups +=
                    ChangelogGroup(
                        title = groupTitle,
                        sections = currentGroupSections.toList(),
                    )
            }
            currentGroupSections = mutableListOf()
            currentTitle = null
        }
    }

    fun parse(rawMarkdown: String): ReleaseChangelog {
        if (rawMarkdown.isBlank()) {
            return ReleaseChangelog(emptyList())
        }

        val state = ParserState()
        for (rawLine in rawMarkdown.lineSequence()) {
            processLine(rawLine.trim(), state)
        }
        state.flushGroup()
        val flatSections =
            state.groups.flatMap { group ->
                group.sections.map { section ->
                    if (section.title.isBlank() && group.title.isNotBlank()) {
                        section.copy(title = group.title)
                    } else {
                        section
                    }
                }
            }
        return ReleaseChangelog(
            sections = flatSections,
            groups = state.groups.toList(),
        )
    }

    private fun processLine(
        trimmed: String,
        state: ParserState,
    ) {
        when {
            trimmed.startsWith("```") -> state.inCodeBlock = !state.inCodeBlock
            state.inCodeBlock -> Unit
            else -> {
                val headingMatch = HEADING_REGEX.matchEntire(trimmed)
                if (headingMatch != null) {
                    val level = headingMatch.groupValues[1].length
                    val headingText = cleanInlineNoise(headingMatch.groupValues[2])
                    if (level <= 2) {
                        state.flushGroup()
                        state.ignoringGroup = shouldIgnoreSection(headingText)
                        state.ignoringSection = state.ignoringGroup
                        state.currentGroupTitle = headingText
                        state.currentTitle = ""
                    } else {
                        state.flushSection()
                        state.ignoringSection = state.ignoringGroup || shouldIgnoreSection(headingText)
                        state.currentTitle = headingText
                    }
                } else if (!state.ignoringGroup && !state.ignoringSection && !shouldSkipLine(trimmed)) {
                    processBodyLine(trimmed, state)
                }
            }
        }
    }

    private fun processBodyLine(
        trimmed: String,
        state: ParserState,
    ) {
        val bulletMatch = BULLET_REGEX.matchEntire(trimmed)
        when {
            bulletMatch != null -> {
                if (state.currentGroupTitle == null) state.currentGroupTitle = ""
                if (state.currentTitle == null) state.currentTitle = ""
                parseItem(bulletMatch.groupValues[1])?.let { state.currentItems += it }
            }

            isItalicEmptyNote(trimmed) -> {
                if (state.currentGroupTitle == null) state.currentGroupTitle = ""
                if (state.currentTitle == null) state.currentTitle = ""
                if (state.currentItems.isEmpty()) {
                    state.currentEmptyNote = trimmed.removeSurrounding("_").trim()
                }
            }

            state.currentTitle != null && trimmed.isNotEmpty() -> {
                parseItem(trimmed)?.let { state.currentItems += it }
            }
        }
    }

    private fun isItalicEmptyNote(trimmed: String): Boolean =
        trimmed.startsWith("_") &&
            trimmed.endsWith("_") &&
            trimmed.length > 2

    private fun shouldIgnoreSection(heading: String): Boolean {
        val normalized = heading.lowercase().trim()
        return IGNORED_SECTION_PREFIXES.any { normalized.startsWith(it) }
    }

    private fun shouldSkipLine(trimmed: String): Boolean =
        trimmed.isEmpty() ||
            trimmed == "---" ||
            trimmed == "***" ||
            trimmed.startsWith(">") ||
            trimmed.startsWith("|") ||
            FULL_CHANGELOG_LINE_REGEX.matches(trimmed)

    private fun parseItem(rawText: String): ChangelogItem? {
        val cleaned =
            cleanInlineNoise(rawText)
                .replace(PR_AUTHOR_SUFFIX_REGEX, "")
                .replace(COMMIT_HASH_LINK_PREFIX_REGEX, "")
                .trim()
        if (cleaned.isEmpty()) return null

        val conventionalMatch = CONVENTIONAL_PREFIX_REGEX.matchEntire(cleaned)
        val (tag, bodyText) =
            if (conventionalMatch != null) {
                val type = conventionalMatch.groupValues[1]
                val scope = conventionalMatch.groupValues[2]
                val rest = conventionalMatch.groupValues[3].trim()
                val formattedBody = if (scope.isNotBlank()) "**$scope:** $rest" else rest
                type to formattedBody
            } else {
                null to cleaned
            }

        return parseSpans(bodyText)
            .takeIf { it.isNotEmpty() }
            ?.let { ChangelogItem(spans = it, tag = tag) }
    }

    private fun cleanInlineNoise(text: String): String =
        text
            .replace(MARKDOWN_LINK_REGEX) { match -> match.groupValues[1] }
            .replace("()", "")
            .trim()

    internal fun parseSpans(text: String): List<ChangelogSpan> {
        if (text.isEmpty()) return emptyList()
        val spans = mutableListOf<ChangelogSpan>()
        var cursor = 0

        for (match in INLINE_TOKEN_REGEX.findAll(text)) {
            if (match.range.first > cursor) {
                spans += ChangelogSpan.Plain(text.substring(cursor, match.range.first))
            }
            val bold = match.groups[1]?.value
            val code = match.groups[2]?.value
            when {
                bold != null -> spans += ChangelogSpan.Bold(bold)
                code != null -> spans += ChangelogSpan.Code(code)
            }
            cursor = match.range.last + 1
        }

        if (cursor < text.length) {
            spans += ChangelogSpan.Plain(text.substring(cursor))
        }
        return spans
    }
}
