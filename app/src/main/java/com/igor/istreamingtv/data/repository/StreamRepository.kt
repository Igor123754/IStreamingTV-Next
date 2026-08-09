package com.igor.istreamingtv.data.repository

import com.igor.istreamingtv.data.remote.TmdbApi
import com.igor.istreamingtv.data.remote.TmdbMovieDetails
import com.igor.istreamingtv.data.remote.stremio.AddonManager
import com.igor.istreamingtv.data.remote.stremio.StremioStream

class StreamRepository(
    private val tmdbApi: TmdbApi,
    private val addonManager: AddonManager,
    private val apiKey: String
) {

    /**
     * Učitaj detalje filma sa TMDB-a
     */
    suspend fun getMovieDetails(movieId: Int): TmdbMovieDetails {
        return tmdbApi.getMovieDetails(movieId, apiKey)
    }

    /**
     * Nađi sve dostupne streamove za film preko Stremio addona
     */
    suspend fun getStreamsForMovie(imdbId: String): List<StremioStream> {
        return addonManager.getAllStreams("movie", imdbId)
    }
}
