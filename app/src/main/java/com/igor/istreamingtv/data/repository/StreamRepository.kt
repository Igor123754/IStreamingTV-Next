package com.igor.istreamingtv.data.repository

import com.igor.istreamingtv.data.remote.TmdbApi
import com.igor.istreamingtv.data.remote.TmdbClient
import com.igor.istreamingtv.data.remote.TmdbMovieDetails
import com.igor.istreamingtv.data.remote.stremio.AddonManager
import com.igor.istreamingtv.data.remote.stremio.StremioStream

class StreamRepository(
    private val apiKey: String
) {

    private val tmdbApi = TmdbClient.retrofit.create(TmdbApi::class.java)
    private val addonManager = AddonManager()

    suspend fun getMovieDetails(movieId: Int): TmdbMovieDetails {
        return tmdbApi.getMovieDetails(movieId, apiKey)
    }

    suspend fun getStreamsForMovie(imdbId: String): List<StremioStream> {
        return addonManager.getAllStreams("movie", imdbId)
    }
}
