package com.igor.istreamingtv.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface TmdbApi {

    @GET("movie/popular")
    suspend fun getPopularMovies(): MovieResponse

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(): MovieResponse

    @GET("trending/movie/week")
    suspend fun getTrendingMovies(): MovieResponse

    @GET("trending/tv/week")
    suspend fun getTrendingSeries(): MovieResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int
    ): TmdbMovieDetails
}
