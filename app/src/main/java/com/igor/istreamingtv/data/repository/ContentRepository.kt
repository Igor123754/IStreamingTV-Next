package com.igor.istreamingtv.data.repository

import com.igor.istreamingtv.data.remote.TmdbApi
import com.igor.istreamingtv.data.remote.TmdbClient
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.TmdbMovieDetails

class ContentRepository(
    private val apiKey: String
) {

    private val api = TmdbClient.retrofit.create(TmdbApi::class.java)

    suspend fun getPopularMovies(page: Int = 1): List<TmdbMovie> {
        return api.getPopularMovies(page, apiKey).results
    }

    suspend fun getTopRatedMovies(page: Int = 1): List<TmdbMovie> {
        return api.getTopRatedMovies(page, apiKey).results
    }

    suspend fun getTrendingMovies(): List<TmdbMovie> {
        return api.getTrendingMovies(apiKey).results
    }

    suspend fun getTrendingSeries(): List<TmdbMovie> {
        return api.getTrendingSeries(apiKey).results
    }

    suspend fun getMovieDetails(movieId: Int): TmdbMovieDetails {
        return api.getMovieDetails(movieId, apiKey)
    }
}
