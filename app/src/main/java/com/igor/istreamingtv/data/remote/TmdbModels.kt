package com.igor.istreamingtv.data.remote

data class MovieResponse(
    val results: List<TmdbMovie>,
    val page: Int,
    val total_pages: Int
)

data class TmdbMovie(
    val id: Int,
    val title: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val release_date: String?,
    val vote_average: Double,
    val genre_ids: List<Int>
)

// NOVO: Detalji filma
data class TmdbMovieDetails(
    val id: Int,
    val title: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val release_date: String?,
    val vote_average: Double,
    val genres: List<Genre>,
    val imdb_id: String?,        // KLJUČNO za Stremio
    val runtime: Int?,           // Trajanje u minutama
    val tagline: String?
)

data class Genre(
    val id: Int,
    val name: String
)
