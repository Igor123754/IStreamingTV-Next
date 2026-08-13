package com.igor.istreamingtv.data.remote

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * TITLOVI — OpenSubtitles Stremio addon (srpski samo).
 * AUTO-SINHRONIZACIJA: meri trajanje titla i upoređuje sa trajanjem
 * filma/epizode (TMDB) — titl koji traje koliko i video = sinhronizovan.
 * (PAL 25fps titlovi traju ~4% kraće → automatski se odbacuju!)
 */
object SubtitleFetcher {

    const val ADDON_BASE_URL = "https://opensubtitles-v3.strem.io"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Svi mogući kodovi za srpski (addon vraća ISO 639-2: "scc"/"srp")
    private val serbianCodes = setOf("sr", "scc", "srp", "sr-rs", "serbian")

    data class SubtitleEntry(
        val url: String,
        val encoding: String?,
        val order: Int
    )

    /** Svi srpski titlovi (do 8), redosled kakav vraća addon */
    suspend fun getSerbianSubtitles(
        type: String,
        imdbId: String,
        season: Int = -1,
        episode: Int = -1
    ): List<SubtitleEntry> = withContext(Dispatchers.IO) {
        try {
            val path = if (type == "series") "series/$imdbId:$season:$episode" else "movie/$imdbId"
            val request = Request.Builder()
                .url("$ADDON_BASE_URL/subtitles/$path.json")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            response.close()
            if (body.isNullOrBlank()) return@withContext emptyList<SubtitleEntry>()

            val obj = JsonParser.parseString(body).asJsonObject
            val arr = obj.getAsJsonArray("subtitles") ?: return@withContext emptyList<SubtitleEntry>()

            val list = mutableListOf<SubtitleEntry>()
            arr.forEachIndexed { index, el ->
                val o = el.asJsonObject
                val url = o.str("url") ?: return@forEachIndexed
                val lang = (o.str("lang") ?: "").lowercase()
                if (lang !in serbianCodes) return@forEachIndexed
                // MikroDVD (.sub) ExoPlayer ne podržava dobro
                if (url.endsWith(".sub", ignoreCase = true)) return@forEachIndexed
                list.add(SubtitleEntry(url, o.str("SubEncoding"), index))
            }
            list.take(8)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * AUTO-SINHRONIZACIJA:
     * za prvih 4 kandidata preuzme titl, izmeri trajanje i uporedi
     * sa očekivanim trajanjem videa. Najbliži = prvi (default).
     */
    suspend fun rankBySync(
        entries: List<SubtitleEntry>,
        expectedSeconds: Int?
    ): List<SubtitleEntry> {
        if (entries.size < 2 || expectedSeconds == null || expectedSeconds <= 0) return entries
        return withContext(Dispatchers.IO) {
            val toCheck = entries.take(4)
            val rest = entries.drop(4)

            val scored = toCheck.map { entry ->
                async {
                    val duration = fetchDurationSeconds(entry.url)
                    val diff = duration?.let { abs(it - expectedSeconds) } ?: Int.MAX_VALUE
                    entry to diff
                }
            }.awaitAll()

            // Sinhronizovani prvi (najmanja razlika trajanja), ostali kao rezerva
            scored.sortedBy { it.second }.map { it.first } + rest
        }
    }

    /** Preuzme titl i vrati njegovo ukupno trajanje u sekundama */
    private fun fetchDurationSeconds(url: String): Int? {
        return try {
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val text = response.body?.string()
            response.close()
            if (text.isNullOrBlank()) null else parseMaxSeconds(text)
        } catch (_: Exception) {
            null
        }
    }

    private val TIME_REGEX = Regex("""(\d{1,2}):(\d{2}):(\d{2})[.,](\d{1,3})""")

    /** Parse SRT/VTT timestamp-ove → max trajanje u sekundama */
    private fun parseMaxSeconds(text: String): Int? {
        var max = -1.0
        var count = 0
        for (m in TIME_REGEX.findAll(text)) {
            val h = m.groupValues[1].toInt()
            val mi = m.groupValues[2].toInt()
            val s = m.groupValues[3].toInt()
            val ms = m.groupValues[4].padEnd(3, '0').toInt()
            val t = h * 3600.0 + mi * 60.0 + s + ms / 1000.0
            if (t > max) max = t
            count++
            if (count > 5000) break
        }
        return if (max > 0) max.toInt() else null
    }

    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull }?.asString
}
