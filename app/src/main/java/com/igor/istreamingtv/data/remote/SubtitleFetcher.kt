package com.igor.istreamingtv.data.remote

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * TITLOVI — OpenSubtitles Stremio addon.
 * Automatski bira NAJBOLJI srpski titl (downloads + rating = najčešće sinhronizovan).
 */
object SubtitleFetcher {

    const val ADDON_BASE_URL = "https://opensubtitles-v3.strem.io"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Svi mogući kodovi za srpski jezik
    private val serbianCodes = setOf("sr", "scc", "srp", "sr-rs", "serbian")

    /**
     * Vraća URL najboljeg srpskog titla, ili null ako ne postoji.
     */
    suspend fun getBestSerbianSubtitle(
        type: String,               // "movie" | "series"
        imdbId: String,
        season: Int = -1,
        episode: Int = -1
    ): String? = withContext(Dispatchers.IO) {
        try {
            val path = if (type == "series") "series/$imdbId:$season:$episode" else "movie/$imdbId"
            val request = Request.Builder()
                .url("$ADDON_BASE_URL/subtitles/$path.json")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            response.close()
            if (body.isNullOrBlank()) return@withContext null

            val obj = JsonParser.parseString(body).asJsonObject
            val arr = obj.getAsJsonArray("subtitles") ?: return@withContext null

            arr.mapNotNull { el ->
                val o = el.asJsonObject
                val url = o.str("url") ?: return@mapNotNull null

                // MikroDVD (.sub) format ExoPlayer ne podržava dobro — preskoči
                if (url.endsWith(".sub", ignoreCase = true)) return@mapNotNull null

                val lang = listOf("lang", "language", "langCode", "subLanguageId")
                    .firstNotNullOfOrNull { key -> o.str(key)?.lowercase() }
                    ?: ""
                if (lang !in serbianCodes) return@mapNotNull null

                // Rang: downloads + rating*10 → najbolji (najsinhronizovaniji) prvi
                val downloads = o.num("downloads")
                val ratings = o.num("ratings")
                Triple(url, downloads + ratings * 10.0, downloads)
            }
                .sortedByDescending { it.second }
                .firstOrNull()
                ?.first
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull }?.asString

    private fun JsonObject.num(key: String): Double =
        try {
            get(key)?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
        } catch (_: Exception) {
            0.0
        }
}
