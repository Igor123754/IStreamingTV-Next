package com.igor.istreamingtv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.remote.StreamPicker
import com.igor.istreamingtv.data.remote.SubtitleFetcher
import com.igor.istreamingtv.data.remote.TmdbEpisode
import com.igor.istreamingtv.data.remote.TmdbHeroDetails
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.TmdbSeason
import com.igor.istreamingtv.data.remote.pickImdbId
import com.igor.istreamingtv.data.repository.ContentRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailsUiState(
    val isLoading: Boolean = true,
    val details: TmdbHeroDetails? = null,
    val collectionName: String? = null,
    val collectionParts: List<TmdbMovie> = emptyList(),
    val seasons: List<TmdbSeason> = emptyList(),
    val selectedSeasonNumber: Int = 0,
    val episodes: List<TmdbEpisode> = emptyList(),
    val similar: List<TmdbMovie> = emptyList(),
    val preparingStreams: Boolean = false,
    val movieCandidates: List<StreamPicker.Candidate> = emptyList(),
    val episodeCandidates: Map<String, List<StreamPicker.Candidate>> = emptyMap(),
    // Srpski titlovi (prefetch u pozadini)
    val movieSubtitleUrl: String? = null,
    val episodeSubtitleUrls: Map<String, String> = emptyMap(),
    val error: String? = null
)

class MovieDetailsViewModel : ViewModel() {

    private val tmdbRepository = ContentRepository(BuildConfig.TMDB_API_KEY)

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    fun load(movie: TmdbMovie, isTv: Boolean) {
        viewModelScope.launch {
            _uiState.value = DetailsUiState(isLoading = true, preparingStreams = true)
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

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    details = details,
                    collectionName = collectionName,
                    collectionParts = collectionParts,
                    seasons = seasons,
                    selectedSeasonNumber = selectedSeason,
                    episodes = episodes
                )

                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(similar = similarDeferred.await())
                }

                // STREAM-OVI + TITLOVI u pozadini
                val imdb = details.pickImdbId()
                if (!imdb.isNullOrBlank()) {
                    if (!isTv) {
                        prepareMovieStreams(imdb)
                        prepareMovieSubtitle(imdb)
                    } else {
                        episodes.firstOrNull()?.let { first ->
                            prepareEpisodeStreams(imdb, first)
                            prepareEpisodeSubtitle(imdb, first)
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(preparingStreams = false)
                }
            } catch (e: Exception) {
                _uiState.value = DetailsUiState(
                    isLoading = false,
                    preparingStreams = false,
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
            val imdb = _uiState.value.details?.pickImdbId() ?: return@launch
            episodes.firstOrNull()?.let { first ->
                prepareEpisodeStreams(imdb, first)
                prepareEpisodeSubtitle(imdb, first)
            }
        }
    }

    suspend fun candidatesForEpisode(season: Int, episode: Int): List<StreamPicker.Candidate> {
        val key = "$season:$episode"
        _uiState.value.episodeCandidates[key]?.let { return it }

        val imdb = _uiState.value.details?.pickImdbId() ?: return emptyList()
        val candidates = StreamPicker.getCandidates("series", imdb, season, episode)
        val prepared = StreamPicker.prepare(candidates)
        _uiState.value = _uiState.value.copy(
            episodeCandidates = _uiState.value.episodeCandidates + (key to prepared)
        )
        return prepared
    }

    /** Srpski titl za epizodu: keš → ili fetch */
    suspend fun subtitleForEpisode(season: Int, episode: Int): String? {
        val key = "$season:$episode"
        _uiState.value.episodeSubtitleUrls[key]?.let { return it }

        val imdb = _uiState.value.details?.pickImdbId() ?: return null
        val sub = safeSubtitle("series", imdb, season, episode) ?: return null
        _uiState.value = _uiState.value.copy(
            episodeSubtitleUrls = _uiState.value.episodeSubtitleUrls + (key to sub)
        )
        return sub
    }

    // ===== PRIVATE =====

    private fun prepareMovieStreams(imdb: String) {
        viewModelScope.launch {
            val candidates = StreamPicker.getCandidates("movie", imdb)
            val prepared = StreamPicker.prepare(candidates)
            _uiState.value = _uiState.value.copy(
                preparingStreams = false,
                movieCandidates = prepared
            )
        }
    }

    private fun prepareMovieSubtitle(imdb: String) {
        viewModelScope.launch {
            val sub = safeSubtitle("movie", imdb) ?: return@launch
            _uiState.value = _uiState.value.copy(movieSubtitleUrl = sub)
        }
    }

    private fun prepareEpisodeStreams(imdb: String, episode: TmdbEpisode) {
        viewModelScope.launch {
            val key = "${episode.season_number}:${episode.episode_number}"
            if (_uiState.value.episodeCandidates.containsKey(key)) return@launch
            val candidates = StreamPicker.getCandidates(
                "series", imdb, episode.season_number, episode.episode_number
            )
            val prepared = StreamPicker.prepare(candidates)
            _uiState.value = _uiState.value.copy(
                preparingStreams = false,
                episodeCandidates = _uiState.value.episodeCandidates + (key to prepared)
            )
        }
    }

    private fun prepareEpisodeSubtitle(imdb: String, episode: TmdbEpisode) {
        viewModelScope.launch {
            val key = "${episode.season_number}:${episode.episode_number}"
            if (_uiState.value.episodeSubtitleUrls.containsKey(key)) return@launch
            val sub = safeSubtitle("series", imdb, episode.season_number, episode.episode_number)
                ?: return@launch
            _uiState.value = _uiState.value.copy(
                episodeSubtitleUrls = _uiState.value.episodeSubtitleUrls + (key to sub)
            )
        }
    }

    private suspend fun safeSubtitle(
        type: String,
        imdb: String,
        season: Int = -1,
        episode: Int = -1
    ): String? = try {
        SubtitleFetcher.getBestSerbianSubtitle(type, imdb, season, episode)
    } catch (_: Exception) {
        null
    }

    private suspend fun fetchEpisodes(tvId: Int, seasonNumber: Int): List<TmdbEpisode> {
        return try {
            tmdbRepository.getSeasonDetails(tvId, seasonNumber).episodes
        } catch (_: Exception) {
            emptyList()
        }
    }
}
