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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
private val CardShape = RoundedCornerShape(12.dp)

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
                catalogs = state.catalogs,
                heroExtras = state.heroExtras,
                initialScroll = viewModel.getHomeVerticalPosition(),
                onSaveScroll = viewModel::saveHomeVerticalPosition,
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
    catalogs: List<Catalog>,
    heroExtras: Map<Int, HeroExtras>,
    initialScroll: ScrollPosition,
    onSaveScroll: (Int, Int) -> Unit,
    onMovieClick: (TmdbMovie) -> Unit,
    onLoadHeroExtras: (TmdbMovie, Boolean) -> Unit
) {
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

    LaunchedEffect(featured?.movie?.id) {
        featured?.let { onLoadHeroExtras(it.movie, it.isTv) }
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialScroll.index,
        initialFirstVisibleItemScrollOffset = initialScroll.offset
    )

    val openMovie: (TmdbMovie) -> Unit = { movie ->
        onSaveScroll(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset
        )
        onMovieClick(movie)
    }

    // ✅ FIX: eksplicitna visina ekrana (rešava "pola učitan" hero pri skrolu nazad)
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

    // Paralaksa hero-a
    val firstItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
    val heroSize = firstItem?.size?.coerceAtLeast(1) ?: 1
    val scrollProgress = (-(firstItem?.offset ?: 0).toFloat() / heroSize).coerceIn(0f, 1f)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeightDp)
            ) {
                if (featured != null) {
                    AppleTvHero(
                        item = featured,
                        heroExtras = heroExtras,
                        currentIndex = heroIndex,
                        totalCount = heroItems.size,
                        onMovieClick = openMovie,
                        scrollProgress = scrollProgress
                    )
                }
            }
        }

        items(catalogs, key = { it.id }) { catalog ->
            CatalogRowSection(
                catalog = catalog,
                onMovieClick = openMovie
            )
        }

        item(key = "bottom-spacer") {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

/**
 * Hero banner: fanart 100% + clearlogo + žanr/uzrast + opis.
 * ✅ "Gledaj" vodi na STRANICU SA DETALJIMA (onMovieClick).
 */
@Composable
private fun AppleTvHero(
    item: HeroItem,
    heroExtras: Map<Int, HeroExtras>,
    currentIndex: Int,
    totalCount: Int,
    onMovieClick: (TmdbMovie) -> Unit,
    scrollProgress: Float
) {
    Crossfade(targetState = item, animationSpec = tween(1000), label = "hero") { currentItem ->
        val movie = currentItem.movie
        val extras = heroExtras[movie.id]

        Box(modifier = Modifier.fillMaxSize()) {

            // FANART 100% EKRANA + blagi zoom pri skrolu
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w1280" + movie.displayBackdropUrl(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1f + scrollProgress * 0.08f
                        scaleY = 1f + scrollProgress * 0.08f
                    },
                contentScale = ContentScale.Crop
            )

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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = (1f - scrollProgress * 1.2f).coerceIn(0f, 1f)
                        translationY = scrollProgress * 240f
                    }
            ) {
                // "Home" pilula gore levo
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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 48.dp, end = 48.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    val logoUrl = extras?.clearLogoUrl
                    if (logoUrl != null) {
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

                    // ✅ "Gledaj" → STRANICA SA DETALJIMA
                    TvFocusableButton(onClick = { onMovieClick(movie) }) { focused ->
                        val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(160), label = "")
                        Row(
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE9E9F2))
                                .padding(horizontal = 36.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Gledaj", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Tačkice dole na sredini
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
}

/**
 * Red kataloga sa Apple TV+ "enter" animacijom i poster karticama.
 * ✅ Posteri w185 (manji = brže dekodiranje na slabim TV-ovima).
 */
@Composable
private fun CatalogRowSection(
    catalog: Catalog,
    onMovieClick: (TmdbMovie) -> Unit
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val rowAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(600), label = "row-alpha")
    val rowOffsetY by animateFloatAsState(if (entered) 0f else 80f, tween(600), label = "row-offset")

    Column(
        modifier = Modifier
            .graphicsLayer {
                alpha = rowAlpha
                translationY = rowOffsetY
            }
            .padding(top = 40.dp)
    ) {
        Text(
            text = catalog.title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 48.dp, bottom = 16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp)
        ) {
            items(catalog.items, key = { it.id }) { movie ->
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
                    // ✅ w185 umesto w342 — ~4x manje memorije, brže na slabim uređajima
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
            letterSpacing = 0.15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
