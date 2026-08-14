package com.igor.istreamingtv.data.remote.stremio

import retrofit2.http.GET
import retrofit2.http.Path

interface StremioCatalogApi {
    @GET("catalog/{type}/{id}.json")
    suspend fun getCatalog(
        @Path("type") type: String,  // "movie" ili "series"
        @Path("id") id: String       // "top", "popular", itd.
    ): StremioCatalogResponse
}
