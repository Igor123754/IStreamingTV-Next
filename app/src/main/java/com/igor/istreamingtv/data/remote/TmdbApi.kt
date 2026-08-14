package com.igor.istreamingtv.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("movie/popular?language=sr-RS")
    suspend fun getPopularMovies(): MovieResponse

    @GET("movie/top_rated?language=sr-RS")
    suspend fun getTopRatedMovies(): MovieResponse

    @GET("trending/movie/week?language=sr-RS")
    suspend fun getTrendingMovies(): MovieResponse

    @GET("trending/tv/week?language=sr-RS")
    suspend fun getTrendingSeries(): MovieResponse

    @GET("tv/popular?language=sr-RS")
    suspend fun getPopularSeries(): MovieResponse

    @GET("tv/top_rated?language=sr-RS")
    suspend fun getTopRatedSeries(): MovieResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int
    ): TmdbMovieDetails

    // ✅ SRPSKI sa engleskim fallback-om — TMDB vraća srpski overview/title
    //    direktno kad postoji, engleski samo ako srpskog nema
    @GET("movie/{movie_id}")
    suspend fun getMovieHeroDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "sr-RS,en-US",
        @Query("append_to_response") appendToResponse: String =
            "images,translations,release_dates,credits"
    ): TmdbHeroDetails

    @GET("tv/{tv_id}")
    suspend fun getTvHeroDetails(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "sr-RS,en-US",
        @Query("append_to_response") appendToResponse: String =
            "images,translations,content_ratings,credits,external_ids"
    ): TmdbHeroDetails

    @GET("collection/{collection_id}")
    suspend fun getCollectionDetails(
        @Path("collection_id") collectionId: Int
    ): TmdbCollectionDetails

    // ✅ Sezone/epizode takođe sa fallback-om (da epizode nikad ne ostanu bez opisa)
    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = "sr-RS,en-US"
    ): TmdbSeasonDetails

    @GET("movie/{movie_id}/similar?language=sr-RS")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int
    ): MovieResponse

    @GET("tv/{tv_id}/similar?language=sr-RS")
    suspend fun getSimilarSeries(
        @Path("tv_id") tvId: Int
    ): MovieResponse
}
