package com.igor.istreamingtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.ui.components.MovieCard
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.theme.Background
import com.igor.istreamingtv.ui.theme.TextPrimary
import com.igor.istreamingtv.ui.theme.TextSecondary

private const val IMAGE_URL =
    "https://image.tmdb.org/t/p/"

private const val BACKDROP_SIZE =
    "w1280"

@Composable
fun HomeScreen(
    onMovieClick: (TmdbMovie) -> Unit,
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
                    series = state.series,
                    onMovieClick = onMovieClick
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

        Text(
            text = "Učitavanje...",
            color = TextPrimary,
            fontSize = 22.sp
        )
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Nije moguće učitati katalog",
                color = TextPrimary,
                fontSize = 24.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = message,
                color = TextSecondary,
                fontSize = 16.sp
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            TvFocusableButton(
                modifier = Modifier
                    .width(180.dp)
                    .height(52.dp),
                onClick = onRetry
            ) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "Pokušaj ponovo",
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    movies: List<TmdbMovie>,
    series: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit
) {

    val scrollState = rememberLazyListState()

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(34.dp)
    ) {

        item {
            TopBar()
        }

        if (movies.isNotEmpty()) {

            item {
                HeroSection(
                    movie = movies.first(),
                    onMovieClick = onMovieClick
                )
            }
        }

        if (movies.isNotEmpty()) {

            item {
                MovieRow(
                    title = "Trending",
                    movies = movies,
                    onMovieClick = onMovieClick
                )
            }
        }

        if (series.isNotEmpty()) {

            item {
                MovieRow(
                    title = "Popularne serije",
                    movies = series,
                    onMovieClick = onMovieClick
                )
            }
        }

        if (movies.size > 2) {

            item {
                MovieRow(
                    title = "Filmovi",
                    movies = movies.drop(2),
                    onMovieClick = onMovieClick
                )
            }
        }

        item {
            Spacer(
                modifier = Modifier.height(80.dp)
            )
        }
    }
}

@Composable
private fun TopBar() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 52.dp,
                vertical = 24.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "IStreamingTV",
            color = TextPrimary,
            fontSize = 25.sp
        )

        Spacer(
            modifier = Modifier.width(45.dp)
        )

        NavigationItem(
            text = "Home",
            selected = true
        )

        NavigationItem(
            text = "Movies"
        )

        NavigationItem(
            text = "Series"
        )

        NavigationItem(
            text = "Live TV"
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )

        NavigationItem(
            text = "⌕  Search"
        )
    }
}

@Composable
private fun NavigationItem(
    text: String,
    selected: Boolean = false
) {

    TvFocusableButton(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(46.dp)
            .width(
                if (text == "⌕  Search") 120.dp else 92.dp
            ),
        onClick = {}
    ) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = text,
                color = if (selected) {
                    TextPrimary
                } else {
                    TextSecondary
                },
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun HeroSection(
    movie: TmdbMovie,
    onMovieClick: (TmdbMovie) -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .height(390.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(30.dp)
            )
            .background(
                Color(0xFF15171D),
                RoundedCornerShape(30.dp)
            )
    ) {

        movie.backdropPath?.let { path ->

            AsyncImage(
                model = IMAGE_URL + BACKDROP_SIZE + path,
                contentDescription = movie.displayTitle,
                modifier = Modifier.fillMaxSize()
            )
        }

        /*
         * Dark overlay preko fanart slike.
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.96f),
                            Color.Black.copy(alpha = 0.76f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        /*
         * Donji gradient.
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f),
                            Background.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(520.dp)
                .padding(start = 48.dp)
        ) {

            GlassLabel(
                text = "TRENDING"
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = movie.displayTitle.orEmpty(),
                color = TextPrimary,
                fontSize = 36.sp,
                lineHeight = 42.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = movie.displayDate.orEmpty(),
                color = TextSecondary,
                fontSize = 15.sp
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Text(
                text = movie.overview.orEmpty(),
                color = TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 3
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                TvFocusableButton(
                    modifier = Modifier
                        .width(145.dp)
                        .height(52.dp),
                    onClick = {
                        onMovieClick(movie)
                    }
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "▶  Gledaj",
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                    }
                }

                TvFocusableButton(
                    modifier = Modifier
                        .width(145.dp)
                        .height(52.dp),
                    onClick = {
                        onMovieClick(movie)
                    }
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "+  Detalji",
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassLabel(
    text: String
) {

    Box(
        modifier = Modifier
            .background(
                Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(50.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50.dp)
            )
            .padding(
                horizontal = 15.dp,
                vertical = 7.dp
            )
    ) {

        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MovieRow(
    title: String,
    movies: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 23.sp
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "Prikaži sve  ›",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }

        Spacer(
            modifier = Modifier.height(18.dp)
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

                Box(
                    modifier = Modifier
                        .width(155.dp)
                        .height(232.dp)
                ) {

                    MovieCard(
                        posterPath = movie.posterPath,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusable(),
                        onClick = {
                            onMovieClick(movie)
                        }
                    )
                }
            }
        }
    }
}
