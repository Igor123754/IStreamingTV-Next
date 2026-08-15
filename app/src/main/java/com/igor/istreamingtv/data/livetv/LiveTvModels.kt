package com.igor.istreamingtv.data.livetv

object LiveTvConfig {
    // 🔧 UPIŠI OVDE SVOJE URL-OVE:
    const val M3U_URL = "https://example.com/moja-lista.m3u8"
    const val EPG_URL = "" // ako je prazno → koristi se url-tvg iz M3U
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
