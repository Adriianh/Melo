package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.repository.LoginRepository
import com.github.adriianh.innertube.YouTube

class InnerTubeLoginRepository : LoginRepository {

    override fun setSessionCookies(cookies: String?) {
        YouTube.cookie = cookies
        YouTube.useLoginForBrowse = !cookies.isNullOrBlank()
    }

    override suspend fun verifySession(): String? {
        val result = YouTube.accountInfo()
        result.onSuccess { info ->
            println("[Melo Auth] Successfully verified session for user: ${info.name}")
        }.onFailure { error ->
            println("[Melo Auth] verifySession failed: ${error.javaClass.simpleName}: ${error.message}")
        }
        return result.getOrNull()?.name
    }
}