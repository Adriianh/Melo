package com.github.adriianh.core.domain.usecase.login

import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.repository.LoginRepository

class GetAccountProfilesUseCase(
    private val repository: LoginRepository,
) {
    suspend operator fun invoke(): Result<List<AccountProfile>> = repository.getAccountProfiles()
}
