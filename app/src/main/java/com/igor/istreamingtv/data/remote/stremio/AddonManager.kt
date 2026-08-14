package com.igor.istreamingtv.data.remote.stremio

import com.igor.istreamingtv.data.remote.TmdbClient
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

data class StremioAddon(
    val name: String,
    val baseUrl: String
)

class AddonManager {

    private val addons = mutableListOf(
        StremioAddon("Cinemeta", "https://v3-cinemeta.strem.io/")
    )

    fun addAddon(name: String, url: String) {
        val normalizedUrl = if (url.endsWith("/")) url else "$url/"
        addons.add(StremioAddon(name, normalizedUrl))
    }

    fun removeAddon(name: String) {
        addons.removeAll { it.name == name }
    }

    suspend fun getAllStreams(type: String, imdbId: String): List<StremioStream> {
        val allStreams = mutableListOf<StremioStream>()
        val contentType = "application/json".toMediaType()

        for (addon in addons) {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(addon.baseUrl)
                    .addConverterFactory(TmdbClient.json.asConverterFactory(contentType))
                    .build()
                val api = retrofit.create(StremioApi::class.java)
                val response = api.getStreams(type, imdbId)
                allStreams.addAll(response.streams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return allStreams
    }

    fun getAddons(): List<StremioAddon> = addons.toList()
}
