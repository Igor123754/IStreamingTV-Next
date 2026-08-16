package com.igor.istreamingtv.data.livetv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

class LiveTvRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun load(): LiveTvData? = withContext(Dispatchers.IO) {
        try {
            val m3uText = fetchText(LiveTvConfig.M3U_URL) ?: return@withContext null
            val (channels, m3uEpgUrl) = M3uParser.parse(m3uText)
            if (channels.isEmpty()) return@withContext LiveTvData(emptyList(), emptyMap())

            val epgUrl = LiveTvConfig.EPG_URL.ifBlank { m3uEpgUrl }
            val epg = if (epgUrl.isNullOrBlank()) emptyMap() else try {
                val resp = client.newCall(Request.Builder().url(epgUrl).build()).execute()
                val body = resp.body
                if (body == null) { resp.close(); emptyMap() } else {
                    val stream = body.byteStream()
                    val input = if (epgUrl.endsWith(".gz", true)) GZIPInputStream(stream) else stream
                    val parsed = EpgParser.parse(input)
                    resp.close()

                    // ✅ Re-map keys: XML "B92.(RS).rs (src05)" is also accessible as "B92.(RS).rs"
                    val result = mutableMapOf<String, List<EpgProgram>>()
                    parsed.forEach { (k, v) ->
                        result[k] = v
                        val stripped = k.substringBefore(" (").trim()
                        if (stripped != k && !result.containsKey(stripped)) result[stripped] = v
                    }

                    // Keep only channels from your list (RAM savings on 2GB)
                    val wanted = buildSet {
                        channels.forEach { ch ->
                            listOfNotNull(ch.epgId, ch.name).forEach { k ->
                                add(k)
                                add(k.substringBefore(" (").trim())
                                add(k.lowercase())
                            }
                        }
                    }
                    result.filterKeys { k ->
                        k in wanted ||
                            k.substringBefore(" (").trim() in wanted ||
                            k.lowercase() in wanted
                    }
                }
            } catch (_: Exception) { emptyMap() }

            LiveTvData(channels, epg)
        } catch (_: Exception) { null }
    }

    private fun fetchText(url: String): String? = try {
        val resp = client.newCall(Request.Builder().url(url).build()).execute()
        val t = resp.body?.string()
        resp.close()
        t
    } catch (_: Exception) { null }
}
