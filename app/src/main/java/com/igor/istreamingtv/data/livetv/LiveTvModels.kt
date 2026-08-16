package com.igor.istreamingtv.data.livetv

object LiveTvConfig {
    // ✅ TVOJA PRAVA M3U LISTA + EPG (m3u4u):
    const val M3U_URL = "http://m3u4u.com/m3u/476rnmmqd7fp464pnekg"
    const val EPG_URL = "http://m3u4u.com/xml/476rnmmqd7fp464pnekg"
}

data class LiveChannel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val group: String?,
    val streamUrl: String,
    val epgId: String?
)

data class EpgProgram(
    val channel: String,
    val title: String,
    val description: String?,
    val iconUrl: String?,
    val startMs: Long,
    val endMs: Long,
    val category: String?
)

data class LiveTvData(
    val channels: List<LiveChannel>,
    val epg: Map<String, List<EpgProgram>>
)

/**
 * ✅ SESIJSKO STANJE (samo RAM, bez keša na disku) — deli se između
 *    početne i player-a da CH+/CH- zapping radi instant sa svim informacijama.
 */
object LiveTvSession {
    var channels: List<LiveChannel> = emptyList()
    var epg: Map<String, List<EpgProgram>> = emptyMap()
    var currentIndex: Int = 0
}
