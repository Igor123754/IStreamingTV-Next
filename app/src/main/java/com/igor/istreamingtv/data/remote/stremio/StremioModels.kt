package com.igor.istreamingtv.data.remote.stremio

/**
 * Stremio Manifest — opisuje šta addon nudi
 */
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

/**
 * Stremio Stream — stvarni link za gledanje
 */
data class StremioStreamResponse(
    val streams: List<StremioStream>
)

data class StremioStream(
    val name: String?,
    val title: String?,          // Npr. "1080p", "Torrentio 4K"
    val url: String?,            // Direktan URL
    val externalUrl: String?,    // Za eksterne playere
    val behaviorHints: BehaviorHints?
) {
    /**
     * Vraća čitljivi naslov streama
     */
    fun displayTitle(): String {
        return title ?: name ?: "Nepoznat izvor"
    }

    /**
     * Da li je direktno playable u ExoPlayer-u
     */
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
