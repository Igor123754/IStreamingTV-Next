package com.igor.istreamingtv.data.remote.stremio

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

        for (addon in addons) {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(addon.baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
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
