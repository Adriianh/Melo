package com.github.adriianh.cli
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.search.GetEntityDetailsUseCase
import com.github.adriianh.data.provider.music.InnerTubeMusicProvider
import com.github.adriianh.data.provider.music.MergedMusicProvider
import kotlinx.coroutines.runBlocking
fun main() = runBlocking {
    val provider = MergedMusicProvider(listOf(InnerTubeMusicProvider()))
    val useCase = GetEntityDetailsUseCase(provider)
    // Let's pass a dummy album
    val album = SearchResult.Album("MPREb_B5b7D0K27zG", "Hood Billionaire", "Rick Ross", "2014", null)
    val result = runCatching { useCase.invoke(album) }
    if (result.isFailure) {
        println("FAILED: ${result.exceptionOrNull()}")
    } else {
        val loaded = result.getOrNull()
        if (loaded is SearchResult.Album) {
            println("SUCCESS, tracks: ${loaded.songs?.size}")
        }
    }
}
