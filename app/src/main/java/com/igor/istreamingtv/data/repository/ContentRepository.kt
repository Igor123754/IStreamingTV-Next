package com.igor.istreamingtv.data.repository

import com.igor.istreamingtv.data.remote.TmdbApi
import com.igor.istreamingtv.data.remote.TmdbClient
import com.igor.istreamingtv.data.remote.TmdbCollectionDetails
import com.igor.istreamingtv.data.remote.TmdbHeroDetails
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.TmdbMovieDetails
import com.igor.istreamingtv.data.remote.TmdbSeasonDetails
import com.igor.istreamingtv.data.remote.stremio.StremioCatalogApi
import com.igor.istreamingtv.data.remote.stremio.StremioMeta
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ContentRepository(
    private val accessToken: String
) {
    private val api = TmdbClient.createRetrofit(accessToken).create(TmdbApi::class.java)
    
    // ✅ Cinemeta API za kataloge
    private val cinemetaApi = Retrofit.Builder()
        .baseUrl("https://v3-cinemeta.strem.io/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(StremioCatalogApi::class.java)

    // TMDB pozivi za detalje
    suspend fun getPopularMovies(): List<TmdbMovie> = api.getPopularMovies().results
    suspend fun getTopRatedMovies(): List<TmdbMovie> = api.getTopRatedMovies().results
    suspend fun getTrendingMovies(): List<TmdbMovie> = api.getTrendingMovies().results
    suspend fun getTrendingSeries(): List<TmdbMovie> = api.getTrendingSeries().results
    suspend fun getPopularSeries(): List<TmdbMovie> = api.getPopularSeries().results
    suspend fun getTopRatedSeries(): List<TmdbMovie> = api.getTopRatedSeries().results
    suspend fun getMovieDetails(movieId: Int): TmdbMovieDetails = api.getMovieDetails(movieId)
    suspend fun getMovieHeroDetails(movieId: Int): TmdbHeroDetails = api.getMovieHeroDetails(movieId)
    suspend fun getTvHeroDetails(tvId: Int): TmdbHeroDetails = api.getTvHeroDetails(tvId)
    suspend fun getCollectionDetails(collectionId: Int): TmdbCollectionDetails = api.getCollectionDetails(collectionId)
    suspend fun getSeasonDetails(tvId: Int, seasonNumber: Int): TmdbSeasonDetails = api.getSeasonDetails(tvId, seasonNumber)
    suspend fun getSimilarMovies(movieId: Int): List<TmdbMovie> = api.getSimilarMovies(movieId).results
    suspend fun getSimilarSeries(tvId: Int): List<TmdbMovie> = api.getSimilarSeries(tvId).results

    /**
     * ✅ Cinemeta katalog (brži od TMDB-a za listu).
     * Konvertuje StremioMeta u TmdbMovie za UI.
     */
    suspend fun getCinemetaCatalog(type: String, catalogId: String): List<TmdbMovie> = try {
        cinemetaApi.getCatalog(type, catalogId).metas.map { it.toTmdbMovie() }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Konverzija StremioMeta → TmdbMovie */
private fun StremioMeta.toTmdbMovie(): TmdbMovie {
    val year = releaseInfo?.take(4)?.toIntOrNull()
    return TmdbMovie(
        id = this.id.hashCode(), // Cinemeta koristi IMDB ID kao string
        title = if (this.type == "movie") this.name else null,
        name = if (this.type == "series") this.name else null,
        originalTitle = null,
        originalName = null,
        overview = this.description,
        posterPath = this.poster,
        backdropPath = this.poster, // Cinemeta nema backdrop, koristimo poster
        releaseDate = this.releaseInfo,
        voteAverage = this.imdbRating?.toDoubleOrNull()?.div(10.0) ?: 0.0,
        genreIds = emptyList() // Cinemeta ima genre kao string, ne ID-eve
    )
}
