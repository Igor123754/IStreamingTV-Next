@file:OptIn(ExperimentalFoundationApi::class)

package com.igor.istreamingtv.ui.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.igor.istreamingtv.data.ContinueEntry
import com.igor.istreamingtv.data.ContinueWatchingStore
import com.igor.istreamingtv.data.remote.*
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.player.PlayerActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

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
    genreIds?.firstNotNullOfOrNull { genreNames[it] } ?: "Film"

private data class HeroItem(val movie: TmdbMovie, val isTv: Boolean)

@Composable
fun HomeScreen(
    onMovieClick: (TmdbMovie) -> Unit,
    onAddToLibrary: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshContinueWatching()
    }

    val onResume: (ContinueEntry) -> Unit = { entry ->
        scope.launch {
            if (entry.imdbId.isNullOrBlank()) return@launch
            val candidates = StreamPicker.getCandidates(
                if (entry.isTv) "series" else "movie",
                entry.imdbId,
                entry.season,
                entry.episode
            )
            if (candidates.isNotEmpty()) {
                val intent = Intent(context, PlayerActivity::class.java).apply {
                    putExtra(
                        "candidates",
                        TmdbClient.json.encodeToString(candidates.map { it.url })
                    )
                    putExtra("imdb_id", entry.imdbId)
                    putExtra("season", entry.season)
                    putExtra("episode", entry.episode)
                    putExtra("runtime_sec", (entry.durationMs / 1000).toInt())
                    putExtra("title", entry.title)
                    putExtra("poster", entry.posterUrl)
                    putExtra("backdrop", entry.backdropUrl)
                }
                context.startActivity(intent)
            }
        }
    }

    val onRemoveContinue: (ContinueEntry) -> Unit = { entry ->
        ContinueWatchingStore.remove(context, entry.key)
        Toast.makeText(context, "Uklonjeno: ${entry.title}", Toast.LENGTH_SHORT).show()
        viewModel.refreshContinueWatching()
    }

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
                continueWatching = state.continueWatching,
                heroExtras = state.heroExtras,
                initialScroll = viewModel.getHomeVerticalPosition(),
                onSaveScroll = viewModel::saveHomeVerticalPosition,
                getCatalogPosition = viewModel::getCatalogPosition,
                onSaveCatalogPosition = viewModel::saveCatalogPosition,
                onMovieClick = onMovieClick,
                onResume = onResume,
                onRemoveContinue = onRemoveContinue,
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
    continueWatching: List<ContinueEntry>,
    heroExtras: Map<Int, HeroExtras>,
    initialScroll: ScrollPosition,
    onSaveScroll: (Int, Int) -> Unit,
    getCatalogPosition: (String) -> ScrollPosition,
    onSaveCatalogPosition: (String, Int, Int) -> Unit,
    onMovieClick: (TmdbMovie) -> Unit,
    onResume: (ContinueEntry) -> Unit,
    onRemoveContinue: (ContinueEntry) -> Unit,
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

    val scope = rememberCoroutineScope()

    val openMovie: (TmdbMovie) -> Unit = { movie ->
        onSaveScroll(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset
        )
        onMovieClick(movie)
    }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

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
                        onHeroGainedFocus = {
                            if (listState.firstVisibleItemIndex != 0 ||
                                listState.firstVisibleItemScrollOffset != 0
                            ) {
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                        }
                    )
                }
            }
        }

        if (continueWatching.isNotEmpty()) {
            item(key = "continue") {
                ContinueRowSection(
                    entries = continueWatching,
                    onResume = onResume,
                    onRemove = onRemoveContinue
                )
            }
        }

        items(catalogs, key = { it.id }) { catalog ->
            CatalogRowSection(
                catalog = catalog,
                initialPosition = getCatalogPosition(catalog.id),
                onSavePosition = { index, offset ->
                    onSaveCatalogPosition(catalog.id, index, offset)
                },
                onMovieClick = openMovie
            )
        }

        item(key = "bottom-spacer") {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun ContinueRowSection(
    entries: List<ContinueEntry>,
    onResume: (ContinueEntry) -> Unit,
    onRemove: (ContinueEntry) -> Unit
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
            text = "Nastavi gledanje",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 48.dp, bottom = 16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp)
        ) {
            items(entries, key = { it.key }) { entry ->
                ContinueCard(
                    entry = entry,
                    onClick = { onResume(entry) },
                    onRemove = { onRemove(entry) }
                )
            }
        }
    }
}

