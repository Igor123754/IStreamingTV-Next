package com.igor.istreamingtv.data.remote

import com.igor.istreamingtv.data.remote.TmdbClient.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class SubtitleTrack(
    val url: String,
    val lang: String,
    val durationSec: Int? = null
)

@Serializable
private data class SubtitlesResponse(
    val subtitles: List<SubtitleItem> = emptyList()
)

@Serializable
private data class SubtitleItem(
    val url: String,
    val lang: String,
    @SerialName("SubEncoding")
    val subEncoding: String? = null
)

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
        val durationSec: Int? = null
    ) {
        val isSerbian: Boolean get() = language == "sr"
    }

    private data class Timing(
        val firstSec: Int,
        val maxSec: Int
    )

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

            // ✅ Type-safe parsing sa kotlinx.serialization
            val parsed = json.decodeFromString<SubtitlesResponse>(body)

            val serbian = mutableListOf<SubtitleEntry>()
            val croatian = mutableListOf<SubtitleEntry>()
            var idx = 0

            parsed.subtitles.forEach { item ->
                val langRaw = item.lang.lowercase()
                if (langRaw !in acceptedCodes) return@forEach
                if (item.url.endsWith(".sub", ignoreCase = true)) return@forEach

                val code = if (langRaw in serbianCodes) "sr" else "hr"
                val entry = SubtitleEntry(item.url, code, item.subEncoding, idx++)
                if (code == "sr") serbian.add(entry) else croatian.add(entry)
            }
            (serbian + croatian).take(10)
        } catch (_: Exception) {
            emptyList()
        }
    }

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
                        var s = abs(timing.maxSec - expectedSeconds)
                        s += index * 5
                        if (timing.firstSec > 600) s += 300
                        s
                    }
                    withDuration to score
                }
            }.awaitAll()

            scored.sortedBy { it.second }.map { it.first } + rest
        }
    }

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

    fun toTracks(entries: List<SubtitleEntry>, limit: Int = 6): List<SubtitleTrack> =
        entries.take(limit).map { SubtitleTrack(it.url, it.language, it.durationSec) }
}
