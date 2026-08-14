package com.igor.istreamingtv.ui.home

import androidx.lifecycle.ViewModel
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

data class HeroExtras(
    val clearLogoUrl: String?,
    val overview: String?,
    val certification: String?
)

data class Catalog(
    val id: String,
    val title: String,
    val items: List<TmdbMovie>
)

data class ScrollPosition(val index: Int, val offset: Int)

data class HomeUiState(
    val isLoading: Boolean = true,
    val movies: List<TmdbMovie> = emptyList(),
    val series: List<TmdbMovie> = emptyList(),
    val catalogs: List<Catalog> = emptyList(),
    val continueWatching: List<ContinueEntry> = emptyList(),
    val heroExtras: Map<Int, HeroExtras> = emptyMap(),
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val repository = ContentRepository(BuildConfig.TMDB_API_KEY)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var homeVerticalPosition = ScrollPosition(0, 0)
    private val catalogPositions = mutableMapOf<String, ScrollPosition>()

    fun getHomeVerticalPosition(): ScrollPosition = homeVerticalPosition
    fun saveHomeVerticalPosition(index: Int, offset: Int) {
        homeVerticalPosition = ScrollPosition(index, offset)
    }

    fun getCatalogPosition(catalogId: String): ScrollPosition =
        catalogPositions[catalogId] ?: ScrollPosition(0, 0)
    fun saveCatalogPosition(catalogId: String, index: Int, offset: Int) {
        catalogPositions[catalogId] = ScrollPosition(index, offset)
    }

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            try {
                // ✅ PARALELNO učitavanje (Cinemeta za kataloge)
                val moviesDeferred = async { repository.getCinemetaCatalog("movie", "top") }
                val seriesDeferred = async { repository.getCinemetaCatalog("series", "top") }
                val trendingMoviesDeferred = async { repository.getCinemetaCatalog("movie", "popular") }
                val trendingSeriesDeferred = async { repository.getCinemetaCatalog("series", "popular") }

                val movies = moviesDeferred.await()
                val series = seriesDeferred.await()
                val trendingMovies = trendingMoviesDeferred.await()
                val trendingSeries = trendingSeriesDeferred.await()

                // ✅ Katalozi sa Cinemeta (brži od TMDB-a)
                val catalogs = listOf(
                    Catalog("trending-movies", "U trendu — filmovi", trendingMovies.take(15)),
                    Catalog("trending-series", "U trendu — serije", trendingSeries.take(15)),
                    Catalog("popular-movies", "Popularni filmovi", movies.take(15)),
                    Catalog("popular-series", "Popularne serije", series.take(15))
                ).filter { it.items.isNotEmpty() }

                _uiState.value = HomeUiState(
                    isLoading = false,
                    movies = movies,
                    series = series,
                    catalogs = catalogs
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = e.message ?: "Greška pri učitavanju"
                )
            }
        }
    }

    fun refreshContinueWatching() {
        // Placeholder — implementiraće se kad bude bilo potrebno
    }

    fun loadHeroExtras(movie: TmdbMovie, isTv: Boolean) {
        viewModelScope.launch {
            try {
                val details = if (isTv) {
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
                // Ignoriši greške u hero extras
            }
        }
    }
}
