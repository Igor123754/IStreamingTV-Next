package com.igor.istreamingtv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.remote.TmdbHeroDetails
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.stremio.StremioStream
import com.igor.istreamingtv.data.repository.ContentRepository
import com.igor.istreamingtv.data.repository.StreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailsUiState(
    val isLoading: Boolean = true,
    val details: TmdbHeroDetails? = null,
    val streams: List<StremioStream> = emptyList(),
    val error: String? = null
)

class MovieDetailsViewModel : ViewModel() {

    private val tmdbRepository = ContentRepository(BuildConfig.TMDB_API_KEY)
    private val streamRepository = StreamRepository(BuildConfig.TMDB_API_KEY)

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    fun load(movie: TmdbMovie, isTv: Boolean) {
        viewModelScope.launch {
            _uiState.value = DetailsUiState(isLoading = true)
            try {
                val details = if (isTv) {
                    tmdbRepository.getTvHeroDetails(movie.id)
                } else {
                    tmdbRepository.getMovieHeroDetails(movie.id)
                }

                // Stream-ovi samo za filmove (preko imdb_id)
                val streams = if (!isTv && !details.imdb_id.isNullOrBlank()) {
                    try {
                        streamRepository.getStreamsForMovie(details.imdb_id)
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                _uiState.value = DetailsUiState(
                    isLoading = false,
                    details = details,
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
