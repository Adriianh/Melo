package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.AccountProfile

interface LoginRepository {
    fun setSessionCookies(cookies: String?)

    fun setActivePageId(pageId: String?)

    suspend fun verifySession(): String?

    suspend fun getAccountProfiles(): Result<List<AccountProfile>>
}
