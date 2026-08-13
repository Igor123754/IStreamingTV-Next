package com.igor.istreamingtv.data.remote

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Traka titla SA JEZIKOM i IZMERENIM TRAJANJEM —
 * player pomoću trajanja bira sinhronizovan titl.
 */
data class SubtitleTrack(
    val url: String,
    val lang: String,          // "sr" | "hr"
    val durationSec: Int? = null
)

/**
 * TITLOVI — OpenSubtitles Stremio addon.
 * PRIORITET: srpski > hrvatski.
 *
 * AUTO-SINHRONIZACIJA (2 nivoa):
 *  1) Pre gledanja: |trajanje titla − TMDB runtime| (grubi ranking)
 *  2) Tokom gledanja: player zna TAČNO trajanje videa i tiho
 *     prebacuje na titl čije izmereno trajanje najbliže
 */
object SubtitleFetcher {

    const val ADDON_BASE_URL = "https://opensubtitles-v3.strem.io"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val serbianCodes = setOf("sr", "scc", "srp", "sr-rs", "serbian")
    private val croatianCodes = setOf("hr", "hrv", "hr-hr", "croatian", "hbs", "bos", "bosnian")
    private val acceptedCodes = serbianCodes + croatianCodes

    data class SubtitleEntry(
        val url: String,
        val language: String,
        val encoding: String?,
        val order: Int,
        val durationSec: Int? = null   // izmereno trajanje titla
    ) {
        val isSerbian: Boolean get() = language == "sr"
    }

    private data class Timing(
        val firstSec: Int,
        val maxSec: Int
    )

    /** Svi prihvaćeni titlovi: srpski PRVO, pa hrvatski (do 10) */
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
                if (url.endsWith(".sub", ignoreCase = true)) return@forEach

                val code = if (langRaw in serbianCodes) "sr" else "hr"
                val entry = SubtitleEntry(url, code, o.str("SubEncoding"), idx++)
                if (code == "sr") serbian.add(entry) else croatian.add(entry)
            }
            (serbian + croatian).take(10)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Grubi ranking pre gledanja (TMDB runtime u minutima) */
    suspend fun rankBySync(
        entries: List<SubtitleEntry>,
        expectedSeconds: Int?
    ): List<SubtitleEntry> {
        if (entries.size < 2 || expectedSeconds == null || expectedSeconds <= 0) return entries

        val rankedSerbian = rankGroup(entries.filter { it.isSerbian }, expectedSeconds)
        val rankedCroatian = rankGroup(entries.filter { !it.isSerbian }, expectedSeconds)
        return rankedSerbian + rankedCroatian
    }

    private suspend fun rankGroup(
        group: List<SubtitleEntry>,
        expectedSeconds: Int
    ): List<SubtitleEntry> {
        if (group.size < 2) return group
        val toCheck = group.take(6)
        val rest = group.drop(6)

        return coroutineScope {
            val scored: List<Pair<SubtitleEntry, Int>> = toCheck.mapIndexed { index, entry ->
                async {
                    val timing = fetchTiming(entry.url)
                    val withDuration = entry.copy(durationSec = timing?.maxSec)
                    val score = if (timing == null) {
                        Int.MAX_VALUE / 2 + index
                    } else {
                        var s = abs(timing.maxSec - expectedSeconds)   // GLAVNI signal
                        s += index * 5                                 // tie-breaker
                        if (timing.firstSec > 600) s += 300            // pogrešan release
                        s
                    }
                    withDuration to score
                }
            }.awaitAll()

            scored.sortedBy { it.second }.map { it.first } + rest
        }
    }

    /** Preuzme titl i vrati (prva replika, max trajanje) u sekundama */
    private suspend fun fetchTiming(url: String): Timing? {
        return try {
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val text = response.body?.string()
            response.close()
            if (text.isNullOrBlank()) null else parseTimings(text)
        } catch (_: Exception) {
            null
        }
    }

    private val TIME_REGEX = Regex("""(\d{1,2}):(\d{2}):(\d{2})[.,](\d{1,3})""")

    private fun parseTimings(text: String): Timing? {
        var first = -1.0
        var max = -1.0
        var count = 0
        for (m in TIME_REGEX.findAll(text)) {
            val h = m.groupValues[1].toInt()
            val mi = m.groupValues[2].toInt()
            val s = m.groupValues[3].toInt()
            val ms = m.groupValues[4].padEnd(3, '0').toInt()
            val t = h * 3600.0 + mi * 60.0 + s + ms / 1000.0
            if (first < 0) first = t
            if (t > max) max = t
            count++
            if (count > 5000) break
        }
        return if (max > 0 && first >= 0) Timing(first.toInt(), max.toInt()) else null
    }

    /** Konverzija u trake (url + lang + trajanje) za player */
    fun toTracks(entries: List<SubtitleEntry>, limit: Int = 6): List<SubtitleTrack> =
        entries.take(limit).map { SubtitleTrack(it.url, it.language, it.durationSec) }

    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull }?.asString
}
