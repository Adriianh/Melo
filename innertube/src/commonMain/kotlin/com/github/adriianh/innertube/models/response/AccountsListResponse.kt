package com.github.adriianh.innertube.models.response

import com.github.adriianh.innertube.models.AccountChannel
import com.github.adriianh.innertube.models.Thumbnails
import kotlinx.serialization.Serializable

@Serializable
data class AccountsListResponse(
    val actions: List<Action> = emptyList(),
) {
    @Serializable
    data class Action(
        val updateChannelSwitcherPageAction: UpdateChannelSwitcherPageAction? = null,
    ) {
        @Serializable
        data class UpdateChannelSwitcherPageAction(
            val page: Page? = null,
        ) {
            @Serializable
            data class Page(
                val channelSwitcherPageRenderer: ChannelSwitcherPageRenderer? = null,
            ) {
                @Serializable
                data class ChannelSwitcherPageRenderer(
                    val contents: List<Content> = emptyList(),
                ) {
                    @Serializable
                    data class Content(
                        val accountItemRenderer: AccountItemRenderer? = null,
                    ) {
                        @Serializable
                        data class AccountItemRenderer(
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
                    }
                }
            }
        }
    }

    fun toAccountChannels(): List<AccountChannel> =
        actions
            .mapNotNull { it.updateChannelSwitcherPageAction }
            .flatMap {
                it.page
                    ?.channelSwitcherPageRenderer
                    ?.contents
                    .orEmpty()
            }.mapNotNull { it.accountItemRenderer?.toAccountChannel() }

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
