package com.igor.istreamingtv.data.livetv

object M3uParser {

    private val ATTR_REGEX = Regex("""([\w-]+)="([^"]*)"""")

    /** Vraća (kanali, url-tvg EPG adresa iz M3U zaglavlja) */
    fun parse(text: String): Pair<List<LiveChannel>, String?> {
        val channels = mutableListOf<LiveChannel>()
        var epgUrl: String? = null
        var attrs = mapOf<String, String>()
        var name = ""
        var hasPending = false

        for (raw in text.lines()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTM3U") -> {
                    epgUrl = ATTR_REGEX.find(line)?.groupValues?.get(2)
                }
                line.startsWith("#EXTINF") -> {
                    val map = mutableMapOf<String, String>()
                    ATTR_REGEX.findAll(line).forEach { map[it.groupValues[1]] = it.groupValues[2] }
                    attrs = map
                    name = line.substringAfterLast(',').trim()
                    hasPending = true
                }
                line.isNotEmpty() && !line.startsWith("#") && hasPending -> {
                    channels.add(
                        LiveChannel(
                            id = attrs["tvg-id"] ?: name.ifBlank { channels.size.toString() },
                            name = name.ifBlank { attrs["tvg-name"] ?: "Kanal" },
                            logoUrl = attrs["tvg-logo"],
                            group = attrs["group-title"],
                            streamUrl = line,
                            epgId = attrs["tvg-id"] ?: attrs["tvg-name"] ?: name
                        )
                    )
                    hasPending = false
                }
            }
        }
        return channels to epgUrl
    }
                                   }
