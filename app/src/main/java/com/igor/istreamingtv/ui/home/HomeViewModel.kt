package com.igor.istreamingtv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.remote.TmdbHeroDetails
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.pickCertification
import com.igor.istreamingtv.data.remote.pickClearLogoUrl
import com.igor.istreamingtv.data.remote.pickSerbianOverview
import com.igor.istreamingtv.data.repository.ContentRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val movies: List<TmdbMovie> = emptyList(),
    val series: List<TmdbMovie> = emptyList(),
    val catalogs: List<Catalog> = emptyList(),
    val heroExtras: Map<Int, HeroExtras> = emptyMap(),
    val error: String? = null
)

data class Catalog(
    val id: String,
    val title: String,
    val isTv: Boolean,
    val items: List<TmdbMovie>
)

data class HeroExtras(
    val clearLogoUrl: String? = null,
    val overview: String? = null,
    val certification: String? = null
)

data class ScrollPosition(
    val index: Int = 0,
    val offset: Int = 0
)

class HomeViewModel : ViewModel() {

    private val repository = ContentRepository(BuildConfig.TMDB_API_KEY)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var homeVerticalPosition = ScrollPosition()
    private val catalogPositions = mutableMapOf<String, ScrollPosition>()

    companion object {
        // ✅ Limit naslova po katalogu — manje slika = brže na slabim uređajima
        private const val CATALOG_LIMIT = 10
    }

    init {
        loadContent()
    }

    fun getHomeVerticalPosition(): ScrollPosition = homeVerticalPosition

    fun saveHomeVerticalPosition(index: Int, offset: Int) {
        homeVerticalPosition = ScrollPosition(index = index, offset = offset)
    }

    fun getCatalogPosition(catalogId: String): ScrollPosition =
        catalogPositions[catalogId] ?: ScrollPosition()

    fun saveCatalogPosition(catalogId: String, index: Int, offset: Int) {
        catalogPositions[catalogId] = ScrollPosition(index = index, offset = offset)
    }

    /**
     * ✅ OPTIMIZOVANO ZA SLABE UREĐAJE (bez keširanja):
     * 1) Hero (trending) se učitava PRVO i odmah prikazuje
     * 2) Katalozi se učitavaju SEKVENCIJALNO (jedan po jedan) i
     *    dodaju u listu kako stižu — nema dugog čekanja i nema
     *    CPU spika od 6 paralelnih zahteva → stranica ostaje glatka
     */
    fun loadContent() {
        if (_uiState.value.movies.isNotEmpty() || _uiState.value.series.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            try {
                // 1) HERO ODMAH — samo 2 paralelna poziva
                val trendingMovies = async { repository.getTrendingMovies() }
                val trendingSeries = async { repository.getTrendingSeries() }

                val movies = trendingMovies.await()
                val series = trendingSeries.await()

                _uiState.value = HomeUiState(
                    isLoading = false,
                    movies = movies,
                    series = series
                )

                // 2) KATALOZI SEKVENCIJALNO — dodaju se kako stižu
                loadCatalog("popular-movies", "Popularni filmovi", false) {
                    repository.getPopularMovies()
                }
                loadCatalog("popular-series", "Popularne serije", true) {
                    repository.getPopularSeries()
                }
                loadCatalog("top-movies", "Najbolje ocenjeni filmovi", false) {
                    repository.getTopRatedMovies()
                }
                loadCatalog("top-series", "Najbolje ocenjene serije", true) {
                    repository.getTopRatedSeries()
                }
                loadCatalog("trending-movies", "Trending filmovi ove nedelje", false) {
                    repository.getTrendingMovies()
                }
                loadCatalog("trending-series", "Trending serije ove nedelje", true) {
                    repository.getTrendingSeries()
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    /**
     * ✅ Pomoćna suspend funkcija — ispravno poziva suspend fetch
     * i dodaje katalog u state čim stigne (bez keširanja)
     */
    private suspend fun loadCatalog(
        id: String,
        title: String,
        isTv: Boolean,
        fetch: suspend () -> List<TmdbMovie>
    ) {
        try {
            val items = fetch()
            if (items.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    catalogs = _uiState.value.catalogs + Catalog(
                        id = id,
                        title = title,
                        isTv = isTv,
                        items = items.take(CATALOG_LIMIT)
                    )
                )
            }
        } catch (_: Exception) {
            // Preskoči neuspešan katalog — stranica nastavlja da radi
        }
    }

    /** Hero dodaci: clearlogo + srpski opis + uzrast (pozadinski) */
    fun loadHeroExtras(movie: TmdbMovie, isTv: Boolean) {
        if (_uiState.value.heroExtras.containsKey(movie.id)) return

        viewModelScope.launch {
            try {
                val details: TmdbHeroDetails = if (isTv) {
                    repository.getTvHeroDetails(movie.id)
                } else {
                    repository.getMovieHeroDetails(movie.id)
                }

                val extras = HeroExtras(
                    clearLogoUrl = details.pickClearLogoUrl(),
                    overview = details.pickSerbianOverview() ?: details.overview,
                    certification = details.pickCertification()
                )

                _uiState.value = _uiState.value.copy(
                    heroExtras = _uiState.value.heroExtras + (movie.id to extras)
                )
            } catch (_: Exception) {
                // Tiho ignoriši — ostaje fallback
            }
        }
    }
}
