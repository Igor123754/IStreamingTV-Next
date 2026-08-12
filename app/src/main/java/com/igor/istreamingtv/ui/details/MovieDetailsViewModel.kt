package com.igor.istreamingtv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.remote.TmdbEpisode
import com.igor.istreamingtv.data.remote.TmdbHeroDetails
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.TmdbSeason
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
    val collectionName: String? = null,
    val collectionParts: List<TmdbMovie> = emptyList(),
    val seasons: List<TmdbSeason> = emptyList(),
    val selectedSeasonNumber: Int = 0,
    val episodes: List<TmdbEpisode> = emptyList(),
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

                // Stream-ovi samo za filmove
                val streams = if (!isTv && !details.imdb_id.isNullOrBlank()) {
                    try {
                        streamRepository.getStreamsForMovie(details.imdb_id)
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                // FILMOVI: nastavci / franšiza
                var collectionName: String? = null
                var collectionParts = emptyList<TmdbMovie>()
                if (!isTv) {
                    val collectionId = details.belongs_to_collection?.id
                    if (collectionId != null) {
                        try {
                            val collection = tmdbRepository.getCollectionDetails(collectionId)
                            collectionName = collection.name
                            collectionParts = collection.parts
                                .filter { it.id != movie.id }
                                .sortedBy { it.release_date ?: "" }
                        } catch (_: Exception) {
                            // Nema kolekcije — ostaje prazno
                        }
                    }
                }

                // SERIJE: sezone + epizode
                var seasons = emptyList<TmdbSeason>()
                var selectedSeason = 0
                var episodes = emptyList<TmdbEpisode>()
                if (isTv) {
                    seasons = details.seasons ?: emptyList()
                    selectedSeason = seasons.firstOrNull { it.season_number > 0 }?.season_number
                        ?: seasons.firstOrNull()?.season_number
                        ?: 0
                    episodes = fetchEpisodes(movie.id, selectedSeason)
                }

                _uiState.value = DetailsUiState(
                    isLoading = false,
                    details = details,
                    streams = streams,
                    collectionName = collectionName,
                    collectionParts = collectionParts,
                    seasons = seasons,
                    selectedSeasonNumber = selectedSeason,
                    episodes = episodes
                )
            } catch (e: Exception) {
                _uiState.value = DetailsUiState(
                    isLoading = false,
                    error = e.message ?: "Greška pri učitavanju"
                )
            }
        }
    }

    fun selectSeason(tvId: Int, seasonNumber: Int) {
        if (_uiState.value.selectedSeasonNumber == seasonNumber) return
        viewModelScope.launch {
            val episodes = fetchEpisodes(tvId, seasonNumber)
            _uiState.value = _uiState.value.copy(
                selectedSeasonNumber = seasonNumber,
                episodes = episodes
            )
        }
    }

    private suspend fun fetchEpisodes(tvId: Int, seasonNumber: Int): List<TmdbEpisode> {
        return try {
            tmdbRepository.getSeasonDetails(tvId, seasonNumber).episodes
        } catch (_: Exception) {
            emptyList()
        }
    }
}
