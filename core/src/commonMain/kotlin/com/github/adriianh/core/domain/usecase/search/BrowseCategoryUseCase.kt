package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.BrowseCategoryResult
import com.github.adriianh.core.domain.repository.MusicRepository

class BrowseCategoryUseCase(
    private val repository: MusicRepository,
) {
    suspend operator fun invoke(browseId: String, params: String?): BrowseCategoryResult? =
        repository.browseCategory(browseId, params)
}