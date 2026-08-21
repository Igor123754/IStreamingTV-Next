package com.igor.istreamingtv.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.remote.MovieResponse
import com.igor.istreamingtv.data.remote.TmdbClient
import com.igor.istreamingtv.data.remote.TmdbMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Collections

/** ✅ Zaseban API za discover po žanru (ne dira TmdbApi/ContentRepository) */
private interface GenreApi {
    @GET("discover/movie")
    suspend fun discover(
        @Query("with_genres") genreId: Int,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("vote_count.gte") voteCountGte: Int = 100,
        @Query("language") language: String = "sr-RS",
        @Query("page") page: Int = 1
    ): MovieResponse
}

data class GenreCatalog(
    val id: Int,
    val title: String,
    val items: List<TmdbMovie>
)

/**
 * ✅ FILMOVI — 19 žanr kataloga, BEZ DUPLIRANJA (globalni seen set),
 *    progresivno učitavanje (chunk po 4 paralelna zahteva) — glatko na slabim TV.
 */
class MoviesViewModel : ViewModel() {

    private val api = TmdbClient
        .createRetrofit(BuildConfig.TMDB_API_KEY)
        .create(GenreApi::class.java)

    private val _catalogs = MutableStateFlow<List<GenreCatalog>>(emptyList())
    val catalogs: StateFlow<List<GenreCatalog>> = _catalogs.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // ✅ 19 žanrova — što više kataloga
    private val genres = listOf(
        28 to "Akcija",
        35 to "Komedija",
        18 to "Drama",
        53 to "Triler",
        27 to "Horor",
        878 to "Naučna fantastika",
        16 to "Animacija",
        10751 to "Porodični",
        80 to "Krimi",
        9648 to "Misterija",
        12 to "Avantura",
        14 to "Fantazija",
        10749 to "Romansa",
        99 to "Dokumentarni",
        36 to "Istorijski",
        10402 to "Muzički",
        37 to "Vestern",
        10752 to "Ratni",
        10770 to "TV filmovi"
    )

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            // ✅ Thread-safe set za deduplikaciju između žanrova
            val seen = Collections.synchronizedSet(mutableSetOf<Int>())
            _loading.value = true

            // ✅ Chunk po 4 paralelna zahteva — redovi stižu jedan po jedan
            genres.chunked(4).forEach { chunk ->
                val loaded = chunk.map { (id, title) ->
                    async(Dispatchers.IO) {
                        try {
                            val res = api.discover(genreId = id)
                            val filtered = res.results.filter { m ->
                                m.posterPath != null && seen.add(m.id)
                            }
                            GenreCatalog(id, title, filtered.take(15))
                        } catch (_: Exception) {
                            GenreCatalog(id, title, emptyList())
                        }
                    }
                }.awaitAll()

                _catalogs.value = _catalogs.value + loaded.filter { it.items.isNotEmpty() }
            }

            _loading.value = false
        }
    }
}
