package com.igor.istreamingtv.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
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
private val TextSecondary = Color(0xB3FFFFFF)

// Mapiranje TMDB genre_ids u nazive (za "Comedy" stil prikaz kao na slici)
private val genreNames = mapOf(
    28 to "Akcija", 12 to "Avantura", 16 to "Animacija", 35 to "Komedija",
    80 to "Krimi", 99 to "Dokumentarac", 18 to "Drama", 10751 to "Porodica",
    14 to "Fantazija", 36 to "Istorija", 27 to "Horor", 10402 to "Muzika",
    9648 to "Misterija", 10749 to "Romansa", 878 to "SF", 53 to "Triler",
    37 to "Vestern", 10759 to "Akcija i avantura", 10765 to "SF i fantazija",
    10762 to "Dečiji", 10763 to "Vesti", 10764 to "Rijaliti", 10766 to "Sapunica",
    10767 to "Talk show", 10768 to "Rat i politika"
)

private fun TmdbMovie.displayBackdropUrl(): String = backdropPath ?: posterPath ?: ""

private fun TmdbMovie.displayGenre(): String =
    genre_ids.firstNotNullOfOrNull { genreNames[it] } ?: "Film"

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
            state.error != null -> ErrorScreen(
                message = state.error ?: "Nepoznata greška",
                onRetry = viewModel::loadContent
            )
            else -> AppleTvHomeContent(
                movies = state.movies,
                onMovieClick = onMovieClick
            )
        }
    }
}

/**
 * Početna u Apple TV+ stilu:
 * fanart preko CELOG ekrana (100%), bez "Up Next" redova.
 */
@Composable
private fun AppleTvHomeContent(
    movies: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit
) {
    val heroMovies = remember(movies) { movies.take(8) }
    var heroIndex by remember { mutableIntStateOf(0) }
    val featured = heroMovies.getOrNull(heroIndex)

    // Automatska rotacija hero sadržaja na 9 sekundi
    LaunchedEffect(heroMovies.size) {
        if (heroMovies.size < 2) return@LaunchedEffect
        while (true) {
            delay(9000)
            heroIndex = (heroIndex + 1) % heroMovies.size
        }
    }

    if (featured != null) {
        AppleTvHero(
            movie = featured,
            currentIndex = heroIndex,
            totalCount = heroMovies.size,
            onMovieClick = onMovieClick
        )
    }
}

@Composable
private fun AppleTvHero(
    movie: TmdbMovie,
    currentIndex: Int,
    totalCount: Int,
    onMovieClick: (TmdbMovie) -> Unit
) {
    Crossfade(targetState = movie, animationSpec = tween(1000), label = "hero") { currentMovie ->
        Box(modifier = Modifier.fillMaxSize()) {

            // 1) FANART PREKO 100% EKRANA
            AsyncImage(
                model = "https://image.tmdb.org/t/p/original" + currentMovie.displayBackdropUrl(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 2) Blage senke samo zbog čitljivosti teksta (levo + dole)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                            endX = 1100f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                            startY = 700f
                        )
                    )
            )

            // 3) "Home" pilula gore levo (kao na slici)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 48.dp, top = 40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Početna",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 4) Naslov + žanr + bedž + opis + dugme (levo, vertikalno centrirano)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 48.dp, end = 48.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentMovie.displayTitle,
                    color = Color.White,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Žanr + bedž sa ocenom (kao "Comedy  [TV-MA]" na slici)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = currentMovie.displayGenre(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "★ %.1f".format(currentMovie.vote_average),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = currentMovie.overview,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.45f)
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Jedno belo dugme kao "Go to Show" na slici
                TvFocusableButton(onClick = { onMovieClick(currentMovie) }) { focused ->
                    val scale by animateFloatAsState(
                        if (focused) 1.05f else 1f, tween(160), label = ""
                    )
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE9E9F2))
                            .padding(horizontal = 36.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Pogledaj",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 5) Carousel tačkice — dole na sredini (aktivna = izdužena, kao na slici)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalCount) { index ->
                    Box(
                        modifier = Modifier
                            .then(
                                if (index == currentIndex) Modifier.size(width = 18.dp, height = 7.dp)
                                else Modifier.size(7.dp)
                            )
                            .clip(CircleShape)
                            .background(
                                if (index == currentIndex) Color.White
                                else Color.White.copy(alpha = 0.35f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerHomeScreen() {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val progress by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = ""
    )

    fun shimmerBrush(): Brush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF111113), Color(0xFF29292D), Color(0xFF111113)),
        startX = -700f + progress * 2400f, endX = progress * 2400f
    )

    Box(modifier = Modifier.fillMaxSize().background(shimmerBrush()))
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
                modifier = Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color.White)
                    .padding(horizontal = 30.dp, vertical = 13.dp)
            ) {
                Text(text = "Pokušaj ponovo", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
