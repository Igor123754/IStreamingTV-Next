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

// Extension properties — UI koristi ova imena
val TmdbMovie.displayTitle: String get() = title
val TmdbMovie.displayDate: String get() = release_date ?: ""
val TmdbMovie.posterPath: String? get() = poster_path
val TmdbMovie.backdropPath: String? get() = backdrop_path

data class TmdbMovieDetails(
    val id: Int,
    val title: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val release_date: String?,
    val vote_average: Double,
    val genres: List<Genre>,
    val imdb_id: String?,
    val runtime: Int?,
    val tagline: String?
)

data class Genre(
    val id: Int,
    val name: String
)

// ===== HERO: clearlogo, prevodi, uzrastne oznake =====

data class TmdbHeroDetails(
    val id: Int = 0,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    val images: TmdbImages? = null,
    val translations: TmdbTranslations? = null,
    val release_dates: TmdbReleaseDates? = null,
    val content_ratings: TmdbContentRatings? = null
)

data class TmdbImages(val logos: List<TmdbLogo> = emptyList())

data class TmdbLogo(
    val file_path: String?,
    val iso_639_1: String?,
    val vote_count: Int = 0
)

data class TmdbTranslations(val translations: List<TmdbTranslation> = emptyList())

data class TmdbTranslation(
    val iso_639_1: String? = null,
    val data: TmdbTranslationData? = null
)

data class TmdbTranslationData(
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null
)

data class TmdbReleaseDates(val results: List<TmdbReleaseCountry> = emptyList())

data class TmdbReleaseCountry(
    val iso_3166_1: String? = null,
    val release_dates: List<TmdbReleaseDate> = emptyList()
)

data class TmdbReleaseDate(val certification: String? = null)

data class TmdbContentRatings(val results: List<TmdbContentRatingCountry> = emptyList())

data class TmdbContentRatingCountry(
    val iso_3166_1: String? = null,
    val rating: String? = null
)

// Bira clearlogo: prvo srpski → originalni (bez jezika) → engleski → najbolje ocenjeni
fun TmdbHeroDetails.pickClearLogoUrl(): String? {
    val all = images?.logos?.filter { !it.file_path.isNullOrBlank() } ?: return null
    val pool = all.filter { it.file_path!!.endsWith(".png") }.ifEmpty { all }
    val pick = pool.firstOrNull { it.iso_639_1 == "sr" }
        ?: pool.firstOrNull { it.iso_639_1 == null }
        ?: pool.firstOrNull { it.iso_639_1 == "en" }
        ?: pool.maxByOrNull { it.vote_count }
    return pick?.file_path?.let { "https://image.tmdb.org/t/p/w500$it" }
}

// Srpski opis ako postoji prevod
fun TmdbHeroDetails.pickSerbianOverview(): String? {
    val sr = translations?.translations?.firstOrNull { it.iso_639_1 == "sr" }?.data
    return sr?.overview?.takeIf { it.isNotBlank() }
}

// Uzrastna preporuka (US oznake: PG-13, R, TV-MA, TV-14...)
fun TmdbHeroDetails.pickCertification(): String? {
    val movieCert = release_dates?.results
        ?.firstOrNull { it.iso_3166_1 == "US" }
        ?.release_dates
        ?.firstNotNullOfOrNull { it.certification?.takeIf { c -> c.isNotBlank() } }
    val tvCert = content_ratings?.results
        ?.firstOrNull { it.iso_3166_1 == "US" }
        ?.rating
        ?.takeIf { it.isNotBlank() }
    return movieCert ?: tvCert
}
