package com.igor.istreamingtv.data.remote

import com.igor.istreamingtv.data.remote.TmdbClient.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
private data class StreamResponse(
    val streams: List<StreamItem> = emptyList()
)

@Serializable
private data class StreamItem(
    val name: String? = null,
    val title: String? = null,
    val url: String? = null
)

object StreamPicker {

    const val ADDON_BASE_URL =
        "https://hdhub.thevolecitor.qzz.io/eyJ0b3Jib3giOiJ1bnNldCIsInF1YWxpdGllcyI6IjEwODBwIiwic29ydCI6ImRlc2MiLCJjYXRhbG9ncyI6IiJ9"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    data class Candidate(
        val url: String,
        val quality: Int,
        val qualityLabel: String,
        val validated: Boolean = false
    )

    private fun deviceRank(quality: Int): Int = when (quality) {
        3 -> 4
        2 -> 3
        1 -> 2
        0 -> 1
        else -> 0
    }

    suspend fun getCandidates(
        type: String,
        imdbId: String,
        season: Int = -1,
        episode: Int = -1
    ): List<Candidate> = withContext(Dispatchers.IO) {
        try {
            val path = if (type == "series") "series/$imdbId:$season:$episode" else "movie/$imdbId"
            val request = Request.Builder()
                .url("$ADDON_BASE_URL/stream/$path.json")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            response.close()
            if (body.isNullOrBlank()) return@withContext emptyList<Candidate>()

            // ✅ Type-safe parsing
            val parsed = json.decodeFromString<StreamResponse>(body)

            parsed.streams.mapNotNull { item ->
                val url = item.url ?: return@mapNotNull null
                if (!url.startsWith("http")) return@mapNotNull null

                val text = buildString {
                    append(item.name ?: "")
                    append(' ')
                    append(item.title ?: "")
                }
                val (q, label) = parseQuality(text)
                Candidate(url, q, label)
            }
                .distinctBy { it.url }
                .sortedByDescending { deviceRank(it.quality) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun prepare(candidates: List<Candidate>, maxChecks: Int = 5): List<Candidate> {
        if (candidates.isEmpty()) return candidates
        return withContext(Dispatchers.IO) {
            val checked = candidates.take(maxChecks).map { c ->
                async { c.copy(validated = probe(c.url)) }
            }.awaitAll()
            val rest = candidates.drop(maxChecks)
            (checked + rest).sortedWith(
                compareByDescending<Candidate> { it.validated }
                    .thenByDescending { deviceRank(it.quality) }
            )
        }
    }

    private fun probe(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-8191")
                .header("Connection", "close")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val code = response.code
            response.close()
            code in 200..399
        } catch (_: Exception) {
            false
        }
    }

    private fun parseQuality(text: String): Pair<Int, String> {
        val t = text.lowercase()
        return when {
            t.contains("2160") || t.contains("4k") || t.contains("uhd") -> 4 to "4K"
            t.contains("1080") -> 3 to "1080p"
            t.contains("720") -> 2 to "720p"
            t.contains("480") || t.contains("360") -> 1 to "480p"
            else -> 0 to "AUTO"
        }
    }

    fun getCandidatesBlocking(type: String, imdbId: String, season: Int, episode: Int): List<Candidate> =
        runBlocking { getCandidates(type, imdbId, season, episode) }
}
