package com.igor.istreamingtv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.remote.TmdbEpisode
import com.igor.istreamingtv.data.remote.TmdbHeroDetails
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.TmdbSeason
import com.igor.istreamingtv.data.remote.stremio.StremioStream
import com.igor.istreamingtv.data.repository.ContentRepository
import com.igor.istreamingtv.data.repository.StreamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class DetailsUiState(
    val isLoading: Boolean = true,
    val details: TmdbHeroDetails? = null,
    val streams: List<StremioStream> = emptyList(),
    val collectionName: String? = null,
    val collectionParts: List<TmdbMovie> = emptyList(),
    val seasons: List<TmdbSeason> = emptyList(),
    val selectedSeasonNumber: Int = 0,
    val episodes: List<TmdbEpisode> = emptyList(),
    val similar: List<TmdbMovie> = emptyList(),
    val error: String? = null
)

class MovieDetailsViewModel : ViewModel() {

    private val tmdbRepository = ContentRepository(BuildConfig.TMDB_API_KEY)
    private val streamRepository = StreamRepository(BuildConfig.TMDB_API_KEY)
    private val okHttpClient = OkHttpClient()
    private val gson = Gson()

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

                val similarDeferred = async {
                    try {
                        if (isTv) tmdbRepository.getSimilarSeries(movie.id)
                        else tmdbRepository.getSimilarMovies(movie.id)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                val streams = if (!isTv && !details.imdb_id.isNullOrBlank()) {
                    try {
                        streamRepository.getStreamsForMovie(details.imdb_id)
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

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
                            // Nema kolekcije
                        }
                    }
                }

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
                    episodes = episodes,
                    similar = similarDeferred.await()
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

    /**
     * Stream-ovi za EPIZODU serije — Cinemeta addon format:
     * stream/series/{imdb_id}:{sezona}:{epizoda}.json
     */
    suspend fun getEpisodeStreams(imdbId: String, season: Int, episode: Int): List<StremioStream> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://v3-cinemeta.strem.io/stream/series/$imdbId:$season:$episode.json"
                val request = Request.Builder().url(url).get().build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string()
                response.close()
                if (body.isNullOrBlank()) return@withContext emptyList()

                val obj = JsonParser.parseString(body).asJsonObject
                val arr = obj.getAsJsonArray("streams") ?: return@withContext emptyList()

                arr.mapNotNull { element ->
                    try {
                        gson.fromJson(element, StremioStream::class.java)
                    } catch (_: Exception) {
                        null
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
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
