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

                    // ✅ ZADRŽI EPG SAMO ZA KANALE IZ TVOJE LISTE
                    //    (drastično manje RAM-a — bitno za 2GB TV i sprečava OOM crash)
                    val wantedKeys = buildSet {
                        channels.forEach { ch ->
                            ch.epgId?.let { add(it) }
                            add(ch.name)
                        }
                    }
                    parsed.filterKeys { wantedKeys.contains(it) }
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
