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

    init {
        loadContent()
    }

    fun loadContent() {

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
