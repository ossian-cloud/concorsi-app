package cloud.ossian.app.data

import android.content.Context
import cloud.ossian.app.BuildConfig
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

const val FEED_URL = "https://ossian.cloud/concorsi/app/v1.json"
const val SITE = "https://ossian.cloud/concorsi/"
private const val UA = "OssianApp/${BuildConfig.VERSION_NAME} (+https://ossian.cloud)"

private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

/** The open calls (downloaded, cached in a file) and the user's own data (saved calls, alerts), all on the phone. */
class Repository(context: Context) {
    private val dir = context.filesDir
    private val cache = File(dir, "concorsi-v1.json")
    private val validator = File(dir, "concorsi-v1.validator")  // "ETag\tvalue" or "Last-Modified\tvalue"
    private val user = File(dir, "user.json")

    fun cached(): Feed? = runCatching { json.decodeFromString<Feed>(cache.readText()) }.getOrNull()

    /** Downloads the feed if it changed; returns the fresh one, or null when unchanged. Throws on network errors. */
    fun refresh(): Feed? {
        if (System.getProperty("ossian.offline") == "true") return null  // JVM screenshot tests: keep the fixture feed
        val conn = URL(FEED_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("User-Agent", UA)
        conn.setRequestProperty("Accept-Encoding", "gzip")
        if (validator.exists() && cache.exists()) {
            val (kind, value) = validator.readText().split('\t', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            conn.setRequestProperty(if (kind == "ETag") "If-None-Match" else "If-Modified-Since", value)
        }
        try {
            if (conn.responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) return null
            if (conn.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${conn.responseCode}")
            val stream = if (conn.contentEncoding == "gzip") GZIPInputStream(conn.inputStream) else conn.inputStream
            val text = stream.bufferedReader().use { it.readText() }
            val feed = json.decodeFromString<Feed>(text)  // parse before replacing the cache
            File(dir, "concorsi-v1.json.tmp").apply { writeText(text); renameTo(cache) }
            val tag = conn.getHeaderField("ETag")?.let { "ETag\t$it" }
                ?: conn.getHeaderField("Last-Modified")?.let { "Last-Modified\t$it" }
            if (tag != null) validator.writeText(tag) else validator.delete()
            return feed
        } finally {
            conn.disconnect()
        }
    }

    @Synchronized
    fun userData(): UserData = runCatching { json.decodeFromString<UserData>(user.readText()) }.getOrDefault(UserData())

    @Synchronized
    fun updateUser(change: (UserData) -> UserData): UserData {
        val next = change(userData())
        File(dir, "user.json.tmp").apply { writeText(json.encodeToString(UserData.serializer(), next)); renameTo(user) }
        return next
    }
}
