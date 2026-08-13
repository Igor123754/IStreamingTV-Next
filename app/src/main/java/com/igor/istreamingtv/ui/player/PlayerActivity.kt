package com.igor.istreamingtv.ui.player

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rewind
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.google.gson.JsonParser
import com.igor.istreamingtv.data.remote.StreamPicker
import com.igor.istreamingtv.data.remote.SubtitleFetcher
import com.igor.istreamingtv.data.remote.SubtitleTrack
import com.igor.istreamingtv.ui.components.TvFocusableButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * MOĆNI PLEJER — Media3 ExoPlayer (optimizovan za 2GB RAM)
 * + APPLE TV+ STIL UI: crni letterbox, auto-hide kontrole,
 * gradient trake, okrugla dugmad, tanak progress, CC, buffering spinner.
 */
class PlayerActivity : ComponentActivity() {

    internal var player: ExoPlayer? = null
    internal var playerTitle: String = ""
    private var playerView: PlayerView? = null

    private val candidateUrls = mutableListOf<String>()
    private var candidateIndex = 0
    private var currentUrl: String? = null
    private val currentSubtitleTracks = mutableListOf<SubtitleTrack>()

    private var imdbId: String? = null
    private var seasonNumber: Int = -1
    private var episodeNumber: Int = -1
    private var runtimeSec: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // ✅ FIX sivih traka: ceo prozor je CRN (letterbox 21:9)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))

        parseIntent()
        if (candidateUrls.isEmpty()) {
            Toast.makeText(this, "Nema izvora za reprodukciju", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        imdbId = intent.getStringExtra("imdb_id")
            ?: intent.getStringExtra("series_imdb")
        seasonNumber = intent.getIntExtra("season", -1)
        episodeNumber = intent.getIntExtra("episode", -1)
        runtimeSec = intent.getIntExtra("runtime_sec", -1)
        playerTitle = intent.getStringExtra("title") ?: ""

        // Apple TV+ UI preko Compose-a
        val composeView = ComposeView(this)
        composeView.setBackgroundColor(android.graphics.Color.BLACK)
        composeView.setContent {
            AppleTvPlayerScreen(activity = this@PlayerActivity)
        }
        setContentView(composeView)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, composeView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Fallback fetch titlova ako nisu stigli sa detalja (max 2.5s)
        lifecycleScope.launch {
            if (currentSubtitleTracks.isEmpty() && !imdbId.isNullOrBlank()) {
                val type = if (seasonNumber >= 0) "series" else "movie"
                val fetched = withTimeoutOrNull(2500) {
                    withContext(Dispatchers.IO) {
                        try {
                            val entries = SubtitleFetcher.getAcceptedSubtitles(
                                type, imdbId!!, seasonNumber, episodeNumber
                            )
                            val ranked = SubtitleFetcher.rankBySync(
                                entries, if (runtimeSec > 0) runtimeSec else null
                            )
                            SubtitleFetcher.toTracks(ranked, 6)
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                }
                if (!fetched.isNullOrEmpty()) {
                    currentSubtitleTracks.clear()
                    currentSubtitleTracks.addAll(fetched)
                }
            }
            withContext(Dispatchers.Main) {
                playCandidate(0)
            }
        }
    }

    private fun parseIntent() {
        intent.getStringExtra("candidates")?.let { json ->
            try {
                val arr = JsonParser.parseString(json).asJsonArray
                arr.forEach { el ->
                    el.takeIf { !it.isJsonNull }?.asString?.let { candidateUrls.add(it) }
                }
            } catch (_: Exception) {
                // Ignoriši
            }
        }

        intent.getStringExtra("subtitle_tracks")?.let { json ->
            try {
                val arr = JsonParser.parseString(json).asJsonArray
                arr.forEach { el ->
                    val o = el.asJsonObject
                    val url = o.get("url")?.takeIf { !it.isJsonNull }?.asString ?: return@forEach
                    val lang = o.get("lang")?.takeIf { !it.isJsonNull }?.asString ?: "sr"
                    currentSubtitleTracks.add(SubtitleTrack(url, lang))
                }
            } catch (_: Exception) {
                // Ignoriši
            }
        }
        if (currentSubtitleTracks.isEmpty()) {
            intent.getStringExtra("subtitle_urls")?.let { json ->
                try {
                    val arr = JsonParser.parseString(json).asJsonArray
                    arr.forEach { el ->
                        el.takeIf { !it.isJsonNull }?.asString?.let {
                            currentSubtitleTracks.add(SubtitleTrack(it, "sr"))
                        }
                    }
                } catch (_: Exception) {
                    // Ignoriši
                }
            }
        }
        if (currentSubtitleTracks.isEmpty()) {
            intent.getStringExtra("subtitle_url")?.let {
                currentSubtitleTracks.add(SubtitleTrack(it, "sr"))
            }
        }

        if (candidateUrls.isEmpty()) {
            intent.getStringExtra("url")?.takeIf { it.isNotBlank() }?.let { candidateUrls.add(it) }
        }
        if (candidateUrls.isEmpty()) {
            intent.getStringExtra("stream")?.let { json ->
                try {
                    val obj = JsonParser.parseString(json).asJsonObject
                    val url = obj.get("url")?.takeIf { !it.isJsonNull }?.asString
                        ?: obj.get("externalUrl")?.takeIf { !it.isJsonNull }?.asString
                    if (!url.isNullOrBlank()) candidateUrls.add(url)
                } catch (_: Exception) {
                    if (json.startsWith("http")) candidateUrls.add(json)
                }
            }
        }
        intent.data?.toString()?.let { if (candidateUrls.isEmpty()) candidateUrls.add(it) }
    }

    private fun playCandidate(index: Int) {
        candidateIndex = index
        currentUrl = candidateUrls[index]
        buildPlayer(currentUrl!!, currentSubtitleTracks.toList())
    }

    private fun buildPlayer(url: String, tracks: List<SubtitleTrack>) {
        val context = this

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val trackSelector = DefaultTrackSelector(context)
        val paramsBuilder = DefaultTrackSelector.Parameters.Builder(context)
            .setPreferredAudioLanguage("en")
        if (tracks.isNotEmpty()) {
            paramsBuilder.setPreferredTextLanguage(tracks.first().lang)
        }
        trackSelector.parameters = paramsBuilder.build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(8_000, 30_000, 2_000, 5_000)
            .setTargetBufferBytes(24 * 1024 * 1024)
            .setBackBuffer(15_000, false)
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(
                mapOf("User-Agent" to "IStreamingTV/1.0 (Android; Media3)")
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        player?.release()

        val exoPlayer = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        exoPlayer.setHandleAudioBecomingNoisy(true)

        val prefs = getSharedPreferences("player_positions", MODE_PRIVATE)
        val savedPosition = prefs.getLong(url, -1L)

        val mediaItemBuilder = MediaItem.Builder().setUri(url)
        if (tracks.isNotEmpty()) {
            var srCount = 0
            var hrCount = 0
            val configs = tracks.mapIndexed { i, track ->
                val isHr = track.lang == "hr"
                val label = if (isHr) {
                    hrCount++
                    if (hrCount == 1) "Hrvatski" else "Hrvatski $hrCount"
                } else {
                    srCount++
                    if (srCount == 1) "Srpski" else "Srpski $srCount"
                }
                val mime = when {
                    track.url.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                    track.url.endsWith(".ass", ignoreCase = true) ||
                        track.url.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
                    else -> MimeTypes.APPLICATION_SUBRIP
                }
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url))
                    .setMimeType(mime)
                    .setLanguage(if (isHr) "hr" else "sr")
                    .setLabel(label)
                    .setSelectionFlags(if (i == 0) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }
            mediaItemBuilder.setSubtitleConfigurations(configs)
        }

        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        if (savedPosition > 0) exoPlayer.seekTo(savedPosition)
        exoPlayer.playWhenReady = true

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (candidateIndex < candidateUrls.size - 1) {
                    Toast.makeText(
                        context,
                        "Izvor ne radi — prelazim na sledeći...",
                        Toast.LENGTH_SHORT
                    ).show()
                    playCandidate(candidateIndex + 1)
                } else {
                    Toast.makeText(
                        context,
                        "Greška reprodukcije: ${error.errorCodeName}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    tryNextEpisode()
                }
            }
        })

        player = exoPlayer
        playerView?.player = exoPlayer
    }

    private fun tryNextEpisode() {
        val imdb = imdbId ?: return
        if (seasonNumber < 0 || episodeNumber < 0) return
        val nextEpisode = episodeNumber + 1

        lifecycleScope.launch(Dispatchers.IO) {
            val candidates = StreamPicker.getCandidates("series", imdb, seasonNumber, nextEpisode)
            val urls = candidates.map { it.url }
            if (urls.isEmpty()) return@launch

            val nextTracks = try {
                val entries = SubtitleFetcher.getAcceptedSubtitles(
                    "series", imdb, seasonNumber, nextEpisode
                )
                SubtitleFetcher.toTracks(entries, 6)
            } catch (_: Exception) {
                emptyList()
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@PlayerActivity,
                    "Sledeća epizoda: S$seasonNumber · E$nextEpisode",
                    Toast.LENGTH_SHORT
                ).show()
                episodeNumber = nextEpisode
                currentSubtitleTracks.clear()
                currentSubtitleTracks.addAll(nextTracks)
                candidateUrls.clear()
                candidateUrls.addAll(urls)
                playCandidate(0)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val p = player ?: return
        val url = currentUrl ?: return

        try {
            val position = p.currentPosition
            val duration = p.duration
            val prefs = getSharedPreferences("player_positions", MODE_PRIVATE)
            when {
                duration > 0 && position > duration - 15_000 -> prefs.edit().remove(url).apply()
                position > 5_000 -> prefs.edit().putLong(url, position).apply()
            }
        } catch (_: Exception) {
            // Ignoriši
        }

        p.release()
        player = null
    }
}

// =====================================================================
// APPLE TV+ STIL UI
// =====================================================================

@Composable
private fun AppleTvPlayerScreen(activity: PlayerActivity) {
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var subsEnabled by remember { mutableStateOf(true) }

    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Polling player stanja (4x u sekundi)
    LaunchedEffect(Unit) {
        while (true) {
            val p = activity.player
            if (p != null) {
                position = p.currentPosition
                duration = p.duration.coerceAtLeast(0)
                isPlaying = p.isPlaying
                isBuffering = p.playbackState == Player.STATE_BUFFERING
                if (!isDragging && duration > 0) {
                    sliderValue = position.toFloat()
                }
            }
            delay(250)
        }
    }

    // Auto-hide kontrole posle 4s mirovanja
    LaunchedEffect(lastInteraction) {
        controlsVisible = true
        delay(4000)
        controlsVisible = false
    }

    fun interact() {
        lastInteraction = System.currentTimeMillis()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // VIDEO (crn letterbox — bez sivih traka)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    activity.playerView = this
                    player = activity.player
                }
            },
            update = { pv -> pv.player = activity.player },
            modifier = Modifier.fillMaxSize()
        )

