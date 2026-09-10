package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.repository.SearchHistoryRepository

class SaveSearchQueryUseCase(
    private val repository: SearchHistoryRepository
) {
    suspend operator fun invoke(query: String) {
        if (query.isNotBlank()) {
            repository.saveQuery(query)
        }
    }
}