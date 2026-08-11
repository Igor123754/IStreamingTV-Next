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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.igor.istreamingtv.data.remote.*
import com.igor.istreamingtv.ui.components.TvFocusableButton
import kotlinx.coroutines.delay

private val AppBackground = Color(0xFF020204)
private val SurfaceBackground = Color(0xFF0C0D12)
private val TextSecondary = Color(0xB3FFFFFF)
private val TextSubtle = Color(0x99FFFFFF)
private val CategoryActive = Color.White
private val CategoryInactive = Color(0x99FFFFFF)
private val CardShape = RoundedCornerShape(16.dp)

private fun TmdbMovie.displayBackdropUrl(): String = backdropPath ?: posterPath ?: ""
private fun TmdbMovie.displayYear(): String = displayDate.take(4)

@Composable
fun HomeScreen(
    onMovieClick: (TmdbMovie) -> Unit,
    onAddToLibrary: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        when {
            state.isLoading -> ShimmerHomeScreen()
            state.error != null -> ErrorScreen(message = state.error ?: "Nepoznata greška", onRetry = viewModel::loadContent)
            else -> AppleTvHomeContent(
                movies = state.movies,
                series = state.series,
                onMovieClick = onMovieClick,
                onAddToLibrary = onAddToLibrary
            )
        }
    }
}

@Composable
private fun AppleTvHomeContent(
    movies: List<TmdbMovie>,
    series: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit,
    onAddToLibrary: () -> Unit
) {
    val heroMovies = remember(movies) { movies.take(5) }
    var heroIndex by remember { mutableIntStateOf(0) }
    val featured = heroMovies.getOrNull(heroIndex)

    LaunchedEffect(heroMovies.size) {
        if (heroMovies.size < 2) return@LaunchedEffect
        while (true) {
            delay(9000)
            heroIndex = (heroIndex + 1) % heroMovies.size
        }
    }

    val verticalState = rememberLazyListState()

    LazyColumn(
        state = verticalState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 44.dp, vertical = 30.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Početna", color = CategoryActive, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Serije", color = CategoryInactive, fontSize = 16.sp)
                    Text(text = "Filmovi", color = CategoryInactive, fontSize = 16.sp)
                    Text(text = "Biblioteka", color = CategoryInactive, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(26.dp))

                Text(text = "Istaknuto", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Nove priče i najbolje preporuke, baš kao na Apple TV+.",
                    color = TextSubtle,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(0.55f)
                )
            }
        }

        item {
            if (featured != null) {
                AppleTvHero(
                    movie = featured,
                    currentIndex = heroIndex,
                    totalCount = heroMovies.size,
                    onMovieClick = onMovieClick,
                    onAddToLibrary = onAddToLibrary
                )
            }
        }

        if (movies.isNotEmpty()) {
            item {
                SectionRow(title = "Top preporuke") {
                    ContentRow(title = "Najbolje ocenjeni", movies = movies, onMovieClick = onMovieClick)
                }
            }
        }

        if (series.isNotEmpty()) {
            item {
                SectionRow(title = "Popularno sada") {
                    ContentRow(title = "Serije koje vrede gledanja", movies = series, onMovieClick = onMovieClick)
                }
            }
        }

        if (movies.isNotEmpty() || series.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AppleTvHero(
    movie: TmdbMovie,
    currentIndex: Int,
    totalCount: Int,
    onMovieClick: (TmdbMovie) -> Unit,
    onAddToLibrary: () -> Unit
) {
    Crossfade(targetState = movie, animationSpec = tween(1000), label = "hero") { currentMovie ->
        Box(modifier = Modifier.fillMaxWidth().height(620.dp)) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/original" + currentMovie.displayBackdropUrl(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f), AppBackground),
                            startY = 260f,
                            endY = 1100f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.64f), Color.Transparent),
                            endX = 1200f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 44.dp, end = 48.dp, bottom = 38.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = currentMovie.displayTitle,
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.63f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = currentMovie.displayYear(), color = TextSecondary, fontSize = 14.sp)
                    Text(text = "4K", color = TextSecondary, fontSize = 14.sp)
                    Text(text = "Drama", color = TextSecondary, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = currentMovie.overview,
                    color = TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.62f)
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    TvFocusableButton(onClick = { onMovieClick(currentMovie) }) { focused ->
                        val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(160), label = "")
                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(horizontal = 40.dp, vertical = 16.dp)
                        ) {
                            Text(text = "Gledaj", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    TvFocusableButton(onClick = onAddToLibrary) { focused ->
                        val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(160), label = "")
                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                                .border(1.dp, Color.White.copy(alpha = 0.26f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 34.dp, vertical = 16.dp)
                        ) {
                            Text(text = "Dodaj u biblioteku", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalCount) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentIndex) 9.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (index == currentIndex) Color.White else Color.White.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionRow(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 44.dp, bottom = 14.dp))
        content()
    }
}

@Composable
private fun ContentRow(
    title: String,
    movies: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 44.dp, bottom = 16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = 44.dp)
        ) {
            items(movies, key = { it.id }) { movie ->
                PosterCard(movie = movie, onClick = { onMovieClick(movie) })
            }
        }
    }
}

@Composable
private fun PosterCard(
    movie: TmdbMovie,
    onClick: () -> Unit
) {
    TvFocusableButton(
        onClick = onClick,
        modifier = Modifier.width(180.dp).height(260.dp)
    ) { focused ->
        val scale by animateFloatAsState(if (focused) 1.07f else 1f, tween(220), label = "")

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(CardShape)
                .background(SurfaceBackground)
                .border(if (focused) 2.dp else 0.dp, Color.White.copy(alpha = if (focused) 0.22f else 0f), CardShape)
        ) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w500" + movie.posterPath,
                contentDescription = movie.displayTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ShimmerHomeScreen() {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val progress by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = ""
    )

    fun shimmerBrush(): Brush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF111113), Color(0xFF29292D), Color(0xFF111113)),
        startX = -700f + progress * 2400f, endX = progress * 2400f
    )

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Box(modifier = Modifier.fillMaxWidth().height(620.dp).background(shimmerBrush()))
        Column(modifier = Modifier.fillMaxSize().padding(start = 44.dp, top = 660.dp)) {
            Box(modifier = Modifier.width(260.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).background(shimmerBrush()))
            Spacer(modifier = Modifier.height(20.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                items(5) {
                    Box(modifier = Modifier.width(180.dp).height(260.dp).clip(CardShape).background(shimmerBrush()))
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "⚠", color = Color.White, fontSize = 46.sp)
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = message, color = TextSecondary, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(28.dp))
        TvFocusableButton(onClick = onRetry) { focused ->
            val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(180), label = "")
            Box(
                modifier = Modifier.scale(scale).clip(RoundedCornerShape(25.dp)).background(Color.White).padding(horizontal = 30.dp, vertical = 13.dp)
            ) {
                Text(text = "Pokušaj ponovo", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
