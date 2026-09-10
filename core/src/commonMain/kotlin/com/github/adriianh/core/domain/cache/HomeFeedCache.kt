package com.github.adriianh.core.domain.cache

import com.github.adriianh.core.domain.model.HomeFeed

interface HomeFeedCache {
    fun getSync(): HomeFeed?
    suspend fun get(): HomeFeed?
    suspend fun save(feed: HomeFeed)
}