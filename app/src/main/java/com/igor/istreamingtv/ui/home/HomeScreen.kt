package com.igor.istreamingtv.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.ui.components.MovieCard
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.theme.Background
import com.igor.istreamingtv.ui.theme.TextPrimary
import com.igor.istreamingtv.ui.theme.TextSecondary

private const val BACKDROP_URL =
    "https://image.tmdb.org/t/p/w1280"

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        when {

            state.isLoading -> {
                LoadingScreen()
            }

            state.error != null -> {
                ErrorScreen(
                    message = state.error ?: "Nepoznata greška",
                    onRetry = viewModel::loadContent
                )
            }

            else -> {
                HomeContent(
                    movies = state.movies,
                    series = state.series
                )
            }
        }
    }
}
