package dump
import com.github.adriianh.innertube.YouTube
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
class DumpTest {
    @Test
    fun dump() = runBlocking {
        YouTube.dumpAlbum("MPREb_oNAdr9eUOfS")
        YouTube.dumpPlaylist("RDCLAK5uy_mHAEb33pqvgdtuxsemicZNu-5w6rLRweo")
    }
}
