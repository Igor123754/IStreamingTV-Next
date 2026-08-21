@file:OptIn(ExperimentalFoundationApi::class)

package com.igor.istreamingtv.ui.search

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.ui.components.NavBarDrawer
import com.igor.istreamingtv.ui.components.NavDestination
import com.igor.istreamingtv.ui.components.NavPill
import com.igor.istreamingtv.ui.components.TvFocusableButton
import kotlinx.coroutines.launch
import java.util.Locale

private val SearchBg = Color(0xFF05070B)

@Composable
private fun MicIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val s = w * 0.11f
        drawRoundRect(Color.White, Offset(w * 0.33f, h * 0.06f), Size(w * 0.34f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.17f), style = Stroke(s))
        drawArc(Color.White, startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(w * 0.16f, h * 0.28f), size = Size(w * 0.68f, h * 0.46f), style = Stroke(s))
        drawLine(Color.White, Offset(w * 0.5f, h * 0.74f), Offset(w * 0.5f, h * 0.94f), strokeWidth = s)
    }
}

@Composable
fun SearchScreen(
    onMovieClick: (TmdbMovie) -> Unit,
    onBack: () -> Unit,
    onOpenHome: () -> Unit = {},
    onOpenLiveTv: () -> Unit = {},
    onOpenMovies: () -> Unit = {}
) {
    val viewModel: SearchViewModel = viewModel()
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val searching by viewModel.searching.collectAsState()

    val context = LocalContext.current
    var navOpen by remember { mutableStateOf(false) }
    val pillFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val closeNav: () -> Unit = {
        navOpen = false
        scope.launch {
            try { pillFocus.requestFocus() } catch (_: Exception) {}
        }
    }

    BackHandler(enabled = navOpen) { closeNav() }

    var listening by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Glasovna pretraga nije dostupna na ovom uređaju", Toast.LENGTH_SHORT).show()
            return
        }
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() { }
            override fun onRmsChanged(rmsdB: Float) { }
            override fun onBufferReceived(buffer: ByteArray?) { }
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) {
                listening = false
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Toast.makeText(context, "Nismo ništa čuli — pokušaj ponovo", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!text.isNullOrBlank()) viewModel.setQuery(text)
            }
            override fun onPartialResults(partialResults: Bundle?) { }
            override fun onEvent(eventType: Int, params: Bundle?) { }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        sr.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) startListening()
        else Toast.makeText(context, "Dozvola za mikrofon je potrebna za glasovnu pretragu", Toast.LENGTH_SHORT).show()
    }

    fun onMicClick() {
        if (listening) {
            speechRecognizer?.stopListening()
            listening = false
        } else if (hasPermission) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { speechRecognizer?.destroy() }
    }

    Box(modifier = Modifier.fillMaxSize().background(SearchBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 48.dp, top = 40.dp)
        ) {
            NavPill(
                icon = Icons.Default.Search,
                label = "Pretraga",
                onClick = { navOpen = true },
                pillFocus = pillFocus
            )

            Spacer(modifier = Modifier.height(28.dp))

            SearchField(
                query = query,
                onQueryChange = viewModel::setQuery,
                listening = listening,
                onMicClick = ::onMicClick
            )

            Spacer(modifier = Modifier.height(32.dp))

            when {
                listening -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎤", fontSize = 44.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Slušam... reci naziv filma ili serije",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 18.sp
                            )
                        }
                    }
                }

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

        // ✅ NAV BAR — "Pretraga" selektovana + MOVIES grana
        NavBarDrawer(
            open = navOpen,
            current = NavDestination.SEARCH,
            onDismiss = closeNav,
            onNavigate = { dest ->
                closeNav()
                when (dest) {
                    NavDestination.HOME -> onOpenHome()
                    NavDestination.LIVE -> onOpenLiveTv()
                    NavDestination.MOVIES -> onOpenMovies()
                    NavDestination.SEARCH -> {}
                    else -> {}
                }
            }
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    listening: Boolean,
    onMicClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var micFocused by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }
    val micScale by animateFloatAsState(
        if (listening) 1.15f else if (micFocused) 1.1f else 1f,
        tween(300), label = ""
    )

    LaunchedEffect(Unit) {
        try { fieldFocus.requestFocus() } catch (_: Exception) {}
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(0.55f)
            .focusRequester(fieldFocus)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    listening -> Color(0xFFE50914).copy(alpha = 0.18f)
                    focused -> Color.White.copy(alpha = 0.16f)
                    else -> Color.White.copy(alpha = 0.08f)
                }
            )
            .then(
                when {
                    listening -> Modifier.border(2.dp, Color(0xFFE50914), RoundedCornerShape(12.dp))
                    focused -> Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    else -> Modifier
                }
            )
            .padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
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
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(micScale)
                .onFocusChanged { micFocused = it.isFocused }
                .focusable()
                .clickable(onClick = onMicClick)
                .clip(CircleShape)
                .background(
                    if (listening) Color(0xFFE50914)
                    else Color.White.copy(alpha = 0.12f)
                )
                .border(
                    1.dp,
                    when {
                        listening -> Color(0xFFE50914)
                        micFocused -> Color.White
                        else -> Color.White.copy(alpha = 0.3f)
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            MicIcon(Modifier.size(20.dp))
        }
    }
}

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
            modifier = Modifier.width(150.dp).height(225.dp)
        ) { focused ->
            val scale by animateFloatAsState(
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
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://image.tmdb.org/t/p/w342" + (movie.posterPath ?: movie.backdropPath ?: ""))
                        .crossfade(false)
                        .bitmapConfig(Bitmap.Config.RGB_565)
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
