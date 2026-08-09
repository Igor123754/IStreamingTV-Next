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

data class ScrollPosition(
    val index: Int = 0,
    val offset: Int = 0
)

class HomeViewModel : ViewModel() {

    private val repository =
        ContentRepository(BuildConfig.TMDB_API_KEY)

    private val _uiState =
        MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    /*
     * Vertikalna pozicija Home ekrana.
     */
    private var homeVerticalPosition =
        ScrollPosition()

    /*
     * Horizontalne pozicije pojedinačnih kataloga.
     *
     * Svaki katalog ima svoju poziciju.
     */
    private val catalogPositions =
        mutableMapOf<String, ScrollPosition>()

    init {
        loadContent()
    }

    /*
     * ---------------------------------------------------------
     * HOME VERTICAL SCROLL
     * ---------------------------------------------------------
     */

    fun getHomeVerticalPosition(): ScrollPosition {
        return homeVerticalPosition
    }

    fun saveHomeVerticalPosition(
        index: Int,
        offset: Int
    ) {
        homeVerticalPosition =
            ScrollPosition(
                index = index,
                offset = offset
            )
    }

    /*
     * ---------------------------------------------------------
     * CATALOG HORIZONTAL SCROLL
     * ---------------------------------------------------------
     */

    fun getCatalogPosition(
        catalogId: String
    ): ScrollPosition {

        return catalogPositions[catalogId]
            ?: ScrollPosition()
    }

    fun saveCatalogPosition(
        catalogId: String,
        index: Int,
        offset: Int
    ) {

        catalogPositions[catalogId] =
            ScrollPosition(
                index = index,
                offset = offset
            )
    }

    /*
     * ---------------------------------------------------------
     * TMDB CONTENT
     * ---------------------------------------------------------
     */

    fun loadContent() {

        /*
         * Ako već imamo podatke, ne učitavamo ponovo
         * svaki put kada se Home ekran vrati.
         *
         * Time ostaju sačuvane i pozicije skrolovanja.
         */
        if (
            _uiState.value.movies.isNotEmpty() ||
            _uiState.value.series.isNotEmpty()
        ) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                HomeUiState(
                    isLoading = true
                )

            try {

                val movies =
                    repository.getTrendingMovies()

                val series =
                    repository.getTrendingSeries()

                _uiState.value =
                    HomeUiState(
                        isLoading = false,
                        movies = movies,
                        series = series
                    )

            } catch (e: Exception) {

                _uiState.value =
                    HomeUiState(
                        isLoading = false,
                        error = e.message
                            ?: "Unknown error"
                    )
            }
        }
    }
}
