package com.igor.istreamingtv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.pickCertification
import com.igor.istreamingtv.data.remote.pickClearLogoUrl
import com.igor.istreamingtv.data.remote.pickSerbianOverview
import com.igor.istreamingtv.data.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val movies: List<TmdbMovie> = emptyList(),
    val series: List<TmdbMovie> = emptyList(),
    val heroExtras: Map<Int, HeroExtras> = emptyMap(),
    val error: String? = null
)

// Dodaci za hero: clearlogo, srpski opis, uzrastna oznaka
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
                val movies = repository.getTrendingMovies()
                val series = repository.getTrendingSeries()
                _uiState.value = HomeUiState(isLoading = false, movies = movies, series = series)
            } catch (e: Exception) {
                _uiState.value = HomeUiState(isLoading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    // Učitava clearlogo + srpski opis + uzrastnu oznaku za trenutni hero naslov
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
                    overview = details.pickSerbianOverview(),
                    certification = details.pickCertification()
                )

                _uiState.value = _uiState.value.copy(
                    heroExtras = _uiState.value.heroExtras + (movie.id to extras)
                )
            } catch (_: Exception) {
                // Tiho ignoriši — ostaje fallback (originalni naslov/opis)
            }
        }
    }
}
