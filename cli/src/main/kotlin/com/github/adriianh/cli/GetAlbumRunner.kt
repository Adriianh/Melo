package com.github.adriianh.cli
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.data.provider.music.InnerTubeMusicProvider
import kotlinx.coroutines.runBlocking
fun main() = runBlocking {
    val id = "MPREb_B5b7D0K27zG"
    val fallback = InnerTubeMusicProvider()
    val providerResult = fallback.getAlbumDetails(id)
    println("3. ProviderResult: ${providerResult?.songs?.size} tracks")
}
