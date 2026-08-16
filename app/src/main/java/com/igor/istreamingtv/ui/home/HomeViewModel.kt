package com.igor.istreamingtv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.ContinueEntry
import com.igor.istreamingtv.data.ContinueWatchingStore
import com.igor.istreamingtv.data.livetv.LiveChannel
import com.igor.istreamingtv.data.livetv.LiveTvRepository
import com.igor.istreamingtv.data.livetv.LiveTvSession
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.pickCertification
import com.igor.istreamingtv.data.remote.pickClearLogoUrl
import com.igor.istreamingtv.data.remote.pickSerbianOverview
import com.igor.istreamingtv.data.repository.ContentRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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
    val liveChannels: List<LiveChannel> = emptyList(),
    val liveEpg: Map<String, List<com.igor.istreamingtv.data.livetv.EpgProgram>> = emptyMap(),
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContentRepository(BuildConfig.TMDB_API_KEY)
    private val liveRepository = LiveTvRepository()

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
        startLiveRefreshLoop()
    }

    fun refreshContinueWatching() {
        val entries = ContinueWatchingStore.load(getApplication())
        if (entries != _uiState.value.continueWatching) {
            _uiState.value = _uiState.value.copy(continueWatching = entries)
        }
    }

    // ✅ AUTO OSVEŽAVANJE EPG-a na 15 min — budući programi se pojavljuju
    //    čim ih provider objavi, bez restarta aplikacije
    private fun startLiveRefreshLoop() {
        viewModelScope.launch {
            while (true) {
                delay(15 * 60_000L)
                refreshLive()
            }
        }
    }

    fun refreshLive() {
        viewModelScope.launch {
            val data = liveRepository.load()
            if (data != null && data.channels.isNotEmpty()) {
                LiveTvSession.channels = data.channels
                LiveTvSession.epg = data.epg
                _uiState.value = _uiState.value.copy(
                    liveChannels = data.channels,
                    liveEpg = data.epg
                )
            }
        }
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val heroMovies = async { repository.getTrendingMovies() }
                val heroSeries = async { repository.getTrendingSeries() }
                val movies = heroMovies.await()
                val series = heroSeries.await()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movies = movies,
                    series = series
                )

                (movies.take(5).map { it to false } + series.take(5).map { it to true }).forEach { (m, isTv) ->
                    launch { loadHeroExtras(m, isTv) }
                }

                launch { addCatalog("popular-movies", "Popularni filmovi", repository.getPopularMovies()) }
                launch { addCatalog("popular-series", "Popularne serije", repository.getPopularSeries()) }
                launch { addCatalog("top-movies", "Najbolje ocenjeni filmovi", repository.getTopRatedMovies()) }
                launch { addCatalog("top-series", "Najbolje ocenjene serije", repository.getTopRatedSeries()) }

                // ✅ LIVE TV — odmah + sesija za player
                launch {
                    val data = liveRepository.load()
                    if (data != null && data.channels.isNotEmpty()) {
                        LiveTvSession.channels = data.channels
                        LiveTvSession.epg = data.epg
                        _uiState.value = _uiState.value.copy(
                            liveChannels = data.channels,
                            liveEpg = data.epg
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Greška pri učitavanju"
                )
            }
        }
    }

    private suspend fun addCatalog(id: String, title: String, items: List<TmdbMovie>) {
        if (items.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            catalogs = _uiState.value.catalogs + Catalog(id, title, items.take(12))
        )
    }

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
