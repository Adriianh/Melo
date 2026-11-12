package com.github.adriianh.innertube.models.response

import com.github.adriianh.innertube.models.AccountChannel
import com.github.adriianh.innertube.models.Thumbnails
import kotlinx.serialization.Serializable

/**
 * Response of `account/accounts_list`. Channels are listed under
 * `actions[].openPopupAction.popup.channelSwitcherRenderer.sections[]
 * .accountItemSectionRenderer.contents[].accountItem`.
 */
@Serializable
data class AccountsListResponse(
    val actions: List<Action> = emptyList(),
) {
    @Serializable
    data class Action(
        val openPopupAction: OpenPopupAction? = null,
    ) {
        @Serializable
        data class OpenPopupAction(
            val popup: Popup? = null,
        ) {
            @Serializable
            data class Popup(
                val channelSwitcherRenderer: ChannelSwitcherRenderer? = null,
            ) {
                @Serializable
                data class ChannelSwitcherRenderer(
                    val sections: List<Section> = emptyList(),
                ) {
                    @Serializable
                    data class Section(
                        val accountItemSectionRenderer: AccountItemSectionRenderer? = null,
                    ) {
                        @Serializable
                        data class AccountItemSectionRenderer(
                            val contents: List<Content> = emptyList(),
                        ) {
                            @Serializable
                            data class Content(
                                val accountItem: AccountItem? = null,
                            )
                        }
                    }
                }
            }
        }
    }

    @Serializable
    data class AccountItem(
        val accountName: SimpleText? = null,
        val accountPhoto: Thumbnails? = null,
        val serviceEndpoint: ServiceEndpoint? = null,
        val channelHandle: SimpleText? = null,
        val isSelected: Boolean = false,
    ) {
        fun toAccountChannel(): AccountChannel? {
            val displayName =
                accountName?.simpleText
                    ?: channelHandle?.simpleText
                    ?: return null
            val tokens =
                serviceEndpoint
                    ?.selectActiveIdentityEndpoint
                    ?.supportedTokens
                    .orEmpty()
            return AccountChannel(
                name = displayName,
                handle = channelHandle?.simpleText,
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

    fun toAccountChannels(): List<AccountChannel> =
        actions
            .mapNotNull { it.openPopupAction?.popup?.channelSwitcherRenderer }
            .flatMap { it.sections }
            .mapNotNull { it.accountItemSectionRenderer }
            .flatMap { it.contents }
            .mapNotNull { it.accountItem?.toAccountChannel() }

    @Serializable
    data class SimpleText(
        val simpleText: String,
    )

    @Serializable
    data class ServiceEndpoint(
        val selectActiveIdentityEndpoint: SelectActiveIdentityEndpoint? = null,
    ) {
        @Serializable
        data class SelectActiveIdentityEndpoint(
            val supportedTokens: List<SupportedToken> = emptyList(),
        ) {
            @Serializable
            data class SupportedToken(
                val pageIdToken: PageIdToken? = null,
                val accountStateToken: AccountStateToken? = null,
            ) {
                @Serializable
                data class PageIdToken(
                    val pageId: String,
                )

                @Serializable
                data class AccountStateToken(
                    val obfuscatedGaiaId: String,
                )
            }
        }
    }
}