        // TAP na video = prikaži/sakrij kontrole
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    interact()
                    controlsVisible = !controlsVisible
                }
        )

        // BUFFERING spinner
        if (isBuffering) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
            )
        }

        // KONTROLE (Apple TV+ stil)
        if (controlsVisible) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Gornji gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                            )
                        )
                )

                // Donji gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )

                // Gore levo: nazad + naslov
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 28.dp, top = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TvFocusableButton(onClick = { activity.finish() }) { focused ->
                        val scale by animateFloatAsState(
                            if (focused) 1.1f else 1f,
                            androidx.compose.animation.core.tween(150),
                            label = ""
                        )
                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (focused) 0.25f else 0.12f))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Nazad",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    if (activity.playerTitle.isNotBlank()) {
                        Text(
                            text = activity.playerTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                // Sredina: -10s / play-pause / +10s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(44.dp)
                ) {
                    // -10s
                    TvFocusableButton(onClick = {
                        interact()
                        val p = activity.player ?: return@TvFocusableButton
                        p.seekTo((p.currentPosition - 10_000).coerceAtLeast(0))
                    }) { focused ->
                        val scale by animateFloatAsState(
                            if (focused) 1.1f else 1f,
                            androidx.compose.animation.core.tween(150),
                            label = ""
                        )
                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (focused) 0.25f else 0.12f))
                                .size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Rewind,
                                contentDescription = "-10s",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // PLAY / PAUSE (veliko okruglo)
                    TvFocusableButton(onClick = {
                        interact()
                        val p = activity.player ?: return@TvFocusableButton
                        if (p.isPlaying) p.pause() else p.play()
                    }) { focused ->
                        val scale by animateFloatAsState(
                            if (focused) 1.12f else 1f,
                            androidx.compose.animation.core.tween(150),
                            label = ""
                        )
                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (focused) 0.35f else 0.2f))
                                .size(84.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // +10s
                    TvFocusableButton(onClick = {
                        interact()
                        val p = activity.player ?: return@TvFocusableButton
                        p.seekTo((p.currentPosition + 10_000).coerceAtMost(p.duration.coerceAtLeast(0)))
                    }) { focused ->
                        val scale by animateFloatAsState(
                            if (focused) 1.1f else 1f,
                            androidx.compose.animation.core.tween(150),
                            label = ""
                        )
                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (focused) 0.25f else 0.12f))
                                .size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "+10s",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Dole: progress + vremena + CC
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 40.dp, end = 40.dp, bottom = 28.dp)
                ) {
                    Slider(
                        value = sliderValue.coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
                        onValueChange = {
                            interact()
                            isDragging = true
                            sliderValue = it
                        },
                        onValueChangeFinished = {
                            isDragging = false
                            activity.player?.seekTo(sliderValue.toLong())
                        },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(if (isDragging) sliderValue.toLong() else position),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // CC (titlovi on/off)
                            TvFocusableButton(onClick = {
                                interact()
                                val p = activity.player ?: return@TvFocusableButton
                                subsEnabled = !subsEnabled
                                p.trackSelectionParameters = p.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subsEnabled)
                                    .build()
                            }) { focused ->
                                val scale by animateFloatAsState(
                                    if (focused) 1.1f else 1f,
                                    androidx.compose.animation.core.tween(150),
                                    label = ""
                                )
                                Box(
                                    modifier = Modifier
                                        .scale(scale)
                                        .clip(CircleShape)
                                        .background(
                                            Color.White.copy(
                                                alpha = when {
                                                    focused -> 0.25f
                                                    subsEnabled -> 0.15f
                                                    else -> 0.06f
                                                }
                                            )
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "CC",
                                        color = if (subsEnabled) Color.White
                                        else Color.White.copy(alpha = 0.4f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = formatTime(duration),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}
