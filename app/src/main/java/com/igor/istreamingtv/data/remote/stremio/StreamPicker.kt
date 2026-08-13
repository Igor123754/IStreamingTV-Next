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
 * 1) Povuče stream-ove sa addona
 * 2) Odbaci torrente/magnete (samo https://)
 * 3) Sortira po kvalitetu: 4K → 1080p → 720p → 480p
 * 4) Validira u pozadini (HTTP probe) da "Gledaj" ima najbolji RADNI stream
 */
object StreamPicker {

    // 🔧🔧🔧 ADDON ZA STRIMOVANJE (HTTPS, bez torenata) 🔧🔧🔧
    // Zameni ovu adresu svojim addonom — sve ostalo radi automatski.
    const val ADDON_BASE_URL = "https://v3-cinemeta.strem.io"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    data class Candidate(
        val url: String,
        val quality: Int,          // 4 = 4K, 3 = 1080p, 2 = 720p, 1 = 480p, 0 = nepoznato
        val qualityLabel: String,
        val validated: Boolean = false
    )

    /** Povuci + filtriraj (bez torenata) + sortira po kvalitetu */
    suspend fun getCandidates(
        type: String,               // "movie" | "series"
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
                // ❌ TORRENTI NAPOLJE — samo direktan HTTPS
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

    /**
     * Priprema u pozadini: validira prvih N kandidata paralelno.
     * Redosled na kraju: prvo VALIDIRANI (po kvalitetu), pa ostali kao rezerva.
     */
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

    /** HTTP probe: da li stream odgovara (2xx / 3xx / 206) */
    private fun probe(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-1")
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

    /** Sinhrona verzija za plejer (sledeća epizoda) */
    fun getCandidatesBlocking(type: String, imdbId: String, season: Int, episode: Int): List<Candidate> =
        runBlocking { getCandidates(type, imdbId, season, episode) }
}
