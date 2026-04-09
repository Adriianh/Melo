package com.github.adriianh.cli
import com.github.adriianh.data.provider.music.InnerTubeMusicProvider
import kotlinx.coroutines.runBlocking
fun main() {
    runBlocking {
        val provider = InnerTubeMusicProvider()
        val queries = listOf("Bad Bunny", "Taylor Swift", "Drake", "Queen")
        for (query in queries) {
            val albums = provider.searchAlbums(query).take(5)
            for (album in albums) {
                try {
                    val details = provider.getAlbumDetails(album.id)
                    println("OK: ${album.title} -> ${details?.songs?.size ?: 0} tracks")
                } catch (e: Exception) {
                    println("FAIL: ${album.title} -> ${e.message}")
                }
            }
        }
    }
}
