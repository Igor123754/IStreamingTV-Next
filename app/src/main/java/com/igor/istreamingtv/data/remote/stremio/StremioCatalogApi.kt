package com.igor.istreamingtv.data.remote.stremio

import retrofit2.http.GET
import retrofit2.http.Path

interface StremioCatalogApi {
    @GET("catalog/{type}/{id}.json")
    suspend fun getCatalog(
        @Path("type") type: String,
        @Path("id") id: String
    ): StremioCatalogResponse
}
