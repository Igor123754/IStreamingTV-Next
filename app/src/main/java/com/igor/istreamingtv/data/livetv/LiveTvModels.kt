package com.igor.istreamingtv.data.livetv

object LiveTvConfig {
    // ✅ Your real M3U list + EPG (m3u4u):
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
 * ✅ Session state (RAM only, no cache) — for CH+/CH- zapping.
 */
object LiveTvSession {
    var channels: List<LiveChannel> = emptyList()
    var epg: Map<String, List<EpgProgram>> = emptyMap()
    var currentIndex: Int = 0
}

// =====================================================================
// ✅ Robust EPG mapping — works even when XML ID has a suffix " (src05)",
//    different casing, or when M3U uses a different ID format
// =====================================================================

private fun normKey(s: String): String = s.trim().lowercase()
    .replace(Regex("\\s*\\(src\\d+\\)"), "")
    .replace(Regex("\\s*\\([^)]*\\)"), "")

/** Finds the EPG program list for a channel (exact match → normalized → fuzzy) */
fun epgListFor(epg: Map<String, List<EpgProgram>>, ch: LiveChannel): List<EpgProgram>? {
    val keys = listOfNotNull(ch.epgId, ch.name).distinct()

    // 1) Exact match
    for (k in keys) epg[k]?.let { return it }

    // 2) Normalized match (strips suffix, casing)
    for (k in keys) {
        val nk = normKey(k)
        val hit = epg.entries.firstOrNull { normKey(it.key) == nk }
        if (hit != null) return hit.value
    }

    // 3) Fuzzy: one key starts with the other
    for (k in keys) {
        val nk = normKey(k)
        if (nk.isBlank()) continue
        val hit = epg.entries.firstOrNull { (ek, _) ->
            val nek = normKey(ek)
            nek.isNotEmpty() && (nek.startsWith(nk) || nk.startsWith(nek))
        }
        if (hit != null) return hit.value
    }
    return null
}
