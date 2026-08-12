package com.igor.istreamingtv.ui.details

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.igor.istreamingtv.data.remote.*
import com.igor.istreamingtv.data.remote.stremio.StremioStream
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.player.PlayerActivity
import java.text.SimpleDateFormat
import java.util.Locale

private val DetailsBackground = Color(0xFF020204)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xB3FFFFFF)

@Composable
fun MovieDetailsScreen(
    movie: TmdbMovie,
    onBack: () -> Unit
) {
    val viewModel: MovieDetailsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    // Serije imaju "name", filmovi "title"
    val isTv = movie.name != null

    LaunchedEffect(movie.id) {
        viewModel.load(movie, isTv)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailsBackground)
    ) {
        when {
            state.isLoading -> DetailsShimmer()
            state.error != null -> DetailsError(
                message = state.error ?: "Nepoznata greška",
                onBack = onBack
            )
            else -> DetailsContent(
                movie = movie,
                details = state.details,
                streams = state.streams,
                isTv = isTv,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun DetailsContent(
    movie: TmdbMovie,
    details: TmdbHeroDetails?,
    streams: List<StremioStream>,
    isTv: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var inLibrary by remember { mutableStateOf(false) }
    var selectedStream by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }

    // Stvarni podaci
    val logoUrl = details?.pickClearLogoUrl()
    val overview = details?.pickSerbianOverview()
        ?: details?.overview?.takeIf { it.isNotBlank() }
        ?: movie.displayOverview
    val genres = details?.genres?.joinToString(", ") { it.name } ?: ""
    val date = details?.release_date ?: details?.first_air_date ?: movie.displayDate
    val runtimeMin = details?.runtime ?: details?.episode_run_time?.firstOrNull()
    val cert = details?.pickCertification()
    val cast = details?.credits?.cast
        ?.mapNotNull { it.name }
        ?.take(3)
        ?.joinToString(", ")
        ?: ""

    val backdropUrl = "https://image.tmdb.org/t/p/w1280" +
        (details?.backdrop_path ?: movie.backdropPath ?: details?.poster_path ?: movie.posterPath ?: "")

    fun playStream(stream: StremioStream) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra("stream", Gson().toJson(stream))
        }
        context.startActivity(intent)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1) Fanart preko celog ekrana
        AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2) Gradient dole za čitljivost (kao na screenshot-u)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.92f)
                        ),
                        startY = 300f
                    )
                )
        )

        // 3) Clearlogo gore levo (srpski ako postoji, inače originalni)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 48.dp, top = 44.dp)
        ) {
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = movie.displayTitle,
                    modifier = Modifier
                        .width(340.dp)
                        .height(120.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart
                )
            } else {
                Text(
                    text = movie.displayTitle,
                    color = TextPrimary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 500.dp)
                )
            }
        }

        // 4) "Nazad" pilula gore desno
        TvFocusableButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 48.dp, top = 44.dp)
        ) { focused ->
            val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(160), label = "")
            Box(
                modifier = Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("← Nazad", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // 5) Donja sekcija: dugmad | opis | uloge + meta red
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, bottom = 40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Levo: dugmad
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // GLEDAY
                    TvFocusableButton(onClick = {
                        if (!isTv && streams.isNotEmpty()) {
                            playStream(streams[selectedStream.coerceIn(0, streams.size - 1)])
                        } else {
                            notice = if (isTv) "Reprodukcija serija stiže uskoro 🎬"
                            else "Nema dostupnih izvora za ovaj naslov"
                        }
                    }) { focused ->
                        val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(160), label = "")
                        Row(
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFE9E9F2))
                                .padding(horizontal = 32.dp, vertical = 13.dp),
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

                    // BIBLIOTEKA toggle
                    TvFocusableButton(onClick = { inLibrary = !inLibrary }) { focused ->
                        val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(160), label = "")
                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 26.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = if (inLibrary) "✓ U biblioteci" else "＋ U biblioteku",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Obaveštenje (nema izvora / serije uskoro)
                    notice?.let {
                        Text(text = it, color = TextSecondary, fontSize = 13.sp)
                    }
                }

                // Sredina: opis
                Text(
                    text = overview,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1.2f)
                )

                // Desno: uloge
                if (cast.isNotBlank()) {
                    Text(
                        text = "Uloge: $cast",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Meta red: žanr • datum • trajanje • uzrast
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (genres.isNotBlank()) {
                    Text(genres, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                val dateText = formatDate(date)
                if (dateText.isNotBlank()) {
                    Text(dateText, color = TextSecondary, fontSize = 14.sp)
                }
                val runtimeText = formatRuntime(runtimeMin)
                if (runtimeText.isNotBlank()) {
                    Text(runtimeText, color = TextSecondary, fontSize = 14.sp)
                }
                if (!cert.isNullOrBlank()) {
                    Text(
                        text = cert,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Izvori (samo ako ima više stream-ova za film)
            if (!isTv && streams.size > 1) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(minOf(streams.size, 5)) { index ->
                        TvFocusableButton(onClick = { selectedStream = index }) { focused ->
                            val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(150), label = "")
                            Box(
                                modifier = Modifier
                                    .scale(scale)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (index == selectedStream) Color.White
                                        else Color.White.copy(alpha = 0.14f)
                                    )
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Izvor ${index + 1}",
                                    color = if (index == selectedStream) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val output = SimpleDateFormat("d. MMMM yyyy.", Locale("sr"))
        output.format(input.parse(iso)!!)
    } catch (_: Exception) {
        iso
    }
}

private fun formatRuntime(minutes: Int?): String {
    if (minutes == null || minutes <= 0) return ""
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h} h ${m} min" else "${m} min"
}

@Composable
private fun DetailsShimmer() {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val progress by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = ""
    )

    val brush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF111113), Color(0xFF29292D), Color(0xFF111113)),
        startX = -700f + progress * 2400f, endX = progress * 2400f
    )

    Box(modifier = Modifier.fillMaxSize().background(brush))
}

@Composable
private fun DetailsError(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(DetailsBackground),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "⚠", color = Color.White, fontSize = 46.sp)
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = message, color = TextSecondary, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(28.dp))
        TvFocusableButton(onClick = onBack) { focused ->
            val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(180), label = "")
            Box(
                modifier = Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color.White)
                    .padding(horizontal = 30.dp, vertical = 13.dp)
            ) {
                Text("Nazad", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
