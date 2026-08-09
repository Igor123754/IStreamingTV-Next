package com.igor.istreamingtv.ui.details

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.igor.istreamingtv.data.remote.stremio.StremioStream
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.player.PlayerActivity
import com.igor.istreamingtv.ui.theme.Background
import com.igor.istreamingtv.ui.theme.TextPrimary
import com.igor.istreamingtv.ui.theme.TextSecondary

private const val BACKDROP_URL = "https://image.tmdb.org/t/p/w1280"

@Composable
fun MovieDetailsScreen(
    movieId: Int,                    // Sada prima ID umesto celog objekta
    onBack: () -> Unit,
    viewModel: MovieDetailsViewModel = viewModel {
        // Factory bi trebalo preko Hilt-a, ali za sada ručno:
        MovieDetailsViewModel(
            streamRepository = com.igor.istreamingtv.data.repository.StreamRepository(
                tmdbApi = com.igor.istreamingtv.data.remote.TmdbClient.retrofit.create(
                    com.igor.istreamingtv.data.remote.TmdbApi::class.java
                ),
                addonManager = com.igor.istreamingtv.data.remote.stremio.AddonManager(),
                apiKey = com.igor.istreamingtv.BuildConfig.TMDB_API_KEY
            )
        )
    }
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    BackHandler { onBack() }

    // Učitaj podatke kada se ekran otvori
    LaunchedEffect(movieId) {
        viewModel.loadMovieDetails(movieId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Učitavanje detalja...",
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                }
            }

            state.error != null -> {
                ErrorDetails(
                    message = state.error!!,
                    onBack = onBack,
                    onRetry = { viewModel.loadMovieDetails(movieId) }
                )
            }

            state.movieDetails != null -> {
                val movie = state.movieDetails!!

                // BACKDROP
                movie.backdrop_path?.let { path ->
                    AsyncImage(
                        model = BACKDROP_URL + path,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // GRADIENTI
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.96f),
                                    Color.Black.copy(alpha = 0.82f),
                                    Color.Black.copy(alpha = 0.48f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Background.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                // SADRŽAJ
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 60.dp, vertical = 42.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // NAZAD DUGME
                    TvFocusableButton(
                        modifier = Modifier
                            .width(110.dp)
                            .height(48.dp),
                        onClick = onBack
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "‹ Nazad",
                                color = TextPrimary
                            )
                        }
                    }

                    // INFO + STREAMOVI
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp)
                    ) {
                        // TAGLINE / ŽANR
                        val genres = movie.genres.joinToString(", ") { it.name }
                        if (genres.isNotEmpty()) {
                            GlassBadge(text = genres)
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // NASLOV
                        Text(
                            text = movie.title,
                            color = TextPrimary,
                            fontSize = 38.sp,
                            lineHeight = 44.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // META INFO
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            movie.release_date?.take(4)?.let { year ->
                                Text(text = year, color = TextSecondary, fontSize = 15.sp)
                            }
                            movie.runtime?.let { min ->
                                Text(
                                    text = "${min / 60}h ${min % 60}min",
                                    color = TextSecondary,
                                    fontSize = 15.sp
                                )
                            }
                            Text(
                                text = "★ ${String.format("%.1f", movie.vote_average)}",
                                color = TextSecondary,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // OVERVIEW
                        Text(
                            text = movie.overview,
                            color = TextSecondary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // STREAMOVI
                        if (state.streams.isNotEmpty()) {
                            Text(
                                text = "Dostupni izvori (${state.streams.size}):",
                                color = TextPrimary,
                                fontSize = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.streams) { stream ->
                                    StreamButton(
                                        stream = stream,
                                        onClick = {
                                            if (stream.isPlayable() && stream.url != null) {
                                                val headers = stream.behaviorHints
                                                    ?.proxyHeaders
                                                    ?.request

                                                context.startActivity(
                                                    PlayerActivity.newIntent(
                                                        context = context,
                                                        url = stream.url,
                                                        title = stream.displayTitle(),
                                                        headers = headers
                                                    )
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        } else if (!state.isLoading) {
                            Text(
                                text = "Nema dostupnih izvora za ovaj film.",
                                color = TextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassBadge(text: String) {
    Box(
        modifier = Modifier
            .background(
                Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text = text, color = TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun StreamButton(
    stream: StremioStream,
    onClick: () -> Unit
) {
    TvFocusableButton(
        modifier = Modifier
            .width(180.dp)
            .height(56.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stream.displayTitle(),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                if (stream.name != null) {
                    Text(
                        text = stream.name,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorDetails(
    message: String,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Greška",
                color = TextPrimary,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvFocusableButton(
                    modifier = Modifier.width(150.dp).height(48.dp),
                    onClick = onRetry
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Pokušaj ponovo", color = TextPrimary)
                    }
                }
                TvFocusableButton(
                    modifier = Modifier.width(120.dp).height(48.dp),
                    onClick = onBack
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Nazad", color = TextPrimary)
                    }
                }
            }
        }
    }
}
