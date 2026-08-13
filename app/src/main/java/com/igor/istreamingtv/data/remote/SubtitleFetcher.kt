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
 * TITLOVI — OpenSubtitles Stremio addon.
 * PRIORITET: srpski (sr/scc/srp) > hrvatski (hr/hrv)
 * AUTO-SINHRONIZACIJA: duration matching (trajanje titla vs TMDB runtime)
 * odbacuje PAL/25fps titlove koji ne odgovaraju 23.976fps stream-u.
 */
object SubtitleFetcher {

    const val ADDON_BASE_URL = "https://opensubtitles-v3.strem.io"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Srpski kodovi (ISO 639-1, 2, 3, bibliografski)
    private val serbianCodes = setOf("sr", "scc", "srp", "sr-rs", "serbian")
    // Hrvatski kodovi (fallback)
    private val croatianCodes = setOf("hr", "hrv", "hr-hr", "croatian", "hbs", "bos", "bosnian")
    // Unija prihvaćenih jezika
    private val acceptedCodes = serbianCodes + croatianCodes

    data class SubtitleEntry(
        val url: String,
        val language: String,      // "sr" ili "hr"
        val encoding: String?,
        val order: Int
    ) {
        val isSerbian: Boolean get() = language == "sr"
    }

    /** Svi prihvaćeni titlovi (sr > hr), do 10, po prioritetu */
    suspend fun getAcceptedSubtitles(
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

            val serbian = mutableListOf<SubtitleEntry>()
            val croatian = mutableListOf<SubtitleEntry>()
            var idx = 0

            arr.forEach { el ->
                val o = el.asJsonObject
                val url = o.str("url") ?: return@forEach
                val langRaw = (o.str("lang") ?: "").lowercase()
                if (langRaw !in acceptedCodes) return@forEach
                // MikroDVD (.sub) ExoPlayer ne podržava dobro
                if (url.endsWith(".sub", ignoreCase = true)) return@forEach

                val code = if (langRaw in serbianCodes) "sr" else "hr"
                val entry = SubtitleEntry(url, code, o.str("SubEncoding"), idx++)
                if (code == "sr") serbian.add(entry) else croatian.add(entry)
            }
            // Srpski PRVO, pa hrvatski
            (serbian + croatian).take(10)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * AUTO-SINHRONIZACIJA po trajanju.
     * Unutar svake jezičke grupe rangira po sinhronizaciji,
     * ali SRPSKI ostaju ISPRED HRVATSKOG u konačnoj listi.
     */
    suspend fun rankBySync(
        entries: List<SubtitleEntry>,
        expectedSeconds: Int?
    ): List<SubtitleEntry> {
        if (entries.size < 2 || expectedSeconds == null || expectedSeconds <= 0) return entries
        return withContext(Dispatchers.IO) {
            val serbian = entries.filter { it.isSerbian }
            val croatian = entries.filter { !it.isSerbian }

            val rankedSerbian = rankGroup(serbian, expectedSeconds)
            val rankedCroatian = rankGroup(croatian, expectedSeconds)

            rankedSerbian + rankedCroatian
        }
    }

    private suspend fun rankGroup(
        group: List<SubtitleEntry>,
        expectedSeconds: Int
    ): List<SubtitleEntry> {
        if (group.size < 2) return group
        val toCheck = group.take(4)
        val rest = group.drop(4)

        val scored = toCheck.map { entry ->
            async {
                val duration = fetchDurationSeconds(entry.url)
                val diff = duration?.let { abs(it - expectedSeconds) } ?: Int.MAX_VALUE
                entry to diff
            }
        }.awaitAll()

        return scored.sortedBy { it.second }.map { it.first } + rest
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
