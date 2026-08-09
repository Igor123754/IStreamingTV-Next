package com.igor.istreamingtv.data.remote.stremio

data class StremioManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String?,
    val types: List<String>,
    val catalogs: List<StremioCatalog>?,
    val resources: List<String>?
)

data class StremioCatalog(
    val type: String,
    val id: String,
    val name: String
)

data class StremioStreamResponse(
    val streams: List<StremioStream>
)

data class StremioStream(
    val name: String?,
    val title: String?,
    val url: String?,
    val externalUrl: String?,
    val behaviorHints: BehaviorHints?
) {
    fun displayTitle(): String {
        return title ?: name ?: "Nepoznat izvor"
    }

    fun isPlayable(): Boolean {
        return url != null
    }
}

data class BehaviorHints(
    val proxyHeaders: ProxyHeaders?,
    val notWebReady: Boolean?,
    val bingeGroup: String?
)

data class ProxyHeaders(
    val request: Map<String, String>?
)
