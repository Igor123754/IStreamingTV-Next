package com.igor.istreamingtv.data.remote

import com.google.gson.annotations.SerializedName

data class TmdbResponse(
    @SerializedName("results")
    val results: List<TmdbMovie>
)

data class TmdbMovie(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("overview")
    val overview: String?,

    @SerializedName("poster_path")
    val posterPath: String?,

    @SerializedName("backdrop_path")
    val backdropPath: String?,

    @SerializedName("release_date")
    val releaseDate: String?,

    @SerializedName("first_air_date")
    val firstAirDate: String?
) {
    val displayTitle: String
        get() = title ?: name ?: "Unknown"

    val displayDate: String
        get() = releaseDate ?: firstAirDate ?: ""
}
