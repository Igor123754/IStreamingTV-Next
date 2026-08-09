package com.igor.istreamingtv.data.remote.stremio

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Predstavlja jedan Stremio addon
 */
data class StremioAddon(
    val name: String,
    val baseUrl: String
)

class AddonManager {

    /**
     * Lista aktivnih addona. Ovde dodaješ svoje addone.
     * 
     * PRIMERI poznatih addon-a (neki zahtevaju sopstveni server):
     * - Cinemeta (metapodaci): "https://v3-cinemeta.strem.io/"
     * - Torrentio (streamovi): "https://torrentio.strem.fun/"
     */
    private val addons = mutableListOf(
        StremioAddon("Cinemeta", "https://v3-cinemeta.strem.io/")
    )

    /**
     * Dodaj novi addon iz Settings-a
     */
    fun addAddon(name: String, url: String) {
        val normalizedUrl = if (url.endsWith("/")) url else "$url/"
        addons.add(StremioAddon(name, normalizedUrl))
    }

    /**
     * Ukloni addon
     */
    fun removeAddon(name: String) {
        addons.removeAll { it.name == name }
    }

    /**
     * Vrati sve streamove iz SVIH aktivnih addona za dati sadržaj
     */
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
                // Ako jedan addon ne radi, nastavljamo sa ostalima
                e.printStackTrace()
            }
        }

        return allStreams
    }

    fun getAddons(): List<StremioAddon> = addons.toList()
}
