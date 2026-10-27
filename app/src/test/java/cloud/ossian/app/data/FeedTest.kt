package cloud.ossian.app.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

class FeedTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test fun stemsAndFolds() {
        assertEquals(listOf("infermier"), terms("Infermieri"))
        assertEquals(listOf("citt"), terms("Città"))  // stemmed like the site: 5+ letters lose the last vowel
        val c = Call(id = "1", t = "Infermiere a tempo indeterminato", e = "ASL Città di Torino", r = listOf("piemonte"))
        assertTrue(Filter(words = "infermieri torino", region = "piemonte").matches(c))
        assertTrue(!Filter(region = "lazio").matches(c))
    }

    /** Parses a real export: ./gradlew test -Dfeed=/path/to/v1.json (skipped without it). */
    @Test fun parsesRealFeed() {
        val path = System.getProperty("feed") ?: ""
        assumeTrue(path.isNotEmpty() && File(path).exists())
        val feed = json.decodeFromString<Feed>(File(path).readText())
        assertTrue(feed.calls.size > 100)
        val open = openSorted(feed.calls, LocalDateTime.now())
        assertTrue(open.isNotEmpty())
        val nurses = open.filter { Filter(profile = "infermieri").matches(it) }
        println("calls ${feed.calls.size}, open ${open.size}, infermieri ${nurses.size}, first ${open.first().t} ${day(open.first().s)}")
    }
}
