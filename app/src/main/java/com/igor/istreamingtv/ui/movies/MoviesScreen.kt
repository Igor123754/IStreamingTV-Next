package com.igor.istreamingtv.ui.movies

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.ui.components.MovieCard
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.home.HomeViewModel
import com.igor.istreamingtv.ui.theme.Background
import com.igor.istreamingtv.ui.theme.TextPrimary
import com.igor.istreamingtv.ui.theme.TextSecondary

@Composable
fun MoviesScreen(
    onMovieClick: (TmdbMovie) -> Unit,
    onBack: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {

    BackHandler {
        onBack()
    }

    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        when {

            state.isLoading -> {

                LoadingMovies()
            }

            state.error != null -> {

                ErrorMovies(
                    message = state.error ?: "Nepoznata greška",
                    onRetry = viewModel::loadContent,
                    onBack = onBack
                )
            }

            else -> {

                MoviesContent(
                    movies = state.movies,
                    onMovieClick = onMovieClick,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun LoadingMovies() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator(
                color = TextPrimary
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = "Učitavanje filmova...",
                color = TextSecondary,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ErrorMovies(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Nije moguće učitati filmove",
                color = TextPrimary,
                fontSize = 24.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = message,
                color = TextSecondary,
                fontSize = 15.sp
            )

            Spacer(
                modifier = Modifier.height(26.dp)
            )

            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                TvFocusableButton(
                    modifier = Modifier
                        .width(170.dp)
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

                TvFocusableButton(
                    modifier = Modifier
                        .width(130.dp)
                        .height(52.dp),
                    onClick = onBack
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "Nazad",
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoviesContent(
    movies: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit,
    onBack: () -> Unit
) {

    val gridState = rememberLazyGridState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        MoviesHeader(
            movieCount = movies.size,
            onBack = onBack
        )

        if (movies.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Nema dostupnih filmova.",
                    color = TextSecondary,
                    fontSize = 18.sp
                )
            }

        } else {

            LazyVerticalGrid(
                columns = GridCells.Adaptive(
                    minSize = 160.dp
                ),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 48.dp,
                    end = 48.dp,
                    top = 10.dp,
                    bottom = 80.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {

                items(
                    items = movies,
                    key = { movie ->
                        movie.id
                    }
                ) { movie ->

                    MovieGridItem(
                        movie = movie,
                        onClick = {
                            onMovieClick(movie)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MoviesHeader(
    movieCount: Int,
    onBack: () -> Unit
) {

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 48.dp,
                vertical = 28.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        TvFocusableButton(
            modifier = Modifier
                .width(110.dp)
                .height(48.dp),
            onClick = onBack
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "‹  Nazad",
                    color = TextPrimary,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.width(26.dp)
        )

        Column {

            Text(
                text = "Filmovi",
                color = TextPrimary,
                fontSize = 30.sp
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = "$movieCount dostupnih filmova",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun MovieGridItem(
    movie: TmdbMovie,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .width(160.dp)
    ) {

        Box(
            modifier = Modifier
                .width(160.dp)
                .height(240.dp)
                .background(
                    Color(0xFF15171D),
                    RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp)
                )
    ) {

            MovieCard(
                posterPath = movie.posterPath,
                modifier = Modifier.fillMaxSize(),
                onClick = onClick
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = movie.displayTitle.orEmpty(),
            color = TextPrimary,
            fontSize = 15.sp,
            maxLines = 2
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = movie.displayDate.orEmpty(),
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}
