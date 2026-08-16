package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.HomeFeed
import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.MusicRepository

class GetHomeUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(
        params: String? = null,
        continuation: String? = null
    ): HomeFeed =
        repository.getHomeFeed(params, continuation)
}

class GetExploreUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(): List<HomeSection> = repository.getExplore()
}

class GetChartsUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(): List<HomeSection> = repository.getCharts()
}

class GetTrendingUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(): List<Track> = repository.getTrending()
}

class GetRadioUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(videoId: String): List<Track> = repository.getRadio(videoId)
}