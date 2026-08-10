package com.igor.istreamingtv.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.igor.istreamingtv.data.TmdbMovie
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.theme.Background
import com.igor.istreamingtv.ui.theme.TextPrimary
import com.igor.istreamingtv.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
// BREND BOJE I GRADIJENTI
// ============================================================

private val AccentCyan = Color(0xFF00C2FF)
private val AccentViolet = Color(0xFF7A5CFF)
private val AccentGradient =
    Brush.horizontalGradient(listOf(AccentCyan, AccentViolet))
private val CardShape = RoundedCornerShape(14.dp)

// ============================================================
// POMOĆNE FUNKCIJE ZA MODEL
// ============================================================

private fun TmdbMovie.displayTitle(): String =
    title ?: name ?: "Nepoznat naslov"

private fun TmdbMovie.displayBackdrop(): String =
    backdropPath ?: posterPath ?: ""

private fun TmdbMovie.displayOverview(): String =
    overview ?: ""

private fun TmdbMovie.displayRating(): String =
    String.format(Locale.US, "%.1f", voteAverage ?: 0.0)

private fun TmdbMovie.displayYear(): String =
    (releaseDate ?: firstAirDate ?: "").take(4)

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
            state.isLoading -> ShimmerHomeScreen()

            state.error != null -> ErrorScreen(
                message = state.error ?: "Nepoznata greška",
                onRetry = viewModel::loadContent
            )

            else -> HomeContent(
                movies = state.movies,
                series = state.series,
                onMovieClick = onMovieClick,
                onMoviesClick = onMoviesClick,
                viewModel = viewModel
            )
        }
    }
}

// ============================================================
// HOME CONTENT — HERO + REDOVI
// ============================================================

