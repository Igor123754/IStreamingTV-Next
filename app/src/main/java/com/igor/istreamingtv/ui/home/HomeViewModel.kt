package com.igor.istreamingtv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val movies: List<TmdbMovie> = emptyList(),
    val series: List<TmdbMovie> = emptyList(),
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val repository =
        ContentRepository(BuildConfig.TMDB_API_KEY)

    private val _uiState =
        MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    /*
     * Home scroll position.
     *
     * Ovo čuvamo u ViewModel-u zato što se HomeScreen
     * uklanja iz Composition kada otvorimo Details.
     *
     * Kada se vratimo na Home, HomeScreen će pročitati
     * ove vrednosti i vratiti LazyColumn na isto mesto.
     */
    private var homeScrollIndex: Int = 0

    private var homeScrollOffset: Int = 0

    fun getHomeScrollIndex(): Int {
        return homeScrollIndex
    }

    fun getHomeScrollOffset(): Int {
        return homeScrollOffset
    }

    fun saveHomeScrollPosition(
        index: Int,
        offset: Int
    ) {
        homeScrollIndex = index
        homeScrollOffset = offset
    }

    init {
        loadContent()
    }

    fun loadContent() {

        /*
         * Ako se Home ponovo pojavi nakon Details,
         * ViewModel je isti i podaci ostaju dostupni.
         *
         * Ne resetujemo scroll poziciju ovde.
         */

        if (
            _uiState.value.movies.isNotEmpty() ||
            _uiState.value.series.isNotEmpty()
        ) {
            return
        }

        viewModelScope.launch {

            _uiState.value = HomeUiState(
                isLoading = true
            )

            try {

                val movies =
                    repository.getTrendingMovies()

                val series =
                    repository.getTrendingSeries()

                _uiState.value = HomeUiState(
                    isLoading = false,
                    movies = movies,
                    series = series
                )

            } catch (e: Exception) {

                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}
