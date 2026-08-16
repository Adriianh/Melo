package com.github.adriianh.core.domain.usecase.login

import com.github.adriianh.core.domain.repository.LoginRepository

class SetSessionCookiesUseCase(private val repository: LoginRepository) {
    operator fun invoke(cookies: String?) {
        repository.setSessionCookies(cookies)
    }
}