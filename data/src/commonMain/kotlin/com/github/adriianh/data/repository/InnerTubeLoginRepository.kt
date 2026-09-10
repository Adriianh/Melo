package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.repository.LoginRepository
import com.github.adriianh.innertube.YouTube

class InnerTubeLoginRepository : LoginRepository {

    override fun setSessionCookies(cookies: String?) {
        YouTube.cookie = cookies
        YouTube.useLoginForBrowse = !cookies.isNullOrBlank()
    }

    override suspend fun verifySession(): String? =
        YouTube.accountInfo().getOrNull()?.name
}