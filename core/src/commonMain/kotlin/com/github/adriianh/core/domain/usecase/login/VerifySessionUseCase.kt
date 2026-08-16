package com.github.adriianh.core.domain.usecase.login

import com.github.adriianh.core.domain.repository.LoginRepository

class VerifySessionUseCase(private val repository: LoginRepository) {
    suspend operator fun invoke(): String? = repository.verifySession()
}