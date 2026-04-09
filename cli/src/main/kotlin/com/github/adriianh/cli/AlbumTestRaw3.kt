package com.github.adriianh.cli
import com.github.adriianh.data.provider.music.InnerTubeMusicProvider
import kotlinx.coroutines.runBlocking
import io.ktor.client.plugins.*
fun main() {
    runBlocking {
        val provider = InnerTubeMusicProvider()
        val search = provider.searchAlbums("Billionaire")
        println("Found albums: ${search.size}")
        val firstId = search.first().id
        println("First album ID: $firstId")
        try {
            val details = provider.getAlbumDetails(firstId)
            println("Details title: ${details?.title}")
            println("Songs inside: ${details?.songs?.size}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
