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
import java.util.Locale

// Boje prilagođene čistom dizajnu sa slike
private val AppBackground = Color(0xFF0F0F0F)
private val CardShape = RoundedCornerShape(12.dp)
private val TextSecondary = Color(0xB3FFFFFF)

private fun TmdbMovie.displayBackdropUrl(): String = backdropPath ?: posterPath ?: ""
private fun TmdbMovie.displayYear(): String = displayDate.take(4)

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
            .background(AppBackground)
    ) {
        when {
            state.isLoading -> {
                ShimmerHomeScreen()
            }
            state.error != null -> {
                ErrorScreen(message = state.error ?: "Nepoznata greška", onRetry = viewModel::loadContent)
            }
            else -> {
                CleanTvContent(
                    movies = state.movies,
                    series = state.series,
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}

@Composable
private fun CleanTvContent(
    movies: List<TmdbMovie>,
    series: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit
) {
    val heroMovies = remember(movies) { movies.take(5) }
    var heroIndex by remember { mutableIntStateOf(0) }
    val featured = heroMovies.getOrNull(heroIndex)

    LaunchedEffect(heroMovies.size) {
        if (heroMovies.size < 2) return@LaunchedEffect
        while (true) {
            delay(8500)
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
            
            // 1. Hero Sekcija preko pola ekrana
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp) 
                ) {
                    if (featured != null) {
                        CleanHero(
                            movie = featured,
                            currentIndex = heroIndex,
                            totalCount = heroMovies.size,
                            onMovieClick = onMovieClick
                        )
                    }
                }
            }

            // 2. Klasični redovi (bez Up Next)
            if (movies.isNotEmpty()) {
                item {
                    ContentRow(
                        title = "Sada u trendu",
                        movies = movies,
                        onMovieClick = onMovieClick
                    )
                }
            }

            if (series.isNotEmpty()) {
                item {
                    ContentRow(
                        title = "Popularne serije",
                        movies = series,
                        onMovieClick = onMovieClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanHero(
    movie: TmdbMovie,
    currentIndex: Int,
    totalCount: Int,
    onMovieClick: (TmdbMovie) -> Unit
) {
    Crossfade(targetState = movie, animationSpec = tween(1000), label = "hero") { currentMovie ->
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Velika pozadinska slika
            AsyncImage(
                model = "https://image.tmdb.org/t/p/original" + currentMovie.displayBackdropUrl(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradijent ispod slike kako bi se spojilo sa pozadinom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                AppBackground
                            ),
                            startY = 400f,
                            endY = 1600f
                        )
                    )
            )
            
            // Gradijent sa leve strane za čitljivost teksta
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            ),
                            endX = 1200f
                        )
                    )
            )

            // "Home" dugme u gornjem levom uglu (kao na slici)
            Row(
                modifier = Modifier
                    .padding(top = 32.dp, start = 40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🏠", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                Text(text = "Početna", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            // Sadržaj dole levo
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 40.dp, bottom = 40.dp, end = 60.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                
                // Naslov filma
                Text(
                    text = currentMovie.displayTitle,
                    color = Color.White,
                    fontSize = 52.sp,
                    lineHeight = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )

                // Podaci ispod naslova (Žanr, ocena, godina) - nalik na "Comedy TV-MA" sa slike
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Film", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    
                    if (currentMovie.displayYear().isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, TextSecondary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = currentMovie.displayYear(), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Opis
                Text(
                    text = currentMovie.overview,
                    color = TextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.5f).padding(top = 4.dp, bottom = 8.dp)
                )

                // Dugme za gledanje ("Go to Show" stil)
                TvFocusableButton(onClick = { onMovieClick(currentMovie) }) { focused ->
                    val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(150), label = "")
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(horizontal = 42.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Gledaj",
                            color = Color.Black,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Indikatori (tačkice) na sredini dole
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(totalCount) { index ->
                    val isSelected = index == currentIndex
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentRow(
    title: String,
    movies: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 40.dp, bottom = 16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = 40.dp)
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
        modifier = Modifier.width(160.dp).height(240.dp)
    ) { focused ->
        val scale by animateFloatAsState(if (focused) 1.07f else 1f, tween(200), label = "")
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(CardShape)
                .background(Color(0xFF1E1E1E))
                .then(
                    if (focused) Modifier.border(3.dp, Color.White, CardShape)
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
}

// OVE FUNKCIJE SU BAZNE ZA TVOJ KOD KAKO NE BI BILO GREŠKE
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
        Box(modifier = Modifier.fillMaxWidth().height(600.dp).background(shimmerBrush()))
        Column(modifier = Modifier.fillMaxSize().padding(start = 40.dp, top = 620.dp)) {
            Box(modifier = Modifier.width(200.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).background(shimmerBrush()))
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                repeat(5) {
                    Box(modifier = Modifier.width(160.dp).height(240.dp).clip(CardShape).background(shimmerBrush()))
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
                modifier = Modifier.scale(scale).clip(RoundedCornerShape(25.dp))
                    .background(Color.White).padding(horizontal = 30.dp, vertical = 13.dp)
            ) {
                Text(text = "Pokušaj ponovo", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
