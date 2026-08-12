package com.igor.istreamingtv.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("movie/popular")
    suspend fun getPopularMovies(): MovieResponse

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(): MovieResponse

    @GET("trending/movie/week")
    suspend fun getTrendingMovies(): MovieResponse

    @GET("trending/tv/week")
    suspend fun getTrendingSeries(): MovieResponse

    @GET("tv/popular")
    suspend fun getPopularSeries(): MovieResponse

    @GET("tv/top_rated")
    suspend fun getTopRatedSeries(): MovieResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int
    ): TmdbMovieDetails

    @GET("movie/{movie_id}")
    suspend fun getMovieHeroDetails(
        @Path("movie_id") movieId: Int,
        @Query("append_to_response") appendToResponse: String =
            "images,translations,release_dates"
    ): TmdbHeroDetails

    @GET("tv/{tv_id}")
    suspend fun getTvHeroDetails(
        @Path("tv_id") tvId: Int,
        @Query("append_to_response") appendToResponse: String =
            "images,translations,content_ratings"
    ): TmdbHeroDetails
}
