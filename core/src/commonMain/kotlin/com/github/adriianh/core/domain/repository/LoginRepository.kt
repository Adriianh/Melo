package com.github.adriianh.core.domain.repository

interface LoginRepository {
    fun setSessionCookies(cookies: String?)
    suspend fun verifySession(): String?
}