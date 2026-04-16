package com.github.adriianh.innertube
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import java.io.File
class GetAlbumRunner {
    @Test
    fun dumpData(): Unit = runBlocking {
        val client = YouTube.javaClass.getDeclaredField("innerTube").apply { isAccessible = true }.get(null) as InnerTube
        val resp = client.browse(com.github.adriianh.innertube.models.YouTubeClient.WEB_REMIX, "VLOLAK5uy_lrCrcAdxFG4aMzMrebs7o9TU384xyF240").bodyAsText()
        File("test_playlist.json").writeText(resp)
    }
}