@Composable
private fun ContinueCard(
    entry: ContinueEntry,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val fraction = if (entry.durationMs > 0) {
        (entry.positionMs.toFloat() / entry.durationMs).coerceIn(0f, 1f)
    } else 0f

    var focused by remember { mutableStateOf(false) }
    var pendingClick by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(220), label = "")

    Column(modifier = Modifier.width(240.dp)) {
        Box(
            modifier = Modifier
                .width(240.dp)
                .height(135.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onRemove
                )
                .focusable()
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    when {
                        event.type == KeyEventType.KeyDown &&
                            event.key == Key.DirectionCenter -> {
                            when {
                                event.nativeKeyEvent.repeatCount >= 6 -> {
                                    pendingClick = false
                                    onRemove()
                                    true
                                }
                                event.nativeKeyEvent.repeatCount > 0 -> true
                                else -> {
                                    pendingClick = true
                                    true
                                }
                            }
                        }
                        event.type == KeyEventType.KeyUp &&
                            event.key == Key.DirectionCenter -> {
                            if (pendingClick) onClick()
                            pendingClick = false
                            true
                        }
                        else -> false
                    }
                }
        ) {
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
                    model = entry.backdropUrl.ifBlank { entry.posterUrl },
                    contentDescription = entry.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = entry.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = if (entry.isTv) "Nastavi · S${entry.season}, E${entry.episode}"
            else "Nastavi gledanje",
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

/**
 * ✅ HERO BEZ PARALAKSE — skrol je sada glatak (nema per-frame skaliranja
 * fullscreen slike). Hero se samo normalno skroluje kao na Apple TV+.
 */
@Composable
private fun AppleTvHero(
    item: HeroItem,
    heroExtras: Map<Int, HeroExtras>,
    currentIndex: Int,
    totalCount: Int,
    onMovieClick: (TmdbMovie) -> Unit,
    onHeroGainedFocus: () -> Unit
) {
    Crossfade(targetState = item, animationSpec = tween(1000), label = "hero") { currentItem ->
        val movie = currentItem.movie
        val extras = heroExtras[movie.id]

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { if (it.hasFocus) onHeroGainedFocus() }
        ) {

            AsyncImage(
                model = "https://image.tmdb.org/t/p/w1280" + movie.displayBackdropUrl(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
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

            Box(modifier = Modifier.fillMaxSize()) {
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

@Composable
private fun CatalogRowSection(
    catalog: Catalog,
    initialPosition: ScrollPosition,
    onSavePosition: (Int, Int) -> Unit,
    onMovieClick: (TmdbMovie) -> Unit
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val rowAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(600), label = "row-alpha")
    val rowOffsetY by animateFloatAsState(if (entered) 0f else 80f, tween(600), label = "row-offset")

    val rowState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialPosition.index,
        initialFirstVisibleItemScrollOffset = initialPosition.offset
    )

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
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp)
        ) {
            items(catalog.items, key = { it.id }) { movie ->
                PosterCard(
                    movie = movie,
                    onClick = {
                        onSavePosition(
                            rowState.firstVisibleItemIndex,
                            rowState.firstVisibleItemScrollOffset
                        )
                        onMovieClick(movie)
                    }
                )
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
                    // ✅ w342 umesto w185 — oštri, čitljivi posteri i na tabletu i na TV-u
                    model = "https://image.tmdb.org/t/p/w342" + movie.posterPath,
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
