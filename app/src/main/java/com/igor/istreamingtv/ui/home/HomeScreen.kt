package com.igor.istreamingtv.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.backdropPath
import com.igor.istreamingtv.data.remote.displayDate
import com.igor.istreamingtv.data.remote.displayTitle
import com.igor.istreamingtv.data.remote.posterPath
import com.igor.istreamingtv.ui.components.MovieCard
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.theme.Background
import com.igor.istreamingtv.ui.theme.TextPrimary
import com.igor.istreamingtv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private const val IMAGE_URL = "https://image.tmdb.org/t/p/"
private const val BACKDROP_SIZE = "w1280"
private const val POSTER_SIZE = "w500"

// ============================================================
// GLAVNI HOME SCREEN
// ============================================================

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
            .background(Background)
    ) {
        when {
            state.isLoading -> {
                ShimmerHomeScreen()
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
                    onMovieClick = onMovieClick,
                    onMoviesClick = onMoviesClick,
                    viewModel = viewModel
                )
            }
        }
    }
}

// ============================================================
// SHIMMER LOADING (premium efekat)
// ============================================================

@Composable
private fun ShimmerHomeScreen() {
    val shimmerColors = listOf(
        Color(0xFF1A1A1A),
        Color(0xFF2A2A2A),
        Color(0xFF1A1A1A)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value),
        tileMode = TileMode.Clamp
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp)
    ) {
        // TopBar shimmer
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brush)
        )

        // Hero shimmer
        Spacer(modifier = Modifier.height(34.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(brush)
        )

        // Row 1 shimmer
        Spacer(modifier = Modifier.height(34.dp))
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(270.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(brush)
                )
            }
        }
    }
}

// ============================================================
// ERROR SCREEN
// ============================================================

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Nije moguće učitati katalog",
                color = TextPrimary,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
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

// ============================================================
// HOME CONTENT
// ============================================================

@Composable
private fun HomeContent(
    movies: List<TmdbMovie>,
    series: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit,
    onMoviesClick: () -> Unit,
    viewModel: HomeViewModel
) {
    val verticalState = rememberLazyListState()

    LaunchedEffect(movies.size, series.size) {
        if (movies.isNotEmpty() || series.isNotEmpty()) {
            val saved = viewModel.getHomeVerticalPosition()
            verticalState.scrollToItem(
                index = saved.index.coerceAtLeast(0),
                scrollOffset = saved.offset
            )
        }
    }

    LaunchedEffect(verticalState) {
        snapshotFlow {
            Pair(
                verticalState.firstVisibleItemIndex,
                verticalState.firstVisibleItemScrollOffset
            )
        }.collect { position ->
            viewModel.saveHomeVerticalPosition(
                index = position.first,
                offset = position.second
            )
        }
    }

    LazyColumn(
        state = verticalState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(40.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            TopBar(onMoviesClick = onMoviesClick)
        }

        if (movies.isNotEmpty()) {
            item {
                HeroCarousel(
                    movies = movies.take(5),
                    onMovieClick = onMovieClick
                )
            }
        }

        if (movies.isNotEmpty()) {
            item {
                MovieRowSnap(
                    catalogId = "trending_movies",
                    title = "Sada u trendu",
                    movies = movies,
                    onMovieClick = onMovieClick,
                    viewModel = viewModel
                )
            }
        }

        if (series.isNotEmpty()) {
            item {
                MovieRowSnap(
                    catalogId = "trending_series",
                    title = "Popularne serije",
                    movies = series,
                    onMovieClick = onMovieClick,
                    viewModel = viewModel
                )
            }
        }

        if (movies.size > 2) {
            item {
                MovieRowSnap(
                    catalogId = "movies",
                    title = "Preporučeni filmovi",
                    movies = movies.drop(2),
                    onMovieClick = onMovieClick,
                    viewModel = viewModel
                )
            }
        }
    }
}

// ============================================================
// TOP BAR
// ============================================================

@Composable
private fun TopBar(
    onMoviesClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 52.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "IStreamingTV",
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(45.dp))
        NavigationItem(text = "Početna", selected = true)
        NavigationItem(text = "Filmovi", onClick = onMoviesClick)
        NavigationItem(text = "Serije")
        NavigationItem(text = "Uživo")
        Spacer(modifier = Modifier.weight(1f))
        NavigationItem(text = "⌕ Pretraga")
    }
}

@Composable
private fun NavigationItem(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    TvFocusableButton(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(46.dp)
            .width(if (text == "⌕ Pretraga") 130.dp else 100.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) TextPrimary else TextSecondary,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// ============================================================
// HERO CAROUSEL (kao Apple TV+)
// ============================================================

@Composable
private fun HeroCarousel(
    movies: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit
) {
    val listState = rememberLazyListState()
    var currentPage by remember { mutableIntStateOf(0) }

    // Auto-rotacija svakih 6 sekundi
    LaunchedEffect(movies.size) {
        while (true) {
            delay(6000)
            if (movies.size > 1) {
                currentPage = (currentPage + 1) % movies.size
                listState.animateScrollToItem(currentPage)
            }
        }
    }

    // Prati scroll za dot indikatore
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                currentPage = index
            }
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
        ) {
            // Carousel sa snap-om
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(
                    items = movies,
                    key = { it.id }
                ) { movie ->
                    HeroCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) }
                    )
                }
            }

            // Gradijent sa dna
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Background.copy(alpha = 0.6f),
                                Background
                            )
                        )
                    )
            )
        }

        // Dot indikatori
        if (movies.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(movies.size) { index ->
                    val isSelected = index == currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    movie: TmdbMovie,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(900.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
    ) {
        // Backdrop slika
        movie.backdropPath?.let { path ->
            AsyncImage(
                model = IMAGE_URL + BACKDROP_SIZE + path,
                contentDescription = movie.displayTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Tamni overlay sa gradijentom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Sadržaj
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(40.dp)
                .width(500.dp)
        ) {
            // Badge
            Box(
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(50.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "PREPORUČENO",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = movie.displayTitle,
                color = TextPrimary,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 48.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = movie.displayDate,
                color = TextSecondary,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = movie.overview,
                color = TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GlowButton(
                    text = "▶  Gledaj",
                    onClick = onClick,
                    isPrimary = true
                )
                GlowButton(
                    text = "+  Detalji",
                    onClick = onClick,
                    isPrimary = false
                )
            }
        }
    }
}

// ============================================================
// MOVIE ROW SA SNAP SCROLLING-OM
// ============================================================

@Composable
private fun MovieRowSnap(
    catalogId: String,
    title: String,
    movies: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit,
    viewModel: HomeViewModel
) {
    val rowState = rememberLazyListState()

    // Snap behavior — kartice se "zaključavaju"
    val snappingLayout = remember(rowState) {
        SnapLayoutInfoProvider(rowState)
    }
    val snapFlingBehavior = rememberSnapFlingBehavior(snappingLayout)

    LaunchedEffect(catalogId, movies.size) {
        val saved = viewModel.getCatalogPosition(catalogId)
        rowState.scrollToItem(
            index = saved.index.coerceAtLeast(0),
            scrollOffset = saved.offset
        )
    }

    LaunchedEffect(rowState) {
        snapshotFlow {
            Pair(
                rowState.firstVisibleItemIndex,
                rowState.firstVisibleItemScrollOffset
            )
        }.collect { position ->
            viewModel.saveCatalogPosition(
                catalogId = catalogId,
                index = position.first,
                offset = position.second
            )
        }
    }

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
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Prikaži sve ›",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyRow(
            state = rowState,
            flingBehavior = snapFlingBehavior,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(end = 48.dp)
        ) {
            items(
                items = movies,
                key = { movie -> movie.id }
            ) { movie ->
                MovieCardGlow(
                    movie = movie,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}

// ============================================================
// MOVIE CARD SA GLOW EFEKTOM (kao Apple TV)
// ============================================================

@Composable
private fun MovieCardGlow(
    movie: TmdbMovie,
    onClick: () -> Unit
) {
    TvFocusableButton(
        modifier = Modifier
            .width(180.dp)
            .height(270.dp),
        onClick = onClick
    ) { isFocused ->
        val scale = if (isFocused) 1.08f else 1f
        val elevation = if (isFocused) 16.dp else 0.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (isFocused) {
                        Modifier
                            .border(
                                width = 2.dp,
                                color = Color(0xFF3B82F6).copy(alpha = 0.8f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .drawWithContent {
                                drawContent()
                                // Glow efekat
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF3B82F6).copy(alpha = 0.25f),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width / 2, size.height / 2),
                                        radius = size.width * 0.8f
                                    )
                                )
                            }
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                )
        ) {
            MovieCard(
                posterPath = movie.posterPath,
                modifier = Modifier.fillMaxSize(),
                onClick = onClick
            )

            // Fade gradijent na dnu za naslov
            AnimatedVisibility(
                visible = isFocused,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            }
        }
    }
}

// ============================================================
// GLOW DUGME (za Hero sekciju)
// ============================================================

@Composable
private fun GlowButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean
) {
    TvFocusableButton(
        modifier = Modifier
            .width(if (isPrimary) 160.dp else 150.dp)
            .height(50.dp),
        onClick = onClick
    ) { isFocused ->
        val bgColor = if (isPrimary) {
            Color.White
        } else {
            Color.White.copy(alpha = 0.12f)
        }

        val textColor = if (isPrimary) {
            Color.Black
        } else {
            TextPrimary
        }

        val scale = if (isFocused) 1.05f else 1f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .then(
                    if (isFocused && isPrimary) {
                        Modifier.border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else if (isFocused) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
