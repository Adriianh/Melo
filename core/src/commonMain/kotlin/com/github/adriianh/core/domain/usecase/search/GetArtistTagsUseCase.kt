package com.github.adriianh.core.domain.usecase.search
import com.github.adriianh.core.domain.provider.DiscoveryProvider
class GetArtistTagsUseCase(
    private val discoveryProvider: DiscoveryProvider
) {
    suspend operator fun invoke(artist: String): List<String> {
        return discoveryProvider.getGenres(artist)
    }
}
