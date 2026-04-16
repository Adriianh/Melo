package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetSearchHistoryUseCase(
    private val repository: SearchHistoryRepository
) {
    operator fun invoke(query: String, limit: Long = 5): Flow<List<String>> {
        return repository.getRecentQueries(query, limit)
    }
}