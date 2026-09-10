
package com.github.adriianh.data.repository
import com.github.adriianh.core.util.MeloDispatchers

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.github.adriianh.core.domain.repository.SearchHistoryRepository
import com.github.adriianh.data.local.MeloDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

import com.github.adriianh.core.platform.currentTimeSeconds

class SearchHistoryRepositoryImpl(database: MeloDatabase) : SearchHistoryRepository {
    private val queries = database.searchHistoryQueries

    override fun getRecentQueries(query: String, limit: Long): Flow<List<String>> {
        return queries.getRecentQueries(queryPattern = query, limit = limit)
            .asFlow()
            .mapToList(MeloDispatchers.IO)
    }

    override suspend fun saveQuery(query: String) {
        withContext(MeloDispatchers.IO) {
            queries.insertQuery(query, currentTimeSeconds() * 1000L)
            queries.deleteOldestItems()
        }
    }

    override suspend fun deleteQuery(query: String) {
        withContext(MeloDispatchers.IO) {
            queries.deleteQuery(query)
        }
    }
}