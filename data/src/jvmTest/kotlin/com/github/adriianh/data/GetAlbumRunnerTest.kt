package com.github.adriianh.data
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.data.provider.music.InnerTubeMusicProvider
import com.github.adriianh.innertube.YouTube
import kotlinx.coroutines.runBlocking
fun main() = runBlocking {
    val id = "MPREb_B5b7D0K27zG"
    println("1. YouTube.album($id)")
    val result = YouTube.album(id).getOrNull()
    println("2. Result album: ${result?.album?.title}, songs: ${result?.songs?.size}")
    val fallback = InnerTubeMusicProvider()
    val providerResult = fallback.getAlbumDetails(id)
    println("3. ProviderResult: ${providerResult?.songs?.size} tracks")
}
