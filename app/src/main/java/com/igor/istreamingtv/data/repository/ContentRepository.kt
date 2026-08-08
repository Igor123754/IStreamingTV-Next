package com.igor.istreamingtv.data.repository

import com.igor.istreamingtv.data.remote.TmdbClient
import com.igor.istreamingtv.data.remote.TmdbMovie

class ContentRepository(
    private val tmdbToken: String
) {

    suspend fun getTrendingMovies(): List<TmdbMovie> {
        return TmdbClient.api
            .getTrendingMovies(
                token = "Bearer $tmdbToken"
            )
            .results
    }

    suspend fun getTrendingSeries(): List<TmdbMovie> {
        return TmdbClient.api
            .getTrendingSeries(
                token = "Bearer $tmdbToken"
            )
            .results
    }
}