@Composable
private fun HomeContent(
    movies: List<TmdbMovie>,
    series: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit,
    onMoviesClick: () -> Unit,
    viewModel: HomeViewModel
) {
    val heroMovies = remember(movies) {
        movies.take(5)
    }

    var heroIndex by remember {
        mutableIntStateOf(0)
    }

    var heroHasFocus by remember {
        mutableStateOf(false)
    }

    val featured = heroMovies.getOrNull(heroIndex)

    LaunchedEffect(heroHasFocus, heroMovies.size) {
        if (heroHasFocus || heroMovies.size < 2) {
            return@LaunchedEffect
        }

        while (true) {
            delay(8000)

            heroIndex =
                (heroIndex + 1) % heroMovies.size
        }
    }

    val verticalState = rememberLazyListState()

    LaunchedEffect(movies.size, series.size) {
        if (movies.isNotEmpty() || series.isNotEmpty()) {
            val saved =
                viewModel.getHomeVerticalPosition()

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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // ====================================================
        // HERO BACKDROP
        // ====================================================

        if (featured != null) {
            HeroBackdrop(movie = featured)
        }

        // ====================================================
        // GRADIENT SCRIM
        // ====================================================

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 320f
                    )
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Background
                        ),
                        startY = 480f,
                        endY = 900f
                    )
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Background.copy(alpha = 0.85f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 700f
                    )
                )
        )

        // ====================================================
        // SADRŽAJ
        // ====================================================

        LazyColumn(
            state = verticalState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(36.dp),
            contentPadding = PaddingValues(
                bottom = 80.dp
            )
        ) {

            item {
                TopBar(
                    onMoviesClick = onMoviesClick
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp)
                        .onFocusEvent {
                            heroHasFocus = it.hasFocus
                        }
                ) {
                    if (featured != null) {
                        HeroInfo(
                            movie = featured,
                            currentIndex = heroIndex,
                            totalCount = heroMovies.size,
                            onMovieClick = onMovieClick,
                            onIndexChange = {
                                heroIndex = it
                            }
                        )
                    }
                }
            }

            if (movies.isNotEmpty()) {
                item {
                    ContentRow(
                        catalogId = "trending_movies",
                        title = "Sada u trendu",
                        movies = movies,
                        entranceIndex = 1,
                        onMovieClick = onMovieClick,
                        viewModel = viewModel
                    )
                }
            }

            if (series.isNotEmpty()) {
                item {
                    ContentRow(
                        catalogId = "trending_series",
                        title = "Popularne serije",
                        movies = series,
                        entranceIndex = 2,
                        onMovieClick = onMovieClick,
                        viewModel = viewModel
                    )
                }
            }

            if (movies.size > 2) {
                item {
                    ContentRow(
                        catalogId = "movies",
                        title = "Preporučeno za tebe",
                        movies = movies.drop(2),
                        entranceIndex = 3,
                        onMovieClick = onMovieClick,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// ============================================================
// HERO POZADINA
// ============================================================

@Composable
private fun HeroBackdrop(
    movie: TmdbMovie
) {
    Crossfade(
        targetState = movie,
        animationSpec = tween(900)
    ) { m ->

        val infinite =
            rememberInfiniteTransition(
                label = "kenburns"
            )

        val scale by infinite.animateFloat(
            initialValue = 1.05f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    14000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "kenburnsScale"
        )

        AsyncImage(
            model =
                "https://image.tmdb.org/t/p/w1280${m.displayBackdrop()}",
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale),
            contentScale = ContentScale.Crop
        )
    }
}

// ============================================================
// HERO INFO
// ============================================================

@Composable
private fun HeroInfo(
    movie: TmdbMovie,
    currentIndex: Int,
    totalCount: Int,
    onMovieClick: (TmdbMovie) -> Unit,
    onIndexChange: (Int) -> Unit
) {
    Crossfade(
        targetState = movie,
        animationSpec = tween(700),
        modifier = Modifier
            .fillMaxWidth()
    ) { m ->

        Column(
            modifier = Modifier
                .padding(
                    start = 56.dp,
                    bottom = 12.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            // =================================================
            // METADATA
            // =================================================

            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                Text(
                    text =
                        "★ ${m.displayRating()}",
                    color =
                        Color(0xFFFFC107),
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                if (m.displayYear().isNotEmpty()) {
                    Text(
                        text = m.displayYear(),
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                }

                QualityBadge("4K")
                QualityBadge("HDR")
            }

            // =================================================
            // NASLOV
            // =================================================

            Text(
                text = m.displayTitle(),
                color = Color.White,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    shadow = Shadow(
                        color =
                            Color.Black.copy(alpha = 0.7f),
                        offset =
                            Offset(0f, 4f),
                        blurRadius = 16f
                    )
                )
            )

            // =================================================
            // SINOPSIS
            // =================================================

            if (m.displayOverview().isNotEmpty()) {
                Text(
                    text = m.displayOverview(),
                    color = TextSecondary,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(560.dp)
                )
            }

            // =================================================
            // DUGMAD
            // =================================================

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                HeroButton(
                    text = "Pogledaj",
                    icon = "▶",
                    primary = true,
                    onClick = {
                        onMovieClick(m)
                    }
                )

                HeroButton(
                    text = "Detalji",
                    icon = "ℹ",
                    primary = false,
                    onClick = {
                        onMovieClick(m)
                    }
                )

                HeroButton(
                    text = "Moja lista",
                    icon = "+",
                    primary = false,
                    onClick = {}
                )
            }

            // =================================================
            // CAROUSEL DOTS
            // =================================================

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                repeat(totalCount) { i ->

                    val selected =
                        i == currentIndex

                    val width by animateDpAsState(
                        targetValue =
                            if (selected) {
                                22.dp
                            } else {
                                7.dp
                            },
                        label = "dot"
                    )

                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) {
                                    AccentCyan
                                } else {
                                    Color.White.copy(
                                        alpha = 0.3f
                                    )
                                }
                            )
                    )
                }
            }

            onIndexChange(currentIndex)
        }
    }
}

// ============================================================
// QUALITY BADGE
// ============================================================

@Composable
private fun QualityBadge(
    text: String
) {
    Box(
        modifier = Modifier
            .border(
                1.dp,
                Color.White.copy(alpha = 0.35f),
                RoundedCornerShape(4.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 2.dp
            )
    ) {

        Text(
            text = text,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ============================================================
// HERO DUGME
// ============================================================

@Composable
private fun HeroButton(
    text: String,
    icon: String? = null,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    TvFocusableButton(
        onClick = onClick
    ) { isFocused ->

        val scale by animateFloatAsState(
            targetValue =
                if (isFocused) {
                    1.07f
                } else {
                    1f
                },
            animationSpec = tween(200),
            label = "btnScale"
        )

        Row(
            modifier = Modifier
                .scale(scale)
                .clip(
                    RoundedCornerShape(26.dp)
                )
                .then(
                    if (primary) {

                        Modifier.background(
                            if (isFocused) {
                                Color.White
                            } else {
                                Color.White.copy(
                                    alpha = 0.9f
                                )
                            }
                        )

                    } else {

                        Modifier
                            .background(
                                Color.White.copy(
                                    alpha =
                                        if (isFocused) {
                                            0.22f
                                        } else {
                                            0.10f
                                        }
                                )
                            )
                            .then(
                                if (isFocused) {
                                    Modifier.border(
                                        1.5.dp,
                                        AccentGradient,
                                        RoundedCornerShape(
                                            26.dp
                                        )
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    }
                )
                .padding(
                    horizontal = 28.dp,
                    vertical = 14.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            icon?.let {
                Text(
                    text = it,
                    color =
                        if (primary) {
                            Color.Black
                        } else {
                            Color.White
                        },
                    fontSize = 15.sp
                )
            }

            Text(
                text = text,
                color =
                    if (primary) {
                        Color.Black
                    } else {
                        Color.White
                    },
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.SemiBold
            )
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
    var clock by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

        while (true) {

            clock =
                SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                ).format(Date())

            delay(10_000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 52.dp,
                vertical = 24.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = "IStreaming",
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "TV",
            color = AccentCyan,
            fontSize = 26.sp,
            fontWeight =
                FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.width(45.dp)
        )

        NavigationItem(
            text = "Početna",
            selected = true
        )

        NavigationItem(
            text = "Filmovi",
            onClick = onMoviesClick
        )

        NavigationItem(
            text = "Serije"
        )

        NavigationItem(
            text = "Uživo"
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )

        Text(
            text = clock,
            color = TextSecondary,
            fontSize = 15.sp,
            fontWeight =
                FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.width(20.dp)
        )

        NavigationItem(
            text = "⌕"
        )
    }
}

// ============================================================
// NAVIGATION ITEM
// ============================================================

@Composable
private fun NavigationItem(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    TvFocusableButton(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(46.dp)
    ) { isFocused ->

        val scale by animateFloatAsState(
            targetValue =
                if (isFocused) {
                    1.08f
                } else {
                    1f
                },
            animationSpec = tween(200),
            label = "navScale"
        )

        val underline by animateDpAsState(
            targetValue =
                if (selected) {
                    24.dp
                } else {
                    0.dp
                },
            label = "underline"
        )

        Column(
            modifier = Modifier
                .scale(scale)
                .padding(horizontal = 12.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = text,
                color =
                    if (selected || isFocused) {
                        TextPrimary
                    } else {
                        TextSecondary
                    },
                fontSize = 15.sp,
                fontWeight =
                    if (selected) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    }
            )

            Box(
                modifier = Modifier
                    .width(underline)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(AccentGradient)
            )
        }
    }
}

// ============================================================
// CONTENT ROW
// ============================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContentRow(
    catalogId: String,
    title: String,
    movies: List<TmdbMovie>,
    entranceIndex: Int,
    onMovieClick: (TmdbMovie) -> Unit,
    viewModel: HomeViewModel
) {
    val rowState =
        rememberLazyListState()

    val snappingLayout =
        remember(rowState) {
            SnapLayoutInfoProvider(rowState)
        }

    val snapFlingBehavior =
        rememberSnapFlingBehavior(
            snappingLayout
        )

    LaunchedEffect(
        catalogId,
        movies.size
    ) {

        val saved =
            viewModel.getCatalogPosition(
                catalogId
            )

        rowState.scrollToItem(
            index =
                saved.index.coerceAtLeast(0),
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
            .then(
                entranceModifier(
                    entranceIndex
                )
            )
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(22.dp)
                    .clip(CircleShape)
                    .background(AccentGradient)
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "Prikaži sve ›",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        LazyRow(
            state = rowState,
            flingBehavior = snapFlingBehavior,
            horizontalArrangement =
                Arrangement.spacedBy(20.dp),
            contentPadding =
                PaddingValues(end = 48.dp)
        ) {

            items(
                items = movies,
                key = { movie ->
                    movie.id
                }
            ) { movie ->

                PosterCard(
                    movie = movie,
                    onClick = {
                        onMovieClick(movie)
                    }
                )
            }
        }
    }
}

// ============================================================
// ULAZNA ANIMACIJA
// ============================================================

@Composable
private fun entranceModifier(
    index: Int
): Modifier {

    var shown by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {

        delay(
            120L * index
        )

        shown = true
    }

    val alpha by animateFloatAsState(
        targetValue =
            if (shown) {
                1f
            } else {
                0f
            },
        animationSpec = tween(550),
        label = "entranceAlpha"
    )

    val offsetY by animateFloatAsState(
        targetValue =
            if (shown) {
                0f
            } else {
                60f
            },
        animationSpec = tween(550),
        label = "entranceY"
    )

    return Modifier.graphicsLayer {

        this.alpha = alpha
        this.translationY = offsetY
    }
}

// ============================================================
// POSTER CARD
// ============================================================

@Composable
private fun PosterCard(
    movie: TmdbMovie,
    onClick: () -> Unit
) {
    TvFocusableButton(
        onClick = onClick,
        modifier = Modifier
            .width(170.dp)
            .height(255.dp)
    ) { isFocused ->

        val scale by animateFloatAsState(
            targetValue =
                if (isFocused) {
                    1.08f
                } else {
                    1f
                },
            animationSpec = tween(250),
            label = "cardScale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(CardShape)
                .background(
                    Color(0xFF141419)
                )
                .then(
                    if (isFocused) {

                        Modifier
                            .border(
                                2.dp,
                                AccentGradient,
                                CardShape
                            )
                            .drawWithContent {

                                drawContent()

                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    AccentCyan.copy(
                                                        alpha = 0.22f
                                                    ),
                                                    Color.Transparent
                                                ),
                                            center =
                                                Offset(
                                                    size.width / 2,
                                                    size.height / 2
                                                ),
                                            radius =
                                                size.width * 0.85f
                                        )
                                )
                            }

                    } else {

                        Modifier.border(
                            1.dp,
                            Color.White.copy(
                                alpha = 0.06f
                            ),
                            CardShape
                        )
                    }
                )
        ) {

            AsyncImage(
                model =
                    "https://image.tmdb.org/t/p/w500${movie.posterPath}",
                contentDescription =
                    movie.displayTitle(),
                modifier =
                    Modifier.fillMaxSize(),
                contentScale =
                    ContentScale.Crop
            )

            // =================================================
            // RATING BADGE
            // =================================================

            if (isFocused) {

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(
                            RoundedCornerShape(6.dp)
                        )
                        .background(
                            Color.Black.copy(
                                alpha = 0.7f
                            )
                        )
                        .padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                ) {

                    Text(
                        text =
                            "★ ${movie.displayRating()}",
                        color =
                            Color(0xFFFFC107),
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }

            // =================================================
            // DONJI GRADIJENT + NASLOV
            // =================================================

            if (isFocused) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(
                            Alignment.BottomCenter
                        )
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(
                                        alpha = 0.85f
                                    )
                                )
                            )
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 8.dp
                        )
                ) {

                    Text(
                        text =
                            movie.displayTitle(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.SemiBold,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ============================================================
// SHIMMER LOADING SCREEN
// ============================================================

@Composable
fun ShimmerHomeScreen() {

    val infinite =
        rememberInfiniteTransition(
            label = "shimmer"
        )

    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween<Float>(
                        1100,
                        easing = LinearEasing
                    ),
                repeatMode =
                    RepeatMode.Restart
            ),
        label = "shimmerProgress"
    )

    fun shimmerBrush(): Brush =
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF17171D),
                Color(0xFF2A2A34),
                Color(0xFF17171D)
            ),
            startX =
                -600f + progress * 2200f,
            endX =
                progress * 2200f
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(
                horizontal = 48.dp,
                vertical = 24.dp
            )
    ) {

        // Top bar placeholder

        Box(
            modifier = Modifier
                .width(220.dp)
                .height(32.dp)
                .clip(
                    RoundedCornerShape(8.dp)
                )
                .background(
                    shimmerBrush()
                )
        )

        Spacer(
            modifier = Modifier.height(48.dp)
        )

        // Hero placeholder

        Box(
            modifier = Modifier
                .width(520.dp)
                .height(64.dp)
                .clip(
                    RoundedCornerShape(12.dp)
                )
                .background(
                    shimmerBrush()
                )
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Box(
            modifier = Modifier
                .width(360.dp)
                .height(20.dp)
                .clip(
                    RoundedCornerShape(8.dp)
                )
                .background(
                    shimmerBrush()
                )
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(48.dp)
                    .clip(
                        RoundedCornerShape(24.dp)
                    )
                    .background(
                        shimmerBrush()
                    )
            )

            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(48.dp)
                    .clip(
                        RoundedCornerShape(24.dp)
                    )
                    .background(
                        shimmerBrush()
                    )
            )
        }

        Spacer(
            modifier = Modifier.height(56.dp)
        )

        // Row placeholder

        Box(
            modifier = Modifier
                .width(260.dp)
                .height(26.dp)
                .clip(
                    RoundedCornerShape(8.dp)
                )
                .background(
                    shimmerBrush()
                )
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(20.dp)
        ) {

            repeat(6) {

                Box(
                    modifier = Modifier
                        .width(170.dp)
                        .height(255.dp)
                        .clip(CardShape)
                        .background(
                            shimmerBrush()
                        )
                )
            }
        }
    }
}

// ============================================================
// ERROR SCREEN
// ============================================================

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        verticalArrangement =
            Arrangement.Center,
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "⚠️",
            fontSize = 52.sp
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = message,
            color = TextSecondary,
            fontSize = 16.sp,
            maxLines = 2,
            overflow =
                TextOverflow.Ellipsis
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        TvFocusableButton(
            onClick = onRetry
        ) { isFocused ->

            val scale by animateFloatAsState(
                targetValue =
                    if (isFocused) {
                        1.07f
                    } else {
                        1f
                    },
                animationSpec = tween(200),
                label = "retryScale"
            )

            Box(
                modifier = Modifier
                    .scale(scale)
                    .clip(
                        RoundedCornerShape(26.dp)
                    )
                    .background(
                        if (isFocused) {
                            Color.White
                        } else {
                            Color.White.copy(
                                alpha = 0.9f
                            )
                        }
                    )
                    .padding(
                        horizontal = 32.dp,
                        vertical = 14.dp
                    )
            ) {

                Text(
                    text = "Pokušaj ponovo",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }
}
