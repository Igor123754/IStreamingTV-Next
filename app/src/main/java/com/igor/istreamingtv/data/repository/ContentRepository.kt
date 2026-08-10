package com.igor.istreamingtv.data.repository

import com.igor.istreamingtv.data.remote.TmdbApi
import com.igor.istreamingtv.data.remote.TmdbClient
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.TmdbMovieDetails

class ContentRepository(
    private val accessToken: String
) {
    private val api = TmdbClient.createRetrofit(accessToken).create(TmdbApi::class.java)

    suspend fun getPopularMovies(): List<TmdbMovie> {
        return api.getPopularMovies().results
    }

    suspend fun getTopRatedMovies(): List<TmdbMovie> {
        return api.getTopRatedMovies().results
    }

    suspend fun getTrendingMovies(): List<TmdbMovie> {
        return api.getTrendingMovies().results
    }

    suspend fun getTrendingSeries(): List<TmdbMovie> {
        return api.getTrendingSeries().results
    }

    suspend fun getMovieDetails(movieId: Int): TmdbMovieDetails {
        return api.getMovieDetails(movieId)
    }
}
