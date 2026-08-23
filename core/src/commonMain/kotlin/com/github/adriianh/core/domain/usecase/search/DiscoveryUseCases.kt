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

class GetArtistRadioUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(artistId: String): List<Track> = repository.getArtistRadio(artistId)
}

class GetRelatedTracksUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(videoId: String): List<Track> = repository.getRelated(videoId)
}

class SearchVideosUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(query: String): List<Track> {
        if (query.isBlank()) return emptyList()
        return repository.searchVideos(query)
    }
}

class SearchSummaryUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(query: String): List<HomeSection> {
        if (query.isBlank()) return emptyList()
        return repository.searchSummary(query)
    }
}