package com.igor.istreamingtv.data.remote.stremio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StremioManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String? = null,
    val types: List<String> = emptyList(),
    val catalogs: List<StremioCatalog>? = null,
    val resources: List<String>? = null
)

@Serializable
data class StremioCatalog(
    val type: String,
    val id: String,
    val name: String
)

@Serializable
data class StremioStreamResponse(
    val streams: List<StremioStream> = emptyList()
)

@Serializable
data class StremioStream(
    val name: String? = null,
    val title: String? = null,
    val url: String? = null,
    @SerialName("externalUrl")
    val externalUrl: String? = null,
    @SerialName("behaviorHints")
    val behaviorHints: BehaviorHints? = null
) {
    fun displayTitle(): String = title ?: name ?: "Nepoznat izvor"
    fun isPlayable(): Boolean = url != null
}

@Serializable
data class BehaviorHints(
    @SerialName("proxyHeaders")
    val proxyHeaders: ProxyHeaders? = null,
    @SerialName("notWebReady")
    val notWebReady: Boolean? = null,
    @SerialName("bingeGroup")
    val bingeGroup: String? = null
)

@Serializable
data class ProxyHeaders(
    val request: Map<String, String>? = null
)
