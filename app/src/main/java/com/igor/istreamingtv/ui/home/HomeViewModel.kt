package com.igor.istreamingtv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.ContinueEntry
import com.igor.istreamingtv.data.ContinueWatchingStore
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
    val continueWatching: List<ContinueEntry> = emptyList(),
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

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContentRepository(BuildConfig.TMDB_API_KEY)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var homeVerticalPosition = ScrollPosition()
    private val catalogPositions = mutableMapOf<String, ScrollPosition>()

    companion object {
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

    /** ✅ Osveži "Nastavi gledanje" listu (poziva se svaki put kad se vratiš na početnu) */
    fun refreshContinueWatching() {
        val entries = ContinueWatchingStore.load(getApplication())
        if (entries != _uiState.value.continueWatching) {
            _uiState.value = _uiState.value.copy(continueWatching = entries)
        }
    }

    fun loadContent() {
        if (_uiState.value.movies.isNotEmpty() || _uiState.value.series.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            try {
                val trendingMovies = async { repository.getTrendingMovies() }
                val trendingSeries = async { repository.getTrendingSeries() }

                val movies = trendingMovies.await()
                val series = trendingSeries.await()

                _uiState.value = HomeUiState(
                    isLoading = false,
                    movies = movies,
                    series = series,
                    continueWatching = ContinueWatchingStore.load(getApplication())
                )

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
            // Preskoči neuspešan katalog
        }
    }

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
                // Tiho ignoriši
            }
        }
    }
}
