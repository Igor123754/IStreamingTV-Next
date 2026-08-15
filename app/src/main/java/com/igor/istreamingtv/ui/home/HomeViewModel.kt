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
    val heroExtras: Map<String, HeroExtras> = emptyMap(),
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

    /**
     * ✅ APPLE TV+ STIL — bez keša:
     * 1) Hero ODMAH (2 paralelna poziva) → ekran trenutno vidljiv
     * 2) Prefetch hero extras u pozadini (TMDB id direktan — 1 poziv)
     * 3) Katalozi PARALELNO + PROGRESIVNO — redovi stižu kako se učitaju
     */
    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 1) HERO ODMAH
                val heroMovies = async { repository.getTrendingMovies() }
                val heroSeries = async { repository.getTrendingSeries() }
                val movies = heroMovies.await()
                val series = heroSeries.await()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movies = movies,
                    series = series
                )

                // 2) PREFETCH hero extras paralelno (rotacija momentalna)
                (movies.take(5).map { it to false } + series.take(5).map { it to true }).forEach { (m, isTv) ->
                    launch { loadHeroExtras(m, isTv) }
                }

                // 3) KATALOZI PARALELNO + PROGRESIVNO
                launch {
                    addCatalog("popular-movies", "Popularni filmovi", repository.getPopularMovies())
                }
                launch {
                    addCatalog("popular-series", "Popularne serije", repository.getPopularSeries())
                }
                launch {
                    addCatalog("top-movies", "Najbolje ocenjeni filmovi", repository.getTopRatedMovies())
                }
                launch {
                    addCatalog("top-series", "Najbolje ocenjene serije", repository.getTopRatedSeries())
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Greška pri učitavanju"
                )
            }
        }
    }

    // ✅ Red se dodaje čim stigne — bez čekanja ostalih
    private suspend fun addCatalog(id: String, title: String, items: List<TmdbMovie>) {
        if (items.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            catalogs = _uiState.value.catalogs + Catalog(id, title, items.take(12))
        )
    }

    // ✅ 1 poziv po naslovu (TMDB id direktan — bez find turе)
    fun loadHeroExtras(movie: TmdbMovie, isTv: Boolean) {
        val key = movie.id.toString()
        if (_uiState.value.heroExtras.containsKey(key)) return
        viewModelScope.launch {
            try {
                val details = if (isTv) repository.getTvHeroDetails(movie.id)
                else repository.getMovieHeroDetails(movie.id)

                val extras = HeroExtras(
                    clearLogoUrl = details.pickClearLogoUrl(),
                    overview = details.pickSerbianOverview() ?: details.overview,
                    certification = details.pickCertification()
                )
                _uiState.value = _uiState.value.copy(
                    heroExtras = _uiState.value.heroExtras + (key to extras)
                )
            } catch (_: Exception) {
            }
        }
    }
}
