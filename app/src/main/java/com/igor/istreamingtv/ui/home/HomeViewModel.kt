package com.igor.istreamingtv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.pickCertification
import com.igor.istreamingtv.data.remote.pickClearLogoUrl
import com.igor.istreamingtv.data.remote.pickSerbianOverview
import com.igor.istreamingtv.data.repository.ContentRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    fun loadContent() {
        if (_uiState.value.movies.isNotEmpty() || _uiState.value.series.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            try {
                val trendingMovies = async { repository.getTrendingMovies() }
                val trendingSeries = async { repository.getTrendingSeries() }
                val popularMovies = async { repository.getPopularMovies() }
                val popularSeries = async { repository.getPopularSeries() }
                val topMovies = async { repository.getTopRatedMovies() }
                val topSeries = async { repository.getTopRatedSeries() }

                awaitAll(trendingMovies, trendingSeries, popularMovies, popularSeries, topMovies, topSeries)

                val catalogs = listOf(
                    Catalog("popular-movies", "Popularni filmovi", false, popularMovies.await()),
                    Catalog("popular-series", "Popularne serije", true, popularSeries.await()),
                    Catalog("top-movies", "Najbolje ocenjeni filmovi", false, topMovies.await()),
                    Catalog("top-series", "Najbolje ocenjene serije", true, topSeries.await()),
                    Catalog("trending-movies", "Trending filmovi ove nedelje", false, trendingMovies.await()),
                    Catalog("trending-series", "Trending serije ove nedelje", true, trendingSeries.await())
                )

                _uiState.value = HomeUiState(
                    isLoading = false,
                    movies = trendingMovies.await(),
                    series = trendingSeries.await(),
                    catalogs = catalogs
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun loadHeroExtras(movie: TmdbMovie, isTv: Boolean) {
        if (_uiState.value.heroExtras.containsKey(movie.id)) return

        viewModelScope.launch {
            try {
                val details = if (isTv) {
                    repository.getTvHeroDetails(movie.id)
                } else {
                    repository.getMovieHeroDetails(movie.id)
                }

                val extras = HeroExtras(
                    clearLogoUrl = details.pickClearLogoUrl(),
                    // Srpski opis ako postoji, inače originalni (EN) opis iz istog poziva
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
