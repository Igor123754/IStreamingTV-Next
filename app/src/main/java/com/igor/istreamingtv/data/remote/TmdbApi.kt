package com.igor.istreamingtv.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TmdbApi {

    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Header("Authorization") token: String,
        @Query("language") language: String = "en-US"
    ): TmdbResponse

    @GET("trending/tv/week")
    suspend fun getTrendingSeries(
        @Header("Authorization") token: String,
        @Query("language") language: String = "en-US"
    ): TmdbResponse
}
