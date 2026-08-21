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

private interface GenreApi {
    @GET("discover/movie")
    suspend fun discover(
        @Query("with_genres") genreId: Int,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("vote_count.gte") voteCountGte: Int = 50,
        @Query("language") language: String = "sr-RS",
        @Query("page") page: Int
    ): MovieResponse
}

data class GenreCatalog(
    val id: Int,
    val title: String,
    val items: List<TmdbMovie>
)

/**
 * ✅ FILMOVI — 19 žanrova, PUNI katalozi (15 filmova po redu):
 *    - 2 strane po žanru (~40 kandidata)
 *    - prvo SVEŽI filmovi (bez dupliranja), pa DOPUNA ako je red kratak
 *    - progresivno učitavanje (chunk po 3) — glatko na slabim TV
 */
class MoviesViewModel : ViewModel() {

    private val api = TmdbClient
        .createRetrofit(BuildConfig.TMDB_API_KEY)
        .create(GenreApi::class.java)

    private val _catalogs = MutableStateFlow<List<GenreCatalog>>(emptyList())
    val catalogs: StateFlow<List<GenreCatalog>> = _catalogs.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

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
            val seen = Collections.synchronizedSet(mutableSetOf<Int>())
            _loading.value = true

            genres.chunked(3).forEach { chunk ->
                val loaded = chunk.map { (id, title) ->
                    async(Dispatchers.IO) {
                        try {
                            val p1 = api.discover(genreId = id, page = 1)
                            val p2 = try {
                                api.discover(genreId = id, page = 2)
                            } catch (_: Exception) { null }

                            val all = (p1.results + (p2?.results ?: emptyList()))
                                .distinctBy { it.id }
                                .filter { it.posterPath != null }

                            // ✅ Prvo sveži (ne viđeni), ostali kao dopuna
                            val fresh = mutableListOf<TmdbMovie>()
                            val dup = mutableListOf<TmdbMovie>()
                            for (m in all) {
                                if (seen.add(m.id)) fresh.add(m) else dup.add(m)
                            }
                            // ✅ PUN RED: sveži + dopuna do 15
                            val items = (fresh + dup).take(15)

                            GenreCatalog(id, title, items)
                        } catch (_: Exception) {
                            GenreCatalog(id, title, emptyList())
                        }
                    }
                }.awaitAll()

                _catalogs.value = _catalogs.value + loaded.filter { it.items.size >= 4 }
            }

            _loading.value = false
        }
    }
}
