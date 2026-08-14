package com.igor.istreamingtv.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieResponse(
    val results: List<TmdbMovie>,
    val page: Int = 0,
    @SerialName("total_pages")
    val totalPages: Int = 0
)

@Serializable
data class TmdbMovie(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title")
    val originalTitle: String? = null,
    @SerialName("original_name")
    val originalName: String? = null,
    val overview: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("backdrop_path")
    val backdropPath: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double = 0.0,
    @SerialName("genre_ids")
    val genreIds: List<Int>? = null
)

val TmdbMovie.displayTitle: String
    get() = title?.takeIf { it.isNotBlank() }
        ?: name?.takeIf { it.isNotBlank() }
        ?: originalTitle
        ?: originalName
        ?: ""

val TmdbMovie.displayOverview: String get() = overview ?: ""
val TmdbMovie.displayDate: String get() = releaseDate ?: ""

@Serializable
data class TmdbMovieDetails(
    val id: Int,
    val title: String,
    val overview: String,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("backdrop_path")
    val backdropPath: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double = 0.0,
    val genres: List<Genre> = emptyList(),
    @SerialName("imdb_id")
    val imdbId: String? = null,
    val runtime: Int? = null,
    val tagline: String? = null
)

@Serializable
data class Genre(
    val id: Int,
    val name: String
)

// ===== HERO / DETALJI =====
@Serializable
data class TmdbHeroDetails(
    val valid: Int = 0,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("backdrop_path")
    val backdropPath: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String? = null,
    val runtime: Int? = null,
    @SerialName("episode_run_time")
    val episodeRunTime: List<Int>? = null,
    val genres: List<Genre>? = null,
    @SerialName("imdb_id")
    val imdbId: String? = null,
    @SerialName("external_ids")
    val externalIds: TmdbExternalIds? = null,
    @SerialName("belongs_to_collection")
    val belongsToCollection: TmdbCollection? = null,
    val seasons: List<TmdbSeason>? = null,
    val images: TmdbImages? = null,
    val translations: TmdbTranslations? = null,
    @SerialName("release_dates")
    val releaseDates: TmdbReleaseDates? = null,
    @SerialName("content_ratings")
    val contentRatings: TmdbContentRatings? = null,
    val credits: TmdbCredits? = null
)

@Serializable
data class TmdbExternalIds(
    @SerialName("imdb_id")
    val imdbId: String? = null
)

@Serializable
data class TmdbCollection(
    val valid: Int = 0,
    val name: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null
)

@Serializable
data class TmdbCollectionDetails(
    val valid: Int = 0,
    val name: String? = null,
    val parts: List<TmdbMovie> = emptyList()
)

@Serializable
data class TmdbSeason(
    val valid: Int = 0,
    @SerialName("season_number")
    val seasonNumber: Int = 0,
    val name: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("episode_count")
    val episodeCount: Int = 0,
    @SerialName("air_date")
    val airDate: String? = null
)

@Serializable
data class TmdbEpisode(
    val valid: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("episode_number")
    val episodeNumber: Int = 0,
    @SerialName("season_number")
    val seasonNumber: Int = 0,
    @SerialName("still_path")
    val stillPath: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double = 0.0,
    @SerialName("air_date")
    val airDate: String? = null,
    val runtime: Int? = null
)

@Serializable
data class TmdbSeasonDetails(
    val valid: Int = 0,
    val name: String? = null,
    val episodes: List<TmdbEpisode> = emptyList()
)

@Serializable
data class TmdbCredits(
    val cast: List<TmdbCast> = emptyList()
)

@Serializable
data class TmdbCast(
    val name: String? = null,
    val character: String? = null
)

@Serializable
data class TmdbImages(
    val logos: List<TmdbLogo> = emptyList()
)

@Serializable
data class TmdbLogo(
    @SerialName("file_path")
    val filePath: String? = null,
    @SerialName("iso_639_1")
    val iso6391: String? = null,
    @SerialName("vote_count")
    val voteCount: Int = 0
)

@Serializable
data class TmdbTranslations(
    val translations: List<TmdbTranslation> = emptyList()
)

@Serializable
data class TmdbTranslation(
    @SerialName("iso_639_1")
    val iso6391: String? = null,
    val data: TmdbTranslationData? = null
)

@Serializable
data class TmdbTranslationData(
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null
)

@Serializable
data class TmdbReleaseDates(
    val results: List<TmdbReleaseCountry> = emptyList()
)

@Serializable
data class TmdbReleaseCountry(
    @SerialName("iso_3166_1")
    val iso31661: String? = null,
    @SerialName("release_dates")
    val releaseDates: List<TmdbReleaseDate> = emptyList()
)

@Serializable
data class TmdbReleaseDate(
    val certification: String? = null
)

@Serializable
data class TmdbContentRatings(
    val results: List<TmdbContentRatingCountry> = emptyList()
)

@Serializable
data class TmdbContentRatingCountry(
    @SerialName("iso_3166_1")
    val iso31661: String? = null,
    val rating: String? = null
)

fun TmdbHeroDetails.pickClearLogoUrl(): String? {
    val all = images?.logos?.filter { !it.filePath.isNullOrBlank() } ?: return null
    val pool = all.filter { it.filePath!!.endsWith(".png") }.ifEmpty { all }
    val pick = pool.firstOrNull { it.iso6391 == "sr" }
        ?: pool.firstOrNull { it.iso6391 == null }
        ?: pool.firstOrNull { it.iso6391 == "en" }
        ?: pool.maxByOrNull { it.voteCount }
    return pick?.filePath?.let { "https://image.tmdb.org/t/p/w500$it" }
}

fun TmdbHeroDetails.pickSerbianOverview(): String? {
    val sr = translations?.translations?.firstOrNull { it.iso6391 == "sr" }?.data
    return sr?.overview?.takeIf { it.isNotBlank() }
}

fun TmdbHeroDetails.pickCertification(): String? {
    val movieCert = releaseDates?.results
        ?.firstOrNull { it.iso31661 == "US" }
        ?.releaseDates
        ?.firstNotNullOfOrNull { it.certification?.takeIf { c -> c.isNotBlank() } }

    val tvCert = contentRatings?.results
        ?.firstOrNull { it.iso31661 == "US" }
        ?.rating
        ?.takeIf { it.isNotBlank() }

    return movieCert ?: tvCert
}

fun TmdbHeroDetails.pickImdbId(): String? =
    imdbId ?: externalIds?.imdbId
