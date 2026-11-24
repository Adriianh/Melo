package com.github.adriianh.core.domain.model.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseChangelogParserTest {
    @Test
    fun `parses nightly release body stripping build info cli and assets`() {
        val raw =
            """
            ## Build information

            | | |
            | :--- | :--- |
            | **Version** | `2.5.0-nightly.20261123.0100` |
            | **Built from** | [`a1b2c3d`](https://github.com/Adriianh/Melo/commit/a1b2c3d) on `master` |

            > [!WARNING]
            > This is an automated development build, not an official release.

            ## Changes since the last nightly

            - [`a1b2c3d`](https://github.com/Adriianh/Melo/commit/a1b2c3d) feat(player): add gapless playback
            - fix(ui): resolve mini player overlap

            ## Changes since the last stable release (`v2.5.0`)

            - [`a1b2c3d`](https://github.com/Adriianh/Melo/commit/a1b2c3d) feat(player): add gapless playback
            - perf: cache home feed

            ---

            ### CLI (TUI)

            ```bash
            curl -fsSL https://example.com/install.sh | bash
            ```

            ### Downloads & Assets

            | Platform | Package / Type | File |
            | :--- | :--- | :--- |
            | Android | APK | [melo.apk](https://example.com/melo.apk) |
            """.trimIndent()

        val changelog = ReleaseChangelogParser.parse(raw)

        assertEquals(2, changelog.sections.size)
        assertEquals("Changes since the last nightly", changelog.sections[0].title)
        assertEquals(2, changelog.sections[0].items.size)
        assertEquals("feat", changelog.sections[0].items[0].tag)
        assertEquals(
            listOf(ChangelogSpan.Bold("player:"), ChangelogSpan.Plain(" add gapless playback")),
            changelog.sections[0].items[0].spans,
        )
        assertEquals("fix", changelog.sections[0].items[1].tag)

        assertEquals("Changes since the last stable release (`v2.5.0`)", changelog.sections[1].title)
        assertEquals(2, changelog.sections[1].items.size)
        assertEquals("perf", changelog.sections[1].items[1].tag)
    }

    @Test
    fun `parses stable github generated release notes and strips full changelog link`() {
        val raw =
            """
            ## What's Changed
            * feat(library): offline playlist sync by @Adriianh in https://github.com/Adriianh/Melo/pull/42
            * Support `FLAC` metadata tags by @Adriianh in https://github.com/Adriianh/Melo/pull/43

            **Full Changelog**: https://github.com/Adriianh/Melo/compare/v2.4.0...v2.5.0
            """.trimIndent()

        val changelog = ReleaseChangelogParser.parse(raw)

        assertEquals(1, changelog.sections.size)
        val items = changelog.sections[0].items
        assertEquals(2, items.size)
        assertEquals("feat", items[0].tag)
        assertEquals(
            listOf(ChangelogSpan.Bold("library:"), ChangelogSpan.Plain(" offline playlist sync")),
            items[0].spans,
        )
        assertEquals(null, items[1].tag)
        assertEquals(
            listOf(
                ChangelogSpan.Plain("Support "),
                ChangelogSpan.Code("FLAC"),
                ChangelogSpan.Plain(" metadata tags"),
            ),
            items[1].spans,
        )
    }

    @Test
    fun `preserves empty note in nightly section when no changes occurred`() {
        val raw =
            """
            ## Changes since the last nightly

            _No changes since the previous nightly build._
            """.trimIndent()

        val changelog = ReleaseChangelogParser.parse(raw)

        assertEquals(1, changelog.sections.size)
        assertTrue(changelog.sections[0].items.isEmpty())
        assertEquals("No changes since the previous nightly build.", changelog.sections[0].emptyNote)
    }

    @Test
    fun `returns empty changelog for blank input`() {
        assertTrue(ReleaseChangelogParser.parse("   \n  ").isEmpty)
    }
}
