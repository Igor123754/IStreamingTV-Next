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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.igor.istreamingtv.ui.components.MovieCard
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.theme.Background
import com.igor.istreamingtv.ui.theme.TextPrimary
import com.igor.istreamingtv.ui.theme.TextSecondary

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
                    message = state.error ?: "Greška",
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
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = message,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        TvFocusableButton(
            modifier = Modifier
                .width(150.dp)
                .height(52.dp),
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
    movies: List<com.igor.istreamingtv.data.remote.TmdbMovie>,
    series: List<com.igor.istreamingtv.data.remote.TmdbMovie>
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF151820),
                        Background,
                        Background
                    )
                )
            )
            .padding(
                start = 52.dp,
                end = 52.dp,
                top = 28.dp,
                bottom = 28.dp
            )
    ) {

        TopNavigation()

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        if (movies.isNotEmpty()) {

            Hero(
                movie = movies.first()
            )

        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        if (movies.isNotEmpty()) {

            MovieSection(
                title = "Trending filmovi",
                movies = movies
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        if (series.isNotEmpty()) {

            MovieSection(
                title = "Popularne serije",
                movies = series
            )
        }
    }
}

@Composable
private fun TopNavigation() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "IStreamingTV",
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.width(45.dp)
        )

        TvFocusableButton(
            modifier = Modifier
                .width(90.dp)
                .height(46.dp),
            onClick = {}
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Home",
                    color = TextPrimary
                )
            }
        }

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        TvFocusableButton(
            modifier = Modifier
                .width(100.dp)
                .height(46.dp),
            onClick = {}
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Movies",
                    color = TextPrimary
                )
            }
        }

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        TvFocusableButton(
            modifier = Modifier
                .width(100.dp)
                .height(46.dp),
            onClick = {}
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Series",
                    color = TextPrimary
                )
            }
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        TvFocusableButton(
            modifier = Modifier
                .width(110.dp)
                .height(46.dp),
            onClick = {}
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Search",
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun Hero(
    movie: com.igor.istreamingtv.data.remote.TmdbMovie
) {

    val backdrop =
        movie.backdropPath?.let {
            "https://image.tmdb.org/t/p/w1280$it"
        }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .clip(
                RoundedCornerShape(28.dp)
            )
            .background(
                Color(0xFF20232A)
            )
    ) {

        if (backdrop != null) {

            coil.compose.AsyncImage(
                model = backdrop,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.90f),
                            Color.Black.copy(alpha = 0.55f),
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
                color = TextSecondary,
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = movie.displayTitle,
                color = TextPrimary,
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = movie.displayDate,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            TvFocusableButton(
                modifier = Modifier
                    .width(150.dp)
                    .height(52.dp),
                onClick = {}
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
    movies: List<com.igor.istreamingtv.data.remote.TmdbMovie>
) {

    Column {

        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            items(
                items = movies,
                key = {
                    it.id
                }
            ) { movie ->

                MovieCard(
                    posterPath = movie.posterPath,
                    onClick = {
                        // Detalji filma dolaze u sledećem koraku.
                    },
                    modifier = Modifier
                        .width(150.dp)
                        .height(220.dp)
                )
            }
        }
    }
}
