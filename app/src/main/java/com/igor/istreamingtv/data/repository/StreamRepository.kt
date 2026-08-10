package com.igor.istreamingtv.data.repository

import com.igor.istreamingtv.data.remote.TmdbApi
import com.igor.istreamingtv.data.remote.TmdbClient
import com.igor.istreamingtv.data.remote.TmdbMovieDetails
import com.igor.istreamingtv.data.remote.stremio.AddonManager
import com.igor.istreamingtv.data.remote.stremio.StremioStream

class StreamRepository(
    private val accessToken: String
) {
    private val tmdbApi = TmdbClient.createRetrofit(accessToken).create(TmdbApi::class.java)
    private val addonManager = AddonManager()

    suspend fun getMovieDetails(movieId: Int): TmdbMovieDetails {
        return tmdbApi.getMovieDetails(movieId)
    }

    suspend fun getStreamsForMovie(imdbId: String): List<StremioStream> {
        return addonManager.getAllStreams("movie", imdbId)
    }
}
