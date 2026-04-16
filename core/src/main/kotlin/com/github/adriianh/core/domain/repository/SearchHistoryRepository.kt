package com.github.adriianh.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun getRecentQueries(query: String, limit: Long = 5): Flow<List<String>>
    suspend fun saveQuery(query: String)
    suspend fun deleteQuery(query: String)
}