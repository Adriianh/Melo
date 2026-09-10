package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.repository.SearchHistoryRepository

class DeleteSearchQueryUseCase(
    private val repository: SearchHistoryRepository
) {
    suspend operator fun invoke(query: String) {
        repository.deleteQuery(query)
    }
}