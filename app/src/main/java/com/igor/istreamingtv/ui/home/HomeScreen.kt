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

// Null-safe žanr (serije/filmovi bez genre_ids ne krešuju)
private fun TmdbMovie.displayGenre(): String =
    genre_ids?.firstNotNullOfOrNull { genreNames[it] } ?: "Film"

private data class HeroItem(val movie: TmdbMovie, val isTv: Boolean)

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
                series = state.series,
                heroExtras = state.heroExtras,
                onMovieClick = onMovieClick,
                onLoadHeroExtras = viewModel::loadHeroExtras
            )
        }
    }
}

@Composable
private fun AppleTvHomeContent(
    movies: List<TmdbMovie>,
    series: List<TmdbMovie>,
    heroExtras: Map<Int, HeroExtras>,
    onMovieClick: (TmdbMovie) -> Unit,
    onLoadHeroExtras: (TmdbMovie, Boolean) -> Unit
) {
    // Hero rotacija: filmovi + serije
    val heroItems = remember(movies, series) {
        movies.take(5).map { HeroItem(it, isTv = false) } +
            series.take(5).map { HeroItem(it, isTv = true) }
    }
    var heroIndex by remember { mutableIntStateOf(0) }
    val featured = heroItems.getOrNull(heroIndex)

    LaunchedEffect(heroItems.size) {
        if (heroItems.size < 2) return@LaunchedEffect
        while (true) {
            delay(9000)
            heroIndex = (heroIndex + 1) % heroItems.size
        }
    }

    // Povuci clearlogo / srpski opis / uzrast za trenutni hero
    LaunchedEffect(featured?.movie?.id) {
        featured?.let { onLoadHeroExtras(it.movie, it.isTv) }
    }

    if (featured != null) {
        AppleTvHero(
            item = featured,
            heroExtras = heroExtras,
            currentIndex = heroIndex,
            totalCount = heroItems.size,
            onMovieClick = onMovieClick
        )
    }
}

@Composable
private fun AppleTvHero(
    item: HeroItem,
    heroExtras: Map<Int, HeroExtras>,
    currentIndex: Int,
    totalCount: Int,
    onMovieClick: (TmdbMovie) -> Unit
) {
    Crossfade(targetState = item, animationSpec = tween(1000), label = "hero") { currentItem ->
        val movie = currentItem.movie
        val extras = heroExtras[movie.id]

        Box(modifier = Modifier.fillMaxSize()) {

            // 1) FANART 100% EKRANA (w1280 = HD kvalitet, mala memorija)
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w1280" + movie.displayBackdropUrl(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 2) Diskretne senke za čitljivost
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

            // 3) "Home" pilula gore levo
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
                Text("Početna", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            // 4) CLEARLOGO + žanr + uzrast + opis + dugme
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 48.dp, end = 48.dp),
                verticalArrangement = Arrangement.Center
            ) {
                val logoUrl = extras?.clearLogoUrl
                if (logoUrl != null) {
                    // Clearlogo (srpski ako postoji, inače originalni) — kompaktna veličina
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = movie.displayTitle,
                        modifier = Modifier
                            .width(300.dp)
                            .height(100.dp),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart
                    )
                } else {
                    // Fallback: običan naslov dok se logo ne učita / ako ne postoji
                    Text(
                        text = movie.displayTitle,
                        color = Color.White,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Žanr + uzrastna preporuka (kao "Comedy [TV-MA]" na slici)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = movie.displayGenre(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    val cert = extras?.certification
                    if (!cert.isNullOrBlank()) {
                        Text(
                            text = cert,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Opis: srpski ako postoji, inače originalni (null-safe)
                Text(
                    text = extras?.overview ?: movie.displayOverview,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.45f)
                )

                Spacer(modifier = Modifier.height(26.dp))

                TvFocusableButton(onClick = { onMovieClick(movie) }) { focused ->
                    val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(160), label = "")
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE9E9F2))
                            .padding(horizontal = 36.dp, vertical = 14.dp)
                    ) {
                        Text("Pogledaj", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 5) Tačkice dole na sredini
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
                Text("Pokušaj ponovo", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
