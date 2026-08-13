package com.igor.istreamingtv.data.remote

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * PAMETAN IZBOR STREAM-A — bez ručnog biranja.
 * Optimizovan za slabe uređaje: manji probe, brži timeout-i.
 */
object StreamPicker {

    const val ADDON_BASE_URL =
        "https://hdhub.thevolecitor.qzz.io/eyJ0b3Jib3giOiJ1bnNldCIsInF1YWxpdGllcyI6IjIxNjBwLDEwODBwLDcyMHAiLCJzb3J0IjoiZGVzYyIsImNhdGFsb2dzIjoiIn0"

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

            val obj = JsonParser.parseString(body).asJsonObject
            val arr = obj.getAsJsonArray("streams") ?: return@withContext emptyList<Candidate>()

            arr.mapNotNull { el ->
                val o = el.asJsonObject
                val url = o.get("url")?.takeIf { !it.isJsonNull }?.asString
                    ?: return@mapNotNull null
                if (!url.startsWith("http")) return@mapNotNull null

                val text = buildString {
                    append(o.get("name")?.takeIf { !it.isJsonNull }?.asString ?: "")
                    append(' ')
                    append(o.get("title")?.takeIf { !it.isJsonNull }?.asString ?: "")
                }
                val (q, label) = parseQuality(text)
                Candidate(url, q, label)
            }
                .distinctBy { it.url }
                .sortedByDescending { it.quality }
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
                    .thenByDescending { it.quality }
            )
        }
    }

    private fun probe(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-8191")  // 8KB umesto 1B — pouzdaniji probe
                .header("Connection", "close")     // Bez keep-alive overhead
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
