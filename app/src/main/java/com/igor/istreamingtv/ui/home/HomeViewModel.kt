package com.igor.istreamingtv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.ContinueEntry
import com.igor.istreamingtv.data.ContinueWatchingStore
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

class HomeViewModel(application: Application) : AndroidViewModel(application) {

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

    fun refreshContinueWatching() {
        val entries = ContinueWatchingStore.load(getApplication())
        if (entries != _uiState.value.continueWatching) {
            _uiState.value = _uiState.value.copy(continueWatching = entries)
        }
    }

    // ✅ Cinemeta katalogi — paralelno, brzo
    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val topMovies = async { repository.getCinemetaCatalog("movie", "top") }
                val topSeries = async { repository.getCinemetaCatalog("series", "top") }
                val popMovies = async { repository.getCinemetaCatalog("movie", "popular") }
                val popSeries = async { repository.getCinemetaCatalog("series", "popular") }

                val movies = topMovies.await()
                val series = topSeries.await()

                val catalogs = listOf(
                    Catalog("top-movies", "Popularni filmovi", movies.take(15)),
                    Catalog("top-series", "Popularne serije", series.take(15)),
                    Catalog("popular-movies", "Trending filmovi", popMovies.await().take(15)),
                    Catalog("popular-series", "Trending serije", popSeries.await().take(15))
                ).filter { it.items.isNotEmpty() }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movies = movies,
                    series = series,
                    catalogs = catalogs
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Greška pri učitavanju"
                )
            }
        }
    }

    fun loadHeroExtras(movie: TmdbMovie, isTv: Boolean) {
        if (_uiState.value.heroExtras.containsKey(movie.id) && movie.id != 0) return
        viewModelScope.launch {
            try {
                var tmdbId = movie.id
                if (tmdbId <= 0 && !movie.imdbId.isNullOrBlank()) {
                    tmdbId = repository.resolveTmdbId(movie.imdbId, isTv) ?: return@launch
                }
                val details = if (isTv) repository.getTvHeroDetails(tmdbId)
                else repository.getMovieHeroDetails(tmdbId)

                val extras = HeroExtras(
                    clearLogoUrl = details.pickClearLogoUrl(),
                    overview = details.pickSerbianOverview() ?: details.overview,
                    certification = details.pickCertification()
                )
                _uiState.value = _uiState.value.copy(
                    heroExtras = _uiState.value.heroExtras + (movie.id to extras)
                )
            } catch (_: Exception) {
            }
        }
    }
}
