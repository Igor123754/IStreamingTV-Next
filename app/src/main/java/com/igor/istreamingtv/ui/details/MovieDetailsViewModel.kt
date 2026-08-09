package com.igor.istreamingtv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.data.remote.TmdbMovieDetails
import com.igor.istreamingtv.data.remote.stremio.StremioStream
import com.igor.istreamingtv.data.repository.StreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailsUiState(
    val isLoading: Boolean = true,
    val movieDetails: TmdbMovieDetails? = null,
    val streams: List<StremioStream> = emptyList(),
    val error: String? = null
)

class MovieDetailsViewModel(
    private val streamRepository: StreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    fun loadMovieDetails(movieId: Int) {
        viewModelScope.launch {
            _uiState.value = DetailsUiState(isLoading = true)

            try {
                // 1. Učitaj detalje sa TMDB
                val details = streamRepository.getMovieDetails(movieId)

                // 2. Ako imamo IMDB ID, učitaj i streamove
                val streams = if (!details.imdb_id.isNullOrEmpty()) {
                    streamRepository.getStreamsForMovie(details.imdb_id)
                } else {
                    emptyList()
                }

                _uiState.value = DetailsUiState(
                    isLoading = false,
                    movieDetails = details,
                    streams = streams
                )

            } catch (e: Exception) {
                _uiState.value = DetailsUiState(
                    isLoading = false,
                    error = e.message ?: "Greška pri učitavanju"
                )
            }
        }
    }
}
