package com.igor.istreamingtv.data.remote.stremio

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Stremio addon API — stream-ovi, manifest i katalozi.
 * `id` za serije je u formatu "tt1234567:1:2" (imdb:season:episode),
 * za filmove samo "tt1234567".
 */
interface StremioApi {

    @GET("stream/{type}/{id}.json")
    suspend fun getStreams(
        @Path("type") type: String,   // "movie" | "series"
        @Path("id") id: String        // "tt1234567" ili "tt1234567:1:2"
    ): StremioStreamResponse

    @GET("manifest.json")
    suspend fun getManifest(): StremioManifest

    @GET("catalog/{type}/{id}.json")
    suspend fun getCatalog(
        @Path("type") type: String,
        @Path("id") id: String
    ): StremioCatalogResponse
}
