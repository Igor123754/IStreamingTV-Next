package com.igor.istreamingtv.data.remote.stremio

import retrofit2.http.GET
import retrofit2.http.Path

interface StremioApi {

    @GET("manifest.json")
    suspend fun getManifest(): StremioManifest

    @GET("stream/{type}/{id}.json")
    suspend fun getStreams(
        @Path("type") type: String,
        @Path("id") id: String
    ): StremioStreamResponse
}
