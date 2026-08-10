package com.igor.istreamingtv.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.data.remote.backdropPath
import com.igor.istreamingtv.data.remote.displayDate
import com.igor.istreamingtv.data.remote.displayTitle
import com.igor.istreamingtv.data.remote.posterPath
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.theme.Background
import com.igor.istreamingtv.ui.theme.TextPrimary
import com.igor.istreamingtv.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale

private val CardShape =
    RoundedCornerShape(12.dp)

private val SoftWhite =
    Color(0xFFF5F5F5)

private val MutedWhite =
    Color(0xB3FFFFFF)

private val FocusWhite =
    Color.White

private val SideBarBackground =
    Color(0xF20A0A0C)

private val FocusBackground =
    Color(0x22FFFFFF)

private fun TmdbMovie.displayBackdropUrl(): String =
    backdropPath ?: posterPath ?: ""

private fun TmdbMovie.displayRating(): String =
    String.format(
        Locale.US,
        "%.1f",
        vote_average
    )

private fun TmdbMovie.displayYear(): String =
    displayDate.take(4)

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
                    message = state.error
                        ?: "Nepoznata greška",
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

@Composable
private fun HomeContent(
    movies: List<TmdbMovie>,
    series: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit,
    onMoviesClick: () -> Unit,
    viewModel: HomeViewModel
) {
    val heroMovies =
        remember(movies) {
            movies.take(5)
        }

    var heroIndex by remember {
        mutableIntStateOf(0)
    }

    var sideBarFocused by remember {
        mutableStateOf(false)
    }

    val featured =
        heroMovies.getOrNull(heroIndex)

    LaunchedEffect(heroMovies.size) {
        if (heroMovies.size < 2) {
            return@LaunchedEffect
        }

        while (true) {
            delay(8500)

            heroIndex =
                (heroIndex + 1) %
                    heroMovies.size
        }
    }

    val verticalState =
        rememberLazyListState()

    LaunchedEffect(
        movies.size,
        series.size
    ) {
        if (
            movies.isNotEmpty() ||
            series.isNotEmpty()
        ) {
            val saved =
                viewModel.getHomeVerticalPosition()

            verticalState.scrollToItem(
                index =
                    saved.index.coerceAtLeast(0),
                scrollOffset =
                    saved.offset
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

        if (featured != null) {
            PremiumHeroBackdrop(
                movie = featured
            )
        }

        PremiumHeroGradient()

        LazyColumn(
            state = verticalState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement =
                Arrangement.spacedBy(44.dp),
            contentPadding =
                PaddingValues(
                    start = 54.dp,
                    bottom = 100.dp
                )
        ) {

            item {
                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(560.dp)
                ) {
                    if (featured != null) {
                        PremiumHero(
                            movie = featured,
                            currentIndex = heroIndex,
                            totalCount =
                                heroMovies.size,
                            onMovieClick =
                                onMovieClick
                        )
                    }
                }
            }

            if (movies.isNotEmpty()) {
                item {
                    ContentRow(
                        catalogId =
                            "trending_movies",
                        title =
                            "Sada u trendu",
                        movies = movies,
                        entranceIndex = 1,
                        onMovieClick =
                            onMovieClick,
                        viewModel = viewModel
                    )
                }
            }

            if (series.isNotEmpty()) {
                item {
                    ContentRow(
                        catalogId =
                            "trending_series",
                        title =
                            "Popularne serije",
                        movies = series,
                        entranceIndex = 2,
                        onMovieClick =
                            onMovieClick,
                        viewModel = viewModel
                    )
                }
            }

            if (movies.size > 2) {
                item {
                    ContentRow(
                        catalogId =
                            "recommended",
                        title =
                            "Preporučeno za tebe",
                        movies =
                            movies.drop(2),
                        entranceIndex = 3,
                        onMovieClick =
                            onMovieClick,
                        viewModel = viewModel
                    )
                }
            }
        }

        SideNavigation(
            expanded = sideBarFocused,
            onExpandedChange = {
                sideBarFocused = it
            },
            onHomeClick = {},
            onMoviesClick = onMoviesClick
        )
    }
}

@Composable
private fun PremiumHeroBackdrop(
    movie: TmdbMovie
) {
    Crossfade(
        targetState = movie,
        animationSpec =
            tween(1000)
    ) { currentMovie ->

        val transition =
            rememberInfiniteTransition(
                label = "heroMotion"
            )

        val scale by transition.animateFloat(
            initialValue = 1.02f,
            targetValue = 1.09f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            18000,
                            easing = LinearEasing
                        ),
                    repeatMode =
                        RepeatMode.Reverse
                ),
            label = "heroScale"
        )

        AsyncImage(
            model =
                "https://image.tmdb.org/t/p/original" +
                    currentMovie.displayBackdropUrl(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale),
            contentScale =
                ContentScale.Crop
        )
    }
}

@Composable
private fun PremiumHeroGradient() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(
                            alpha = 0.92f
                        ),
                        Color.Black.copy(
                            alpha = 0.52f
                        ),
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = 1150f
                )
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(
                            alpha = 0.12f
                        ),
                        Color.Transparent,
                        Background
                    ),
                    startY = 0f,
                    endY = 850f
                )
            )
    )
}

@Composable
private fun PremiumHero(
    movie: TmdbMovie,
    currentIndex: Int,
    totalCount: Int,
    onMovieClick: (TmdbMovie) -> Unit
) {
    Crossfade(
        targetState = movie,
        animationSpec =
            tween(750)
    ) { currentMovie ->

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 120.dp,
                    start = 24.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(13.dp)
            ) {

                Text(
                    text =
                        "★ ${currentMovie.displayRating()}",
                    color = SoftWhite,
                    fontSize = 15.sp,
                    fontWeight =
                        FontWeight.Medium
                )

                Text(
                    text = "•",
                    color =
                        Color.White.copy(
                            alpha = 0.45f
                        )
                )

                if (
                    currentMovie
                        .displayYear()
                        .isNotEmpty()
                ) {
                    Text(
                        text =
                            currentMovie
                                .displayYear(),
                        color = MutedWhite,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "•",
                    color =
                        Color.White.copy(
                            alpha = 0.45f
                        )
                )

                Text(
                    text = "HD",
                    color = MutedWhite,
                    fontSize = 13.sp,
                    fontWeight =
                        FontWeight.Medium
                )
            }

            Text(
                text =
                    currentMovie.displayTitle,
                color = Color.White,
                fontSize = 58.sp,
                lineHeight = 64.sp,
                fontWeight =
                    FontWeight.Bold,
                maxLines = 2,
                overflow =
                    TextOverflow.Ellipsis,
                modifier =
                    Modifier.width(650.dp),
                style = TextStyle(
                    shadow = Shadow(
                        color =
                            Color.Black.copy(
                                alpha = 0.55f
                            ),
                        offset =
                            Offset(
                                0f,
                                3f
                            ),
                        blurRadius = 14f
                    )
                )
            )

            if (
                currentMovie.overview
                    .isNotBlank()
            ) {
                Text(
                    text =
                        currentMovie.overview,
                    color =
                        Color.White.copy(
                            alpha = 0.78f
                        ),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    maxLines = 3,
                    overflow =
                        TextOverflow.Ellipsis,
                    modifier =
                        Modifier.width(590.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                PremiumActionButton(
                    text = "Gledaj",
                    icon = "▶",
                    primary = true,
                    onClick = {
                        onMovieClick(
                            currentMovie
                        )
                    }
                )

                PremiumActionButton(
                    text = "Moja lista",
                    icon = "+",
                    primary = false,
                    onClick = {}
                )
            }

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {

                repeat(totalCount) { index ->

                    val selected =
                        index == currentIndex

                    val width by
                        animateDpAsState(
                            targetValue =
                                if (selected) {
                                    28.dp
                                } else {
                                    6.dp
                                },
                            animationSpec =
                                tween(300),
                            label =
                                "heroIndicator"
                        )

                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(5.dp)
                            .clip(
                                CircleShape
                            )
                            .background(
                                if (selected) {
                                    Color.White
                                } else {
                                    Color.White.copy(
                                        alpha = 0.32f
                                    )
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumActionButton(
    text: String,
    icon: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    TvFocusableButton(
        onClick = onClick
    ) { focused ->

        val scale by
            animateFloatAsState(
                targetValue =
                    if (focused) {
                        1.06f
                    } else {
                        1f
                    },
                animationSpec =
                    tween(180),
                label =
                    "actionScale"
            )

        Row(
            modifier = Modifier
                .scale(scale)
                .clip(
                    RoundedCornerShape(
                        26.dp
                    )
                )
                .background(
                    if (primary) {
                        Color.White
                    } else {
                        Color.White.copy(
                            alpha =
                                if (focused) {
                                    0.22f
                                } else {
                                    0.12f
                                }
                        )
                    }
                )
                .then(
                    if (
                        !primary &&
                        focused
                    ) {
                        Modifier.border(
                            1.dp,
                            Color.White.copy(
                                alpha = 0.75f
                            ),
                            RoundedCornerShape(
                                26.dp
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(
                    horizontal = 25.dp,
                    vertical = 13.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(9.dp)
        ) {

            Text(
                text = icon,
                color =
                    if (primary) {
                        Color.Black
                    } else {
                        Color.White
                    },
                fontSize = 14.sp
            )

            Text(
                text = text,
                color =
                    if (primary) {
                        Color.Black
                    } else {
                        Color.White
                    },
                fontSize = 15.sp,
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SideNavigation(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onHomeClick: () -> Unit,
    onMoviesClick: () -> Unit
) {
    val width by
        animateDpAsState(
            targetValue =
                if (expanded) {
                    230.dp
                } else {
                    74.dp
                },
            animationSpec =
                tween(260),
            label = "sidebarWidth"
        )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(width)
            .onFocusEvent { focusState ->
                onExpandedChange(
                    focusState.hasFocus
                )
            }
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        SideBarBackground,
                        SideBarBackground.copy(
                            alpha =
                                if (expanded) {
                                    0.96f
                                } else {
                                    0.68f
                                }
                        ),
                        Color.Transparent
                    )
                )
            )
            .padding(
                start = 14.dp,
                top = 34.dp,
                bottom = 34.dp
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxHeight(),
            verticalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {

            SideNavItem(
                icon = "⌂",
                title = "Početna",
                selected = true,
                expanded = expanded,
                onClick = onHomeClick
            )

            SideNavItem(
                icon = "▣",
                title = "Filmovi",
                selected = false,
                expanded = expanded,
                onClick = onMoviesClick
            )

            SideNavItem(
                icon = "▤",
                title = "Serije",
                selected = false,
                expanded = expanded,
                onClick = {}
            )

            SideNavItem(
                icon = "▶",
                title = "Uživo",
                selected = false,
                expanded = expanded,
                onClick = {}
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            SideNavItem(
                icon = "♡",
                title = "Moja lista",
                selected = false,
                expanded = expanded,
                onClick = {}
            )

            SideNavItem(
                icon = "⌕",
                title = "Pretraga",
                selected = false,
                expanded = expanded,
                onClick = {}
            )

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )

            SideNavItem(
                icon = "⚙",
                title = "Podešavanja",
                selected = false,
                expanded = expanded,
                onClick = {}
            )
        }
    }
}

@Composable
private fun SideNavItem(
    icon: String,
    title: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    TvFocusableButton(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(54.dp)
    ) { focused ->

        val scale by
            animateFloatAsState(
                targetValue =
                    if (focused) {
                        1.04f
                    } else {
                        1f
                    },
                animationSpec =
                    tween(180),
                label =
                    "sideItemScale"
            )

        Row(
            modifier = Modifier
                .scale(scale)
                .clip(
                    RoundedCornerShape(
                        10.dp
                    )
                )
                .background(
                    if (
                        focused ||
                        selected
                    ) {
                        FocusBackground
                    } else {
                        Color.Transparent
                    }
                )
                .padding(
                    horizontal = 15.dp,
                    vertical = 11.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = icon,
                color =
                    if (
                        selected ||
                        focused
                    ) {
                        FocusWhite
                    } else {
                        MutedWhite
                    },
                fontSize = 21.sp,
                fontWeight =
                    FontWeight.Normal,
                modifier =
                    Modifier.width(31.dp)
            )

            AnimatedVisibility(
                visible = expanded,
                enter =
                    fadeIn(
                        tween(180)
                    ),
                exit =
                    fadeOut(
                        tween(100)
                    )
            ) {

                Text(
                    text = title,
                    color =
                        if (
                            selected ||
                            focused
                        ) {
                            FocusWhite
                        } else {
                            MutedWhite
                        },
                    fontSize = 15.sp,
                    fontWeight =
                        if (selected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                    maxLines = 1
                )
            }
        }
    }
}

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
            SnapLayoutInfoProvider(
                rowState
            )
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
            scrollOffset =
                saved.offset
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
            .then(
                entranceModifier(
                    entranceIndex
                )
            )
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )

            Text(
                text = "Prikaži sve  ›",
                color =
                    Color.White.copy(
                        alpha = 0.55f
                    ),
                fontSize = 14.sp
            )
        }

        Spacer(
            modifier =
                Modifier.height(17.dp)
        )

        LazyRow(
            state = rowState,
            flingBehavior =
                snapFlingBehavior,
            horizontalArrangement =
                Arrangement.spacedBy(17.dp),
            contentPadding =
                PaddingValues(
                    end = 50.dp
                )
        ) {

            items(
                items = movies,
                key = { movie ->
                    movie.id
                }
            ) { movie ->

                PremiumPosterCard(
                    movie = movie,
                    onClick = {
                        onMovieClick(movie)
                    }
                )
            }
        }
    }
}

@Composable
private fun PremiumPosterCard(
    movie: TmdbMovie,
    onClick: () -> Unit
) {
    TvFocusableButton(
        onClick = onClick,
        modifier = Modifier
            .width(178.dp)
            .height(265.dp)
    ) { focused ->

        val scale by
            animateFloatAsState(
                targetValue =
                    if (focused) {
                        1.075f
                    } else {
                        1f
                    },
                animationSpec =
                    tween(220),
                label = "posterScale"
            )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(CardShape)
                .background(
                    Color(0xFF17171A)
                )
                .then(
                    if (focused) {
                        Modifier.border(
                            2.dp,
                            Color.White.copy(
                                alpha = 0.9f
                            ),
                            CardShape
                        )
                    } else {
                        Modifier
                    }
                )
        ) {

            AsyncImage(
                model =
                    "https://image.tmdb.org/t/p/w500" +
                        movie.posterPath,
                contentDescription =
                    movie.displayTitle,
                modifier =
                    Modifier.fillMaxSize(),
                contentScale =
                    ContentScale.Crop
            )

            if (focused) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(
                                        alpha = 0.85f
                                    )
                                )
                            )
                        )
                )

                Text(
                    text =
                        movie.displayTitle,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    maxLines = 2,
                    overflow =
                        TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .align(
                                Alignment.BottomStart
                            )
                            .padding(
                                12.dp
                            )
                )
            }
        }
    }
}

@Composable
private fun entranceModifier(
    index: Int
): Modifier {
    var shown by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        delay(
            130L * index
        )
        shown = true
    }

    val alpha by
        animateFloatAsState(
            targetValue =
                if (shown) {
                    1f
                } else {
                    0f
                },
            animationSpec =
                tween(550),
            label =
                "rowAlpha"
        )

    val offsetY by
        animateFloatAsState(
            targetValue =
                if (shown) {
                    0f
                } else {
                    45f
                },
            animationSpec =
                tween(550),
            label =
                "rowOffset"
        )

    return Modifier.graphicsLayer {
        this.alpha = alpha
        this.translationY = offsetY
    }
}

@Composable
fun ShimmerHomeScreen() {
    val infinite =
        rememberInfiniteTransition(
            label = "shimmer"
        )

    val progress by
        infinite.animateFloat(
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
                Color(0xFF111113),
                Color(0xFF29292D),
                Color(0xFF111113)
            ),
            startX =
                -700f +
                    progress * 2400f,
            endX =
                progress * 2400f
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Background
            )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(570.dp)
                .background(
                    shimmerBrush()
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 55.dp,
                    top = 520.dp
                )
        ) {

            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(28.dp)
                    .clip(
                        RoundedCornerShape(
                            8.dp
                        )
                    )
                    .background(
                        shimmerBrush()
                    )
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(17.dp)
            ) {

                repeat(6) {

                    Box(
                        modifier = Modifier
                            .width(178.dp)
                            .height(265.dp)
                            .clip(CardShape)
                            .background(
                                shimmerBrush()
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Background
            ),
        verticalArrangement =
            Arrangement.Center,
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "⚠",
            color = Color.White,
            fontSize = 46.sp
        )

        Spacer(
            modifier =
                Modifier.height(18.dp)
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
            modifier =
                Modifier.height(28.dp)
        )

        TvFocusableButton(
            onClick = onRetry
        ) { focused ->

            val scale by
                animateFloatAsState(
                    targetValue =
                        if (focused) {
                            1.06f
                        } else {
                            1f
                        },
                    animationSpec =
                        tween(180),
                    label =
                        "retryScale"
                )

            Box(
                modifier = Modifier
                    .scale(scale)
                    .clip(
                        RoundedCornerShape(
                            25.dp
                        )
                    )
                    .background(
                        Color.White
                    )
                    .padding(
                        horizontal = 30.dp,
                        vertical = 13.dp
                    )
            ) {

                Text(
                    text =
                        "Pokušaj ponovo",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }
}
