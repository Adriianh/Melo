package com.github.adriianh.innertube.models.response

import com.github.adriianh.innertube.models.AccountChannel
import com.github.adriianh.innertube.models.Thumbnails
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Response of `GET https://music.youtube.com/getAccountSwitcherEndpoint`.
 *
 * The body is prefixed with `)]}'` and wraps the payload in `{"code", "data"}`.
 * Channels live under `data.actions[].getMultiPageMenuAction.menu.multiPageMenuRenderer
 * .sections[].accountSectionListRenderer.contents[].accountItemSectionRenderer.contents[].accountItem`.
 *
 * Every Google account signed in on the session gets its own section; only the
 * section headed by `googleAccountHeaderRenderer` belongs to the current account.
 */
@Serializable
data class AccountSwitcherResponse(
    val code: String? = null,
    val data: Data? = null,
) {
    @Serializable
    data class Data(
        val actions: List<Action> = emptyList(),
    )

    @Serializable
    data class Action(
        val getMultiPageMenuAction: GetMultiPageMenuAction? = null,
    )

    @Serializable
    data class GetMultiPageMenuAction(
        val menu: Menu? = null,
    )

    @Serializable
    data class Menu(
        val multiPageMenuRenderer: MultiPageMenuRenderer? = null,
    )

    @Serializable
    data class MultiPageMenuRenderer(
        val sections: List<Section> = emptyList(),
    )

    @Serializable
    data class Section(
        val accountSectionListRenderer: AccountSectionListRenderer? = null,
    )

    @Serializable
    data class AccountSectionListRenderer(
        val contents: List<SectionContent> = emptyList(),
        val header: SectionHeader? = null,
    )

    @Serializable
    data class SectionHeader(
        val googleAccountHeaderRenderer: GoogleAccountHeaderRenderer? = null,
    )

    @Serializable
    data class GoogleAccountHeaderRenderer(
        val name: Text? = null,
        val email: Text? = null,
    )

    @Serializable
    data class SectionContent(
        val accountItemSectionRenderer: AccountItemSectionRenderer? = null,
    )

    @Serializable
    data class AccountItemSectionRenderer(
        val contents: List<ItemContent> = emptyList(),
    )

    @Serializable
    data class ItemContent(
        val accountItem: AccountItem? = null,
    )

    @Serializable
    data class AccountItem(
        val accountName: Text? = null,
        val accountPhoto: Thumbnails? = null,
        val isSelected: Boolean = false,
        val serviceEndpoint: ServiceEndpoint? = null,
        val accountByline: Text? = null,
        val channelHandle: Text? = null,
    ) {
        fun toAccountChannel(): AccountChannel? {
            val handle = channelHandle?.text
            val displayName = accountName?.text ?: handle ?: return null
            val tokens =
                serviceEndpoint
                    ?.selectActiveIdentityEndpoint
                    ?.supportedTokens
                    .orEmpty()
            return AccountChannel(
                name = displayName,
                handle = handle,
                byline = accountByline?.text,
                avatarUrl = accountPhoto?.thumbnails?.lastOrNull()?.url,
                pageId = tokens.firstNotNullOfOrNull { it.pageIdToken?.pageId },
                obfuscatedGaiaId =
                    tokens.firstNotNullOfOrNull {
                        it.accountStateToken?.obfuscatedGaiaId
                    },
                isSelected = isSelected,
            )
        }
    }

    @Serializable
    data class Text(
        val runs: List<TextRun> = emptyList(),
    ) {
        val text: String?
            get() = runs.joinToString(separator = "") { it.text }.takeIf { it.isNotBlank() }
    }

    @Serializable
    data class TextRun(
        val text: String = "",
    )

    @Serializable
    data class ServiceEndpoint(
        val selectActiveIdentityEndpoint: SelectActiveIdentityEndpoint? = null,
    )

    @Serializable
    data class SelectActiveIdentityEndpoint(
        val supportedTokens: List<SupportedToken> = emptyList(),
    )

    @Serializable
    data class SupportedToken(
        val pageIdToken: PageIdToken? = null,
        val accountStateToken: AccountStateToken? = null,
    )

    @Serializable
    data class PageIdToken(
        val pageId: String,
    )

    @Serializable
    data class AccountStateToken(
        val obfuscatedGaiaId: String,
    )

    /** Channels of the current Google account only. */
    fun toAccountChannels(): List<AccountChannel> =
        data
            ?.actions
            .orEmpty()
            .mapNotNull { it.getMultiPageMenuAction?.menu?.multiPageMenuRenderer }
            .flatMap { it.sections }
            .mapNotNull { it.accountSectionListRenderer }
            .firstOrNull { it.header?.googleAccountHeaderRenderer != null }
            ?.contents
            .orEmpty()
            .mapNotNull { it.accountItemSectionRenderer }
            .flatMap { it.contents }
            .mapNotNull { it.accountItem?.toAccountChannel() }

    companion object {
        private const val XSSI_PREFIX = ")]}'"

        private val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }

        /** Decodes a raw body, stripping the anti-XSSI prefix when present. */
        fun parse(raw: String): AccountSwitcherResponse =
            json.decodeFromString(
                serializer(),
                raw.trimStart().removePrefix(XSSI_PREFIX).trimStart(),
            )
    }
}
