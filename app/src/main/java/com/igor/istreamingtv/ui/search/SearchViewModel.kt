package com.igor.istreamingtv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val repository = ContentRepository(BuildConfig.TMDB_API_KEY)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<TmdbMovie>>(emptyList())
    val results: StateFlow<List<TmdbMovie>> = _results.asStateFlow()

    private val _suggestions = MutableStateFlow<List<TmdbMovie>>(emptyList())
    val suggestions: StateFlow<List<TmdbMovie>> = _suggestions.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    init {
        // ✅ Predlozi kad je polje prazno (Apple TV+ stil)
        viewModelScope.launch {
            try {
                _suggestions.value =
                    (repository.getTrendingMovies() + repository.getTrendingSeries())
                        .distinctBy { it.id }
            } catch (_: Exception) { }
        }

        // ✅ Debounce 400ms — pretraga kucajući, bez spama
        viewModelScope.launch {
            _query.debounce(400).collect { q ->
                if (q.isBlank()) {
                    _results.value = emptyList()
                    return@collect
                }
                _searching.value = true
                try {
                    _results.value = repository.searchMulti(q)
                } catch (_: Exception) {
                    _results.value = emptyList()
                }
                _searching.value = false
            }
        }
    }

    fun setQuery(q: String) {
        _query.value = q
    }
}
