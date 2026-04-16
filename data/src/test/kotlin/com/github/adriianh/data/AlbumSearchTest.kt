package com.github.adriianh.data
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.models.AlbumItem
import kotlinx.coroutines.runBlocking
fun main() = runBlocking {
    val search = YouTube.search("Rick Ross Album", YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
    val album = search?.items?.filterIsInstance<AlbumItem>()?.firstOrNull()
    println("Search album id: ${album?.browseId}")
    if (album != null) {
        val details = YouTube.album(album.browseId).getOrNull()
        println("Album songs size: ${details?.songs?.size}")
        println("Other versions size: ${details?.otherVersions?.size}")
    }
}
