package com.igor.istreamingtv.ui.movies

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.igor.istreamingtv.BuildConfig
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.displayTitle
import com.igor.istreamingtv.data.remote.posterPath
import com.igor.istreamingtv.data.repository.ContentRepository
import com.igor.istreamingtv.ui.components.TvFocusableButton
import kotlinx.coroutines.launch

private val MoviesBackground = Color(0xFF020204)
private val SurfaceBackground = Color(0xFF0C0D12)
private val CardShape = RoundedCornerShape(12.dp)

@Composable
fun MoviesScreen(
    onMovieClick: (TmdbMovie) -> Unit,
    onBack: () -> Unit
) {
    val repository = remember { ContentRepository(BuildConfig.TMDB_API_KEY) }
    var movies by remember { mutableStateOf<List<TmdbMovie>>(emptyList()) }
    var series by remember { mutableStateOf<List<TmdbMovie>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        launch {
            try {
                movies = repository.getPopularMovies()
                series = repository.getPopularSeries()
            } catch (_: Exception) {
            }
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoviesBackground)
    ) {
        Text(
            text = "Filmovi i serije",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 48.dp, top = 32.dp, bottom = 8.dp)
        )

        if (loading) {
            Box(modifier = Modifier.fillMaxSize().background(MoviesBackground))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (movies.isNotEmpty()) {
                    item(key = "movies-header") {
                        RowHeader("Popularni filmovi")
                    }
                    item(key = "movies-row") {
                        MoviesRow(movies = movies, onMovieClick = onMovieClick)
                    }
                }
                if (series.isNotEmpty()) {
                    item(key = "series-header") {
                        RowHeader("Popularne serije")
                    }
                    item(key = "series-row") {
                        MoviesRow(movies = series, onMovieClick = onMovieClick)
                    }
                }
                item(key = "bottom-spacer") {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun RowHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 48.dp, top = 24.dp, bottom = 16.dp)
    )
}

@Composable
private fun MoviesRow(
    movies: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val rowAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(600), label = "row-alpha")
    val rowOffsetY by animateFloatAsState(if (entered) 0f else 80f, tween(600), label = "row-offset")

    LazyRow(
        modifier = Modifier
            .graphicsLayer {
                alpha = rowAlpha
                translationY = rowOffsetY
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 48.dp, end = 48.dp)
    ) {
        items(movies, key = { it.id }) { movie ->
            MoviePosterCard(movie = movie, onClick = { onMovieClick(movie) })
        }
    }
}

@Composable
private fun MoviePosterCard(
    movie: TmdbMovie,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.width(150.dp)) {
        TvFocusableButton(
            onClick = onClick,
            modifier = Modifier
                .width(150.dp)
                .height(225.dp)
        ) { focused ->
            val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(220), label = "")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(CardShape)
                    .background(SurfaceBackground)
                    .then(
                        if (focused) Modifier.border(3.dp, Color.White, CardShape)
                        else Modifier
                    )
            ) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w185" + movie.posterPath,
                    contentDescription = movie.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = movie.displayTitle,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
