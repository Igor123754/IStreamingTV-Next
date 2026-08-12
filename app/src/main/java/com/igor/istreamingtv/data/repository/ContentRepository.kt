package com.igor.istreamingtv.data.repository

import com.igor.istreamingtv.data.remote.TmdbApi
import com.igor.istreamingtv.data.remote.TmdbClient
import com.igor.istreamingtv.data.remote.TmdbHeroDetails
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.TmdbMovieDetails

class ContentRepository(
    private val accessToken: String
) {
    private val api = TmdbClient.createRetrofit(accessToken).create(TmdbApi::class.java)

    suspend fun getPopularMovies(): List<TmdbMovie> = api.getPopularMovies().results

    suspend fun getTopRatedMovies(): List<TmdbMovie> = api.getTopRatedMovies().results

    suspend fun getTrendingMovies(): List<TmdbMovie> = api.getTrendingMovies().results

    suspend fun getTrendingSeries(): List<TmdbMovie> = api.getTrendingSeries().results

    suspend fun getMovieDetails(movieId: Int): TmdbMovieDetails = api.getMovieDetails(movieId)

    suspend fun getMovieHeroDetails(movieId: Int): TmdbHeroDetails =
        api.getMovieHeroDetails(movieId)

    suspend fun getTvHeroDetails(tvId: Int): TmdbHeroDetails =
        api.getTvHeroDetails(tvId)
}
