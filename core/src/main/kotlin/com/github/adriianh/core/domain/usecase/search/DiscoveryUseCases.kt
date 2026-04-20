package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.repository.MusicRepository

class GetHomeUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(): List<SearchResult.ArtistSection> = repository.getHome()
}

class GetExploreUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(): List<SearchResult.ArtistSection> = repository.getExplore()
}

class GetTrendingUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(): List<Track> = repository.getTrending()
}

class GetRadioUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(videoId: String): List<Track> = repository.getRadio(videoId)
}