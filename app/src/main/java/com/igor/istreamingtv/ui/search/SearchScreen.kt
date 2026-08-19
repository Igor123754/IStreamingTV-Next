@file:OptIn(ExperimentalFoundationApi::class)

package com.igor.istreamingtv.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.ui.components.TvFocusableButton

private val SearchBg = Color(0xFF020204)

/**
 * ✅ PRETRAGA — Apple TV+ stil: polje gore, grid postera dole.
 *    Prazno polje → "Popularno"; kucaš → rezultati uživo.
 */
@Composable
fun SearchScreen(
    onMovieClick: (TmdbMovie) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: SearchViewModel = viewModel()
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val searching by viewModel.searching.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchBg)
            .padding(start = 48.dp, end = 48.dp, top = 40.dp)
    ) {
        Text("Pretraga", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        SearchField(query = query, onQueryChange = viewModel::setQuery)

        Spacer(modifier = Modifier.height(32.dp))

        when {
            query.isBlank() -> {
                Text(
                    "Popularno",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ResultsGrid(items = suggestions, onMovieClick = onMovieClick)
            }

            searching -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                }
            }

            results.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nema rezultata za \"$query\"",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 18.sp
                    )
                }
            }

            else -> {
                Text(
                    "Rezultati",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ResultsGrid(items = results, onMovieClick = onMovieClick)
            }
        }
    }
}

/** ✅ Polje za pretragu — fokus + tastatura (TV IME ili tablet) */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try { fieldFocus.requestFocus() } catch (_: Exception) {}
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(0.55f)
            .focusRequester(fieldFocus)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f))
            .then(
                if (focused) Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp))
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Search,
            null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** ✅ Grid postera (kao Apple TV+ rezultati) */
@Composable
private fun ResultsGrid(
    items: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { movie ->
            SearchPosterCard(movie = movie, onClick = { onMovieClick(movie) })
        }
    }
}

@Composable
private fun SearchPosterCard(
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
            val scale by androidx.compose.animation.core.animateFloatAsState(
                if (focused) 1.08f else 1f,
                tween(220), label = ""
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0C0D12))
                    .then(
                        if (focused) Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp))
                        else Modifier
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data("https://image.tmdb.org/t/p/w342" + (movie.posterPath ?: movie.backdropPath ?: ""))
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun tween(durationMillis: Int) =
    androidx.compose.animation.core.tween<Float>(durationMillis)
