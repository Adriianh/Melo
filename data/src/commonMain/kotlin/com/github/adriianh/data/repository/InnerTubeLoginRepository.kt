package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.repository.LoginRepository
import com.github.adriianh.innertube.YouTube

class InnerTubeLoginRepository : LoginRepository {
    override fun setSessionCookies(cookies: String?) {
        YouTube.cookie = cookies
        YouTube.pageId = null
        YouTube.useLoginForBrowse = !cookies.isNullOrBlank()
    }

    override fun setActivePageId(pageId: String?) {
        YouTube.pageId = pageId
    }

    override suspend fun verifySession(): String? = YouTube.accountInfo().getOrNull()?.name

    override suspend fun getAccountProfiles(): Result<List<AccountProfile>> =
        YouTube.accountChannels().map { channels ->
            channels.map { channel ->
                AccountProfile(
                    name = channel.name,
                    channelHandle = channel.handle,
                    avatarUrl = channel.avatarUrl,
                    pageId = channel.pageId,
                )
            }
        }
}
