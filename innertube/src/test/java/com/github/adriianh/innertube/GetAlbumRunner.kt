package com.github.adriianh.innertube
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
class GetAlbumRunner {
    @Test
    fun dumpData(): Unit {
        runBlocking {
            val res = YouTube.album("MPREb_B5b7D0K27zG")
            val albumPage = res.getOrThrow()
            throw Exception("Songs count for MPREb_B5b7D0K27zG: ${albumPage.songs.size}")
        }
    }
}
