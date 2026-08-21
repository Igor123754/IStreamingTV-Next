package com.igor.istreamingtv.ui.movies

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.ui.components.MoviesIcon
import com.igor.istreamingtv.ui.components.NavBarDrawer
import com.igor.istreamingtv.ui.components.NavDestination
import com.igor.istreamingtv.ui.components.TvFocusableButton
import kotlinx.coroutines.launch

private val MoviesBg = Color(0xFF05070B)
private val CardBg = Color(0xFF151A21)
private val CardBorder = Color.White.copy(alpha = 0.06f)

/**
 * ✅ FILMOVI — Apple TV+ stil: pill + veliki naslov + gradient,
 *    19 PUNIH žanr kataloga (15 postera po redu).
 */
@Composable
fun MoviesScreen(
    onMovieClick: (TmdbMovie) -> Unit,
    onBack: () -> Unit,
    onOpenHome: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenLiveTv: () -> Unit = {}
) {
    val viewModel: MoviesViewModel = viewModel()
    val catalogs by viewModel.catalogs.collectAsState()
    val loading by viewModel.loading.collectAsState()

    val scope = rememberCoroutineScope()
    var navOpen by remember { mutableStateOf(false) }
    val pillFocus = remember { FocusRequester() }

    val closeNav: () -> Unit = {
        navOpen = false
        scope.launch { try { pillFocus.requestFocus() } catch (_: Exception) {} }
    }

    Box(modifier = Modifier.fillMaxSize().background(MoviesBg)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ✅ VRH: pill + veliki naslov (Apple TV+ stil)
            item(key = "top") {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(180.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1A1F2A), MoviesBg)
                                )
                            )
                    )
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp, end = 48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MoviesPill(pillFocus = pillFocus, onOpenNav = { navOpen = true })
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Filmovi",
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(start = 48.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Svi žanrovi · najsvežiji naslovi",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 48.dp)
                        )
                    }
                }
            }

            // ✅ Spinner dok nema redova
            if (loading && catalogs.isEmpty()) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                    }
                }
            }

            // ✅ ŽANR KATALOZI — puni redovi, stižu progresivno
            catalogs.forEach { catalog ->
                item(key = "genre_${catalog.id}") {
                    Column(modifier = Modifier.padding(top = 32.dp)) {
                        Text(
                            catalog.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 48.dp, bottom = 12.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(start = 48.dp, end = 48.dp)
                        ) {
                            items(catalog.items, key = { it.id }) { movie ->
                                MovieCard(movie = movie, onClick = { onMovieClick(movie) })
                            }
                        }
                    }
                }
            }

            // ✅ Spinner na dnu dok stižu ostali redovi
            if (loading && catalogs.isNotEmpty()) {
                item(key = "loading-more") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White.copy(alpha = 0.5f),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            item(key = "bottom-spacer") { Spacer(modifier = Modifier.height(60.dp)) }
        }

        // ✅ NAV BAR — "Filmovi" selektovani
        NavBarDrawer(
            open = navOpen,
            current = NavDestination.MOVIES,
            onDismiss = closeNav,
            onNavigate = { dest ->
                closeNav()
                when (dest) {
                    NavDestination.HOME -> onOpenHome()
                    NavDestination.SEARCH -> onOpenSearch()
                    NavDestination.LIVE -> onOpenLiveTv()
                    else -> {}
                }
            }
        )
    }
}

/** ✅ Pill "Filmovi" */
@Composable
private fun MoviesPill(
    pillFocus: FocusRequester,
    onOpenNav: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(180), label = "")
    Row(
        modifier = Modifier
            .focusRequester(pillFocus)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onOpenNav)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF48484A).copy(alpha = if (focused) 0.95f else 0.75f))
            .then(if (focused) Modifier.border(2.dp, Color.White, RoundedCornerShape(22.dp)) else Modifier)
            .padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(26.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            MoviesIcon(Modifier.size(15.dp))
        }
        Text("Filmovi", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** ✅ Poster kartica (Apple TV+ stil) */
@Composable
private fun MovieCard(
    movie: TmdbMovie,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.width(150.dp)) {
        TvFocusableButton(
            onClick = onClick,
            modifier = Modifier.width(150.dp).height(225.dp)
        ) { focused ->
            val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(220), label = "")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .then(
                        if (focused) Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp))
                        else Modifier.border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://image.tmdb.org/t/p/w342${movie.posterPath}")
                        .crossfade(false)
                        .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            movie.title ?: movie.name ?: "",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
