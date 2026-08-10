package com.igor.istreamingtv.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.igor.istreamingtv.data.remote.*
import com.igor.istreamingtv.ui.components.TvFocusableButton
import kotlinx.coroutines.delay

// Apple TV stil koristi apsolutnu crnu i vrlo čiste bele tonove
private val AppleBackground = Color.Black
private val AppleFocusWhite = Color.White
private val AppleMutedText = Color(0x99FFFFFF)
private val AppleCardShape = RoundedCornerShape(16.dp) // Zaobljenije kartice
private val TopNavShape = RoundedCornerShape(20.dp)

private fun TmdbMovie.displayBackdropUrl(): String = backdropPath ?: posterPath ?: ""

@Composable
fun HomeScreen(
    onMovieClick: (TmdbMovie) -> Unit,
    onMoviesClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleBackground)
    ) {
        when {
            state.isLoading -> ShimmerHomeScreen() // Zadržavamo tvoj odličan shimmer
            state.error != null -> ErrorScreen(state.error ?: "Greška", viewModel::loadContent)
            else -> {
                AppleTvContent(
                    movies = state.movies,
                    series = state.series,
                    onMovieClick = onMovieClick,
                    onMoviesClick = onMoviesClick
                )
            }
        }
    }
}

@Composable
private fun AppleTvContent(
    movies: List<TmdbMovie>,
    series: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit,
    onMoviesClick: () -> Unit
) {
    val heroMovies = remember(movies) { movies.take(5) }
    var heroIndex by remember { mutableIntStateOf(0) }
    val featured = heroMovies.getOrNull(heroIndex)

    LaunchedEffect(heroMovies.size) {
        if (heroMovies.size < 2) return@LaunchedEffect
        while (true) {
            delay(8000)
            heroIndex = (heroIndex + 1) % heroMovies.size
        }
    }

    val verticalState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = verticalState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(650.dp) // Viša hero sekcija u Apple stilu
                ) {
                    if (featured != null) {
                        AppleTvHero(
                            movie = featured,
                            onMovieClick = onMovieClick
                        )
                    }
                }
            }

            if (movies.isNotEmpty()) {
                item {
                    AppleTvRow(
                        title = "Sada u trendu",
                        movies = movies,
                        onMovieClick = onMovieClick
                    )
                }
            }

            if (series.isNotEmpty()) {
                item {
                    AppleTvRow(
                        title = "Popularne serije",
                        movies = series,
                        onMovieClick = onMovieClick
                    )
                }
            }
        }

        // Top Navigation Bar preko celog sadržaja
        AppleTvTopNavigation(
            onMoviesClick = onMoviesClick
        )
    }
}

@Composable
private fun AppleTvHero(
    movie: TmdbMovie,
    onMovieClick: (TmdbMovie) -> Unit
) {
    Crossfade(targetState = movie, animationSpec = tween(1000), label = "hero") { currentMovie ->
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/original" + currentMovie.displayBackdropUrl(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Blagi donji gradijent kako bi tekst bio čitljiv, ali bez crnih traka sa strane
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f), // Zatamnjenje za top meni
                                Color.Transparent,
                                Color.Transparent,
                                AppleBackground
                            ),
                            startY = 0f,
                            endY = 1800f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 60.dp, bottom = 40.dp, end = 60.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tipičan Apple mali naslov žanra/kategorije iznad naslova
                Text(
                    text = "A P P L E   O R I G I N A L",
                    color = AppleMutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Text(
                    text = currentMovie.displayTitle,
                    color = Color.White,
                    fontSize = 64.sp,
                    lineHeight = 70.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )

                Text(
                    text = currentMovie.overview,
                    color = AppleMutedText,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.5f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    AppleTvButton(text = "Gledaj", primary = true) { onMovieClick(currentMovie) }
                    AppleTvButton(text = "Dodaj na listu", primary = false) {}
                }
            }
        }
    }
}

@Composable
private fun AppleTvTopNavigation(
    onMoviesClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppleTopNavItem("Gledaj odmah", true) {}
            AppleTopNavItem("Filmovi", false, onMoviesClick)
            AppleTopNavItem("Serije", false) {}
            AppleTopNavItem("Pretraga", false) {}
        }
    }
}

@Composable
private fun AppleTopNavItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TvFocusableButton(onClick = onClick) { focused ->
        val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(150), label = "")
        Box(
            modifier = Modifier
                .scale(scale)
                .clip(TopNavShape)
                .background(if (focused || selected) AppleFocusWhite else Color.Transparent)
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                color = if (focused || selected) Color.Black else AppleMutedText,
                fontSize = 15.sp,
                fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun AppleTvButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    TvFocusableButton(onClick = onClick) { focused ->
        val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(200), label = "")
        Box(
            modifier = Modifier
                .scale(scale)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        primary && focused -> Color.White
                        primary -> AppleMutedText.copy(alpha = 0.9f)
                        focused -> Color.White.copy(alpha = 0.2f)
                        else -> Color.White.copy(alpha = 0.1f)
                    }
                )
                .padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                text = text,
                color = if (primary && focused) Color.Black else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AppleTvRow(
    title: String,
    movies: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 60.dp, bottom = 16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(horizontal = 60.dp)
        ) {
            items(movies) { movie ->
                AppleTvPosterCard(movie = movie, onClick = { onMovieClick(movie) })
            }
        }
    }
}

@Composable
private fun AppleTvPosterCard(
    movie: TmdbMovie,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(200.dp)
    ) {
        TvFocusableButton(
            onClick = onClick,
            modifier = Modifier
                .width(200.dp)
                .height(300.dp) // Blago šire proporcije
        ) { focused ->
            isFocused = focused
            val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(200), label = "")
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(AppleCardShape)
                    .then(
                        if (focused) Modifier.border(4.dp, Color.White, AppleCardShape)
                        else Modifier
                    )
            ) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w500" + movie.posterPath,
                    contentDescription = movie.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        // Tekst ispod kartice, animira se kada je kartica u fokusu
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = movie.displayTitle,
            color = if (isFocused) Color.White else AppleMutedText,
            fontSize = 15.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
