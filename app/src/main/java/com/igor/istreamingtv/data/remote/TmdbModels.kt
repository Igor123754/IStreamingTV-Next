package com.igor.istreamingtv.data.remote

data class MovieResponse(
    val results: List<TmdbMovie>,
    val page: Int,
    val total_pages: Int
)

data class TmdbMovie(
    val id: Int,
    val title: String?,
    val name: String?,
    val original_title: String?,
    val original_name: String?,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val release_date: String?,
    val vote_average: Double,
    val genre_ids: List<Int>?
)

val TmdbMovie.displayTitle: String
    get() = title?.takeIf { it.isNotBlank() }
        ?: name?.takeIf { it.isNotBlank() }
        ?: original_title
        ?: original_name
        ?: ""

val TmdbMovie.displayOverview: String get() = overview ?: ""
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

// ===== HERO / DETALJI =====

data class TmdbHeroDetails(
    val id: Int = 0,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    val backdrop_path: String? = null,
    val poster_path: String? = null,
    val release_date: String? = null,
    val first_air_date: String? = null,
    val runtime: Int? = null,
    val episode_run_time: List<Int>? = null,
    val genres: List<Genre>? = null,
    val imdb_id: String? = null,
    val external_ids: TmdbExternalIds? = null,
    val belongs_to_collection: TmdbCollection? = null,
    val seasons: List<TmdbSeason>? = null,
    val images: TmdbImages? = null,
    val translations: TmdbTranslations? = null,
    val release_dates: TmdbReleaseDates? = null,
    val content_ratings: TmdbContentRatings? = null,
    val credits: TmdbCredits? = null
)

// Serije nemaju imdb_id na vrhu — nalazi se u external_ids
data class TmdbExternalIds(
    val imdb_id: String? = null
)

// ===== KOLEKCIJE (nastavci) =====

data class TmdbCollection(
    val id: Int = 0,
    val name: String? = null,
    val poster_path: String? = null
)

data class TmdbCollectionDetails(
    val id: Int = 0,
    val name: String? = null,
    val parts: List<TmdbMovie> = emptyList()
)

// ===== SEZONE I EPIZODE =====

data class TmdbSeason(
    val id: Int = 0,
    val season_number: Int = 0,
    val name: String? = null,
    val poster_path: String? = null,
    val episode_count: Int = 0,
    val air_date: String? = null
)

data class TmdbEpisode(
    val id: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    val episode_number: Int = 0,
    val season_number: Int = 0,
    val still_path: String? = null,
    val vote_average: Double = 0.0,
    val air_date: String? = null,
    val runtime: Int? = null
)

data class TmdbSeasonDetails(
    val id: Int = 0,
    val name: String? = null,
    val episodes: List<TmdbEpisode> = emptyList()
)

// ===== OSTALO =====

data class TmdbCredits(val cast: List<TmdbCast> = emptyList())

data class TmdbCast(
    val name: String? = null,
    val character: String? = null
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

fun TmdbHeroDetails.pickClearLogoUrl(): String? {
    val all = images?.logos?.filter { !it.file_path.isNullOrBlank() } ?: return null
    val pool = all.filter { it.file_path!!.endsWith(".png") }.ifEmpty { all }
    val pick = pool.firstOrNull { it.iso_639_1 == "sr" }
        ?: pool.firstOrNull { it.iso_639_1 == null }
        ?: pool.firstOrNull { it.iso_639_1 == "en" }
        ?: pool.maxByOrNull { it.vote_count }
    return pick?.file_path?.let { "https://image.tmdb.org/t/p/w500$it" }
}

fun TmdbHeroDetails.pickSerbianOverview(): String? {
    val sr = translations?.translations?.firstOrNull { it.iso_639_1 == "sr" }?.data
    return sr?.overview?.takeIf { it.isNotBlank() }
}

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

// IMDb ID: filmovi ga imaju na vrhu, serije u external_ids
fun TmdbHeroDetails.pickImdbId(): String? =
    imdb_id ?: external_ids?.imdb_id
