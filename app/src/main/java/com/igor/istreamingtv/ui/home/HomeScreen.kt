package com.igor.istreamingtv.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@Composable
private fun LoadingScreen() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        CircularProgressIndicator(
            color = TextPrimary
        )
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Nije moguće učitati katalog",
            color = TextPrimary
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = message,
            color = TextSecondary
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        TvFocusableButton(
            modifier = Modifier
                .width(170.dp)
                .height(54.dp),
            onClick = onRetry
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Pokušaj ponovo",
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    movies: List<TmdbMovie>,
    series: List<TmdbMovie>
) {

    val scrollState = rememberLazyListState()

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(30.dp)
    ) {

        item {

            TopNavigation()
        }

        if (movies.isNotEmpty()) {

            item {

                Hero(
                    movie = movies.first()
                )
            }
        }

        if (movies.isNotEmpty()) {

            item {

                MovieSection(
                    title = "Trending filmovi",
                    movies = movies
                )
            }
        }

        if (series.isNotEmpty()) {

            item {

                MovieSection(
                    title = "Popularne serije",
                    movies = series
                )
            }
        }

        item {

            Spacer(
                modifier = Modifier.height(60.dp)
            )
        }
    }
}

@Composable
private fun TopNavigation() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 48.dp,
                vertical = 24.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "IStreamingTV",
            color = TextPrimary
        )

        Spacer(
            modifier = Modifier.width(40.dp)
        )

        NavigationButton(
            text = "Home"
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        NavigationButton(
            text = "Movies"
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        NavigationButton(
            text = "Series"
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )

        NavigationButton(
            text = "Search"
        )
    }
}

@Composable
private fun NavigationButton(
    text: String
) {

    TvFocusableButton(
        modifier = Modifier
            .width(100.dp)
            .height(48.dp),
        onClick = {
            // Navigacija dolazi u sledećem koraku.
        }
    ) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = text,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun Hero(
    movie: TmdbMovie
) {

    val backdropPath = movie.backdropPath

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .height(360.dp)
            .clip(
                RoundedCornerShape(28.dp)
            )
            .background(
                Color(0xFF1A1C22)
            )
    ) {

        if (backdropPath != null) {

            AsyncImage(
                model = BACKDROP_URL + backdropPath,
                contentDescription = movie.displayTitle,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.95f),
                            Color.Black.copy(alpha = 0.65f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(42.dp)
        ) {

            Text(
                text = "TRENDING",
                color = TextSecondary
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = movie.displayTitle,
                color = TextPrimary
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            if (movie.displayDate.isNotEmpty()) {

                Text(
                    text = movie.displayDate,
                    color = TextSecondary
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            TvFocusableButton(
                modifier = Modifier
                    .width(150.dp)
                    .height(52.dp),
                onClick = {
                    // Player dolazi kasnije.
                }
            ) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "▶  Gledaj",
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieSection(
    title: String,
    movies: List<TmdbMovie>
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
    ) {

        Text(
            text = title,
            color = TextPrimary
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            items(
                items = movies,
                key = { movie ->
                    movie.id
                }
            ) { movie ->

                MovieCard(
                    posterPath = movie.posterPath,
                    modifier = Modifier
                        .width(155.dp)
                        .height(230.dp),
                    onClick = {
                        // Detalji filma dolaze u sledećem koraku.
                    }
                )
            }
        }
    }
}
