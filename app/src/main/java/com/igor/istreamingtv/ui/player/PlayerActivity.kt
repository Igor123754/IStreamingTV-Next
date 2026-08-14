package com.igor.istreamingtv.ui.player

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
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
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.igor.istreamingtv.data.ContinueEntry
import com.igor.istreamingtv.data.ContinueWatchingStore
import com.igor.istreamingtv.data.remote.StreamPicker
import com.igor.istreamingtv.data.remote.SubtitleFetcher
import com.igor.istreamingtv.data.remote.SubtitleTrack
import com.igor.istreamingtv.data.remote.TmdbClient
import com.igor.istreamingtv.ui.components.TvFocusableButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

internal enum class TrackMenuKind { NONE, SUBTITLES, AUDIO }

class PlayerActivity : ComponentActivity() {

    internal var player: ExoPlayer? = null
    internal var playerView: PlayerView? = null
    internal var playerTitle: String = ""
    internal var playerSeason: Int = -1
    internal var playerEpisode: Int = -1
    internal var playerPoster: String = ""
    internal var playerBackdrop: String = ""
    internal var playerOverview: String = ""

    internal var isSeriesPlay: Boolean = false
    internal var introStartMs: Long = -1
    internal var introEndMs: Long = -1

    internal var menuKind by mutableStateOf(TrackMenuKind.NONE)

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
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))

        parseIntent()
        if (candidateUrls.isEmpty()) {
            Toast.makeText(this, "Nema izvora za reprodukciju", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        imdbId = intent.getStringExtra("imdb_id") ?: intent.getStringExtra("series_imdb")
        seasonNumber = intent.getIntExtra("season", -1)
        episodeNumber = intent.getIntExtra("episode", -1)
        runtimeSec = intent.getIntExtra("runtime_sec", -1)
        playerTitle = intent.getStringExtra("title") ?: ""
        playerSeason = seasonNumber
        playerEpisode = episodeNumber
        playerPoster = intent.getStringExtra("poster") ?: ""
        playerBackdrop = intent.getStringExtra("backdrop") ?: ""
        playerOverview = intent.getStringExtra("overview") ?: ""

        if (!imdbId.isNullOrBlank() && seasonNumber >= 0 && episodeNumber >= 0) {
            isSeriesPlay = true
            lifecycleScope.launch {
                val intro = fetchIntroTimestamps(imdbId!!, seasonNumber, episodeNumber)
                if (intro != null) {
                    introStartMs = (intro.first * 1000).toLong()
                    introEndMs = (intro.second * 1000).toLong()
                }
            }
        }

        val composeView = ComposeView(this)
        composeView.setBackgroundColor(android.graphics.Color.BLACK)
        composeView.setContent { AppleTvPlayerScreen(activity = this@PlayerActivity) }
        setContentView(composeView)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, composeView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        lifecycleScope.launch {
            if (currentSubtitleTracks.isEmpty() && !imdbId.isNullOrBlank()) {
                val type = if (seasonNumber >= 0) "series" else "movie"
                val fetched = withTimeoutOrNull(2500) {
                    withContext(Dispatchers.IO) {
                        try {
                            val entries = SubtitleFetcher.getAcceptedSubtitles(type, imdbId!!, seasonNumber, episodeNumber)
                            val ranked = SubtitleFetcher.rankBySync(entries, if (runtimeSec > 0) runtimeSec else null)
                            SubtitleFetcher.toTracks(ranked, 6)
                        } catch (_: Exception) { emptyList() }
                    }
                }
                if (!fetched.isNullOrEmpty()) {
                    currentSubtitleTracks.clear()
                    currentSubtitleTracks.addAll(fetched)
                }
            }
            withContext(Dispatchers.Main) { playCandidate(0) }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if ((event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) &&
            event.action == KeyEvent.ACTION_DOWN
        ) {
            if (menuKind != TrackMenuKind.NONE) {
                menuKind = TrackMenuKind.NONE
                return true
            }
            finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE -> { togglePlayInternal(); return true }
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (menuKind == TrackMenuKind.NONE) { togglePlayInternal(); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    internal fun togglePlayInternal() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    @Serializable private data class IntroResponse(val start: Double = 0.0, val end: Double = 0.0)
    @Serializable private data class StreamObject(val url: String? = null, val externalUrl: String? = null)

    private suspend fun fetchIntroTimestamps(imdbId: String, season: Int, episode: Int): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.introdb.app/intro?imdb=$imdbId&season=$season&episode=$episode"
                val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build()
                val response = client.newCall(Request.Builder().url(url).get().build()).execute()
                val body = response.body?.string()
                response.close()
                if (body.isNullOrBlank()) return@withContext null
                val json = TmdbClient.json.decodeFromString<IntroResponse>(body)
                if (json.start >= 0 && json.end > json.start) Pair(json.start, json.end) else null
            } catch (_: Exception) { null }
        }

    internal fun skipIntro() {
        val p = player ?: return
        if (introEndMs > 0) p.seekTo(introEndMs)
    }

    private fun parseIntent() {
        intent.getStringExtra("candidates")?.let { s ->
            try { candidateUrls.addAll(TmdbClient.json.decodeFromString<List<String>>(s)) } catch (_: Exception) {}
        }
        intent.getStringExtra("subtitle_tracks")?.let { s ->
            try { currentSubtitleTracks.addAll(TmdbClient.json.decodeFromString<List<SubtitleTrack>>(s)) } catch (_: Exception) {}
        }
        if (currentSubtitleTracks.isEmpty()) intent.getStringExtra("subtitle_urls")?.let { s ->
            try { TmdbClient.json.decodeFromString<List<String>>(s).forEach { currentSubtitleTracks.add(SubtitleTrack(it, "sr")) } } catch (_: Exception) {}
        }
        if (currentSubtitleTracks.isEmpty()) intent.getStringExtra("subtitle_url")?.let { currentSubtitleTracks.add(SubtitleTrack(it, "sr")) }

        if (candidateUrls.isEmpty()) intent.getStringExtra("url")?.takeIf { it.isNotBlank() }?.let { candidateUrls.add(it) }
        if (candidateUrls.isEmpty()) intent.getStringExtra("stream")?.let { s ->
            try {
                val o = TmdbClient.json.decodeFromString<StreamObject>(s)
                val u = o.url ?: o.externalUrl
                if (!u.isNullOrBlank()) candidateUrls.add(u)
            } catch (_: Exception) { if (s.startsWith("http")) candidateUrls.add(s) }
        }
        intent.data?.toString()?.let { if (candidateUrls.isEmpty()) candidateUrls.add(it) }
    }

    private fun playCandidate(index: Int) {
        candidateIndex = index
        currentUrl = candidateUrls[index]
        buildPlayer(currentUrl!!, currentSubtitleTracks.toList())
    }

    private fun autoPickSyncedSubtitle(exoPlayer: ExoPlayer, tracks: List<SubtitleTrack>) {
        val actualMs = exoPlayer.duration
        if (actualMs <= 0) return
        val measured = tracks.mapIndexedNotNull { i, t -> t.durationSec?.let { Triple(i, it, abs(it * 1000L - actualMs)) } }
        if (measured.isEmpty()) return
        val best = measured.minByOrNull { it.third } ?: return
        if (best.first != 0 && best.third < 3 * 60 * 1000L) applySubtitleByIndex(exoPlayer, best.first)
    }

    private fun applySubtitleByIndex(exoPlayer: ExoPlayer, index: Int) {
        exoPlayer.currentTracks.groups.forEach { g ->
            if (g.type == C.TRACK_TYPE_TEXT && index < g.length) {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .addOverride(TrackSelectionOverride(g.mediaTrackGroup, listOf(index)))
                    .build()
            }
        }
    }

    private fun buildPlayer(url: String, tracks: List<SubtitleTrack>) {
        val context = this
        val trackSelector = DefaultTrackSelector(context)
        val pb = DefaultTrackSelector.Parameters.Builder(context).setPreferredAudioLanguage("en")
        if (tracks.isNotEmpty()) pb.setPreferredTextLanguage(tracks.first().lang)
        trackSelector.parameters = pb.build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(8_000, 30_000, 2_000, 5_000)
            .setTargetBufferBytes(24 * 1024 * 1024)
            .setBackBuffer(15_000, false)
            .build()

        val okHttp = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true).followSslRedirects(true).retryOnConnectionFailure(true).build()

        val dsFactory = OkHttpDataSource.Factory(okHttp)
            .setDefaultRequestProperties(mapOf("User-Agent" to "IStreamingTV/1.0 (Android; Media3)"))

        player?.release()

        val exo = ExoPlayer.Builder(context)
            .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dsFactory))
            .build()

        exo.setHandleAudioBecomingNoisy(true)

        val saved = getSharedPreferences("player_positions", MODE_PRIVATE).getLong(url, -1L)

        val mib = MediaItem.Builder().setUri(url)
        if (tracks.isNotEmpty()) {
            var sr = 0; var hr = 0
            mib.setSubtitleConfigurations(tracks.mapIndexed { i, t ->
                val isHr = t.lang == "hr"
                val label = if (isHr) { hr++; if (hr == 1) "Hrvatski" else "Hrvatski $hr" }
                else { sr++; if (sr == 1) "Srpski" else "Srpski $sr" }
                val mime = when {
                    t.url.endsWith(".vtt", true) -> MimeTypes.TEXT_VTT
                    t.url.endsWith(".ass", true) || t.url.endsWith(".ssa", true) -> MimeTypes.TEXT_SSA
                    else -> MimeTypes.APPLICATION_SUBRIP
                }
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(t.url)).setMimeType(mime)
                    .setLanguage(if (isHr) "hr" else "sr").setLabel(label)
                    .setSelectionFlags(if (i == 0) C.SELECTION_FLAG_DEFAULT else 0).build()
            })
        }

        exo.setMediaItem(mib.build())
        exo.prepare()
        if (saved > 0) exo.seekTo(saved)
        exo.playWhenReady = true

        var autoSync = false
        exo.addListener(object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                if (candidateIndex < candidateUrls.size - 1) {
                    Toast.makeText(context, "Izvor ne radi — prelazim na sledeći...", Toast.LENGTH_SHORT).show()
                    playCandidate(candidateIndex + 1)
                } else Toast.makeText(context, "Greška reprodukcije: ${e.errorCodeName}", Toast.LENGTH_LONG).show()
            }
            override fun onPlaybackStateChanged(s: Int) {
                if (s == Player.STATE_ENDED) tryNextEpisode()
                if (s == Player.STATE_READY && !autoSync && tracks.size > 1) { autoSync = true; autoPickSyncedSubtitle(exo, tracks) }
            }
        })

        player = exo
        playerView?.player = exo
    }

    private fun tryNextEpisode() {
        val imdb = imdbId ?: return
        if (seasonNumber < 0 || episodeNumber < 0) return
        val next = episodeNumber + 1
        lifecycleScope.launch(Dispatchers.IO) {
            val candidates = StreamPicker.getCandidates("series", imdb, seasonNumber, next)
            val urls = candidates.map { it.url }
            if (urls.isEmpty()) return@launch
            val nextTracks = try {
                SubtitleFetcher.toTracks(SubtitleFetcher.getAcceptedSubtitles("series", imdb, seasonNumber, next), 6)
            } catch (_: Exception) { emptyList() }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@PlayerActivity, "Sledeća epizoda: S$seasonNumber · E$next", Toast.LENGTH_SHORT).show()
                episodeNumber = next; playerSeason = seasonNumber; playerEpisode = next
                currentSubtitleTracks.clear(); currentSubtitleTracks.addAll(nextTracks)
                candidateUrls.clear(); candidateUrls.addAll(urls)
                if (seasonNumber >= 0) {
                    val intro = fetchIntroTimestamps(imdb, seasonNumber, next)
                    introStartMs = intro?.first?.times(1000)?.toLong() ?: -1
                    introEndMs = intro?.second?.times(1000)?.toLong() ?: -1
                }
                playCandidate(0)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val p = player ?: return
        val url = currentUrl ?: return
        try {
            val pos = p.currentPosition; val dur = p.duration
            val prefs = getSharedPreferences("player_positions", MODE_PRIVATE)
            when {
                dur > 0 && pos > dur - 15_000 -> prefs.edit().remove(url).apply()
                pos > 5_000 -> prefs.edit().putLong(url, pos).apply()
            }
            if (!imdbId.isNullOrBlank() || playerTitle.isNotBlank()) {
                val key = if (seasonNumber >= 0) "${imdbId}_s${seasonNumber}_e${episodeNumber}" else imdbId ?: url
                ContinueWatchingStore.upsert(this, ContinueEntry(key, imdbId, playerTitle, playerPoster, playerBackdrop,
                    seasonNumber >= 0, seasonNumber, episodeNumber, pos, dur, System.currentTimeMillis()), pos, dur)
            }
        } catch (_: Exception) {}
        p.release(); player = null
    }

    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}

// =====================================================================
// APPLE TV+ STIL UI — DVE RAVNI (plejer + panel podešavanja)
// =====================================================================

private data class TrackOption(val label: String, val groupIndex: Int, val trackIndex: Int, val selected: Boolean, val type: Int)

private fun audioLanguageName(code: String?): String = when (code?.lowercase()) {
    "en", "eng" -> "Engleski"; "sr", "scc", "srp" -> "Srpski"; "hr", "hrv" -> "Hrvatski"
    "hi", "hin" -> "Hindi"; "de", "ger", "deu" -> "Nemački"; "fr", "fre", "fra" -> "Francuski"
    "es", "spa" -> "Španski"; null, "" -> "Original"; else -> code.uppercase()
}

private fun collectOptions(player: Player, type: Int): List<TrackOption> {
    val list = mutableListOf<TrackOption>()
    player.currentTracks.groups.forEachIndexed { gi, g ->
        if (g.type == type) for (i in 0 until g.length) {
            val f = g.getTrackFormat(i)
            list.add(TrackOption(f.label ?: audioLanguageName(f.language), gi, i, g.isTrackSelected(i), type))
        }
    }
    return list
}

private fun selectOption(player: Player, groups: List<Tracks.Group>, o: TrackOption) {
    val g = groups.getOrNull(o.groupIndex) ?: return
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(o.type, false).clearOverridesOfType(o.type)
        .addOverride(TrackSelectionOverride(g.mediaTrackGroup, listOf(o.trackIndex))).build()
}

private fun disableSubtitles(player: Player) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
}

private fun buildPanelData(player: Player?, type: Int): Pair<List<String>, Int> {
    val p = player ?: return emptyList<String>() to 0
    val labels = mutableListOf<String>()
    var sel = 0
    val disabled = p.trackSelectionParameters.disabledTrackTypes.contains(type)
    if (type == C.TRACK_TYPE_TEXT) { labels.add("Isključeno"); if (disabled) sel = 0 }
    var idx = if (type == C.TRACK_TYPE_TEXT) 1 else 0
    p.currentTracks.groups.forEach { g ->
        if (g.type == type) for (i in 0 until g.length) {
            val f = g.getTrackFormat(i)
            labels.add(f.label ?: audioLanguageName(f.language))
            if (!disabled && g.isTrackSelected(i)) sel = idx
            idx++
        }
    }
    return labels to sel
}

@Composable
private fun PauseBars(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(4.dp, 14.dp).background(Color.White))
        Box(Modifier.size(4.dp, 14.dp).background(Color.White))
    }
}

@Composable
private fun SubtitlesIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val s = w * 0.09f
        drawRoundRect(Color.White, androidx.compose.ui.geometry.Offset(s, s * 1.5f),
            androidx.compose.ui.geometry.Size(w - s * 2, h - s * 3),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s),
            style = androidx.compose.ui.graphics.drawscope.Stroke(s))
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.42f), androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.42f), strokeWidth = s)
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.62f), androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.62f), strokeWidth = s)
    }
}

@Composable
private fun AudioIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(Color.White, androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.35f), androidx.compose.ui.geometry.Size(w * 0.2f, h * 0.3f))
        drawPath(androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.35f, h * 0.35f); lineTo(w * 0.6f, h * 0.15f); lineTo(w * 0.6f, h * 0.85f); lineTo(w * 0.35f, h * 0.65f); close()
        }, Color.White)
        drawArc(Color.White, -60f, 120f, false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.2f),
            size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.6f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.09f))
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    options: List<String>,
    initialSelected: Int,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit
) {
    var selected by remember { mutableIntStateOf(initialSelected.coerceIn(0, (options.size - 1).coerceAtLeast(0))) }
    val panelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { panelFocus.requestFocus() } catch (_: Exception) {} }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .focusRequester(panelFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> { selected = (selected - 1).coerceAtLeast(0); true }
                    Key.DirectionDown -> { selected = (selected + 1).coerceAtMost(options.size - 1); true }
                    Key.DirectionCenter -> { onSelect(selected); true }
                    Key.Back -> { onClose(); true }
                    else -> false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
                .width(340.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1E).copy(alpha = 0.96f))
                .padding(24.dp)
        ) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            options.forEachIndexed { i, label ->
                val isSel = i == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.White, fontSize = 16.sp,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium)
                    if (isSel) Text("●", color = Color.White, fontSize = 14.sp)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun AppleSeekBar(
    fraction: Float,
    onFractionSeek: (Float) -> Unit,
    onStepSeek: (Long) -> Unit,
    onInteract: () -> Unit,
    onNavigateDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .height(28.dp)
            .onGloballyPositioned { widthPx = it.size.width.toFloat().coerceAtLeast(1f) }
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown) when (e.key) {
                    Key.DirectionLeft -> { onInteract(); onStepSeek(-10_000); true }
                    Key.DirectionRight -> { onInteract(); onStepSeek(10_000); true }
                    Key.DirectionDown -> { onNavigateDown(); true }
                    else -> false
                } else false
            }
            .pointerInput(Unit) { detectTapGestures { onInteract(); onFractionSeek((it.x / widthPx).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { c, _ -> onInteract(); onFractionSeek((c.position.x / widthPx).coerceIn(0f, 1f)) } },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.3f)))
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
        if (focused) {
            val wd = with(density) { widthPx.toDp() }
            Box(Modifier.size(14.dp).offset(x = (wd * fraction) - 7.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
private fun CircleControlButton(
    focusRequester: FocusRequester,
    onActivate: () -> Unit,
    onNavigateUp: () -> Unit,
    content: @Composable () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.12f else 1f, tween(150), label = "")
    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown) when (e.key) {
                    Key.DirectionCenter -> { onActivate(); true }
                    Key.DirectionUp -> { onNavigateUp(); true }
                    else -> false
                } else false
            }
            .clickable(onClick = onActivate)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (focused) 0.3f else 0.15f))
            .size(44.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun AppleTvPlayerScreen(activity: PlayerActivity) {
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showPausedInfo by remember { mutableStateOf(false) }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isReady by remember { mutableStateOf(false) }
    var sliderFraction by remember { mutableFloatStateOf(0f) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val seekFocus = remember { FocusRequester() }
    val ccFocus = remember { FocusRequester() }
    val audioFocus = remember { FocusRequester() }

    val menuKind = activity.menuKind
    val groups = activity.player?.currentTracks?.groups ?: emptyList()

    LaunchedEffect(Unit) {
        while (true) {
            val p = activity.player
            if (p != null) {
                position = p.currentPosition; duration = p.duration.coerceAtLeast(0)
                isPlaying = p.isPlaying
                isBuffering = p.playbackState == Player.STATE_BUFFERING
                isReady = p.playbackState == Player.STATE_READY
                if (duration > 0) sliderFraction = position.toFloat() / duration.toFloat()
            }
            delay(500)
        }
    }
    LaunchedEffect(isPlaying, isReady) {
        if (!isPlaying && isReady) { delay(5000); showPausedInfo = true } else showPausedInfo = false
    }
    LaunchedEffect(controlsVisible, showPausedInfo) {
        while (controlsVisible || showPausedInfo) { nowMillis = System.currentTimeMillis(); delay(1000) }
    }
    LaunchedEffect(lastInteraction, menuKind, showPausedInfo) {
        controlsVisible = true
        if (menuKind == TrackMenuKind.NONE && !showPausedInfo) { delay(4000); controlsVisible = false }
    }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible && menuKind == TrackMenuKind.NONE) {
            try { seekFocus.requestFocus() } catch (_: Exception) {}
        }
    }

    fun interact() { lastInteraction = System.currentTimeMillis() }

    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val clockText = timeFmt.format(Date(nowMillis))
    val endText = timeFmt.format(Date(nowMillis + (duration - position).coerceAtLeast(0)))

    val showSkipIntro = activity.isSeriesPlay && isPlaying &&
        activity.introStartMs >= 0 && activity.introEndMs > 0 &&
        position in activity.introStartMs..activity.introEndMs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { e ->
                val isDpad = e.key == Key.DirectionLeft || e.key == Key.DirectionRight ||
                    e.key == Key.DirectionUp || e.key == Key.DirectionDown
                if (isDpad && !controlsVisible && menuKind == TrackMenuKind.NONE && e.type == KeyEventType.KeyDown) {
                    interact(); true
                } else false
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    subtitleView?.apply {
                        setApplyEmbeddedStyles(false)
                        setStyle(CaptionStyleCompat(android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                            android.graphics.Color.argb(180, 0, 0, 0), null))
                        setFractionalTextSize(0.05f)
                    }
                    activity.playerView = this
                    player = activity.player
                }
            },
            update = { it.player = activity.player },
            modifier = Modifier.fillMaxSize()
        )

        Box(Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(
                onTap = { interact(); controlsVisible = !controlsVisible },
                onDoubleTap = { interact(); activity.togglePlayInternal() }
            )
        })

        // ✅ FIX: imenovani argumenti za CircularProgressIndicator
        if (isBuffering) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(56.dp).align(Alignment.Center)
            )
        }

        if (showSkipIntro) {
            TvFocusableButton(
                onClick = { interact(); activity.skipIntro() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 120.dp)
            ) { focused ->
                val s by animateFloatAsState(if (focused) 1.06f else 1f, tween(150), label = "")
                Row(Modifier.scale(s).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = if (focused) 0.9f else 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Preskoči uvod", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("▶▶", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (controlsVisible || showPausedInfo) {
            Column(Modifier.align(Alignment.TopEnd).padding(end = 28.dp, top = 28.dp), horizontalAlignment = Alignment.End) {
                Text(clockText, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("Kraj: $endText", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
            }
        }

        if (controlsVisible || showPausedInfo) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(260.dp).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)))))

                if (!isPlaying && !isBuffering && isReady)
                    PauseBars(Modifier.align(Alignment.BottomCenter).padding(bottom = 128.dp))

                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 40.dp, end = 40.dp, bottom = 24.dp)) {
                    if (showPausedInfo) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (activity.playerPoster.isNotBlank())
                                AsyncImage(activity.playerPoster, null, Modifier.width(90.dp).height(135.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f)) {
                                if (activity.playerTitle.isNotBlank()) {
                                    Text(activity.playerTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Spacer(Modifier.height(6.dp))
                                }
                                if (activity.playerOverview.isNotBlank())
                                    Text(activity.playerOverview, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 18.sp, maxLines = 3,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    }

                    if (activity.playerSeason >= 0) {
                        Text("S${activity.playerSeason}, E${activity.playerEpisode}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                    }
                    if (activity.playerTitle.isNotBlank()) {
                        Text(activity.playerTitle, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(Modifier.height(10.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        AppleSeekBar(
                            fraction = sliderFraction,
                            onFractionSeek = { f -> interact(); activity.player?.let { it.seekTo((f * duration).toLong()) } },
                            onStepSeek = { d -> activity.player?.let { it.seekTo((it.currentPosition + d).coerceIn(0, it.duration.coerceAtLeast(0))) } },
                            onInteract = { interact() },
                            onNavigateDown = { try { ccFocus.requestFocus() } catch (_: Exception) {} },
                            modifier = Modifier.weight(1f).focusRequester(seekFocus)
                        )

                        CircleControlButton(
                            focusRequester = ccFocus,
                            onActivate = { interact(); activity.menuKind = TrackMenuKind.SUBTITLES },
                            onNavigateUp = { try { seekFocus.requestFocus() } catch (_: Exception) {} }
                        ) { SubtitlesIcon(Modifier.size(22.dp)) }

                        CircleControlButton(
                            focusRequester = audioFocus,
                            onActivate = { interact(); activity.menuKind = TrackMenuKind.AUDIO },
                            onNavigateUp = { try { seekFocus.requestFocus() } catch (_: Exception) {} }
                        ) { AudioIcon(Modifier.size(22.dp)) }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(position), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                        Text("-" + formatTime((duration - position).coerceAtLeast(0)), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    }
                }
            }
        }

        if (menuKind == TrackMenuKind.SUBTITLES) {
            val (labels, sel) = buildPanelData(activity.player, C.TRACK_TYPE_TEXT)
            SettingsPanel(
                title = "Titlovi", options = labels, initialSelected = sel,
                onSelect = { i ->
                    val p = activity.player
                    if (p != null) {
                        if (i == 0) disableSubtitles(p)
                        else collectOptions(p, C.TRACK_TYPE_TEXT).getOrNull(i - 1)?.let { selectOption(p, groups, it) }
                    }
                    activity.menuKind = TrackMenuKind.NONE
                },
                onClose = { activity.menuKind = TrackMenuKind.NONE }
            )
        }
        if (menuKind == TrackMenuKind.AUDIO) {
            val (labels, sel) = buildPanelData(activity.player, C.TRACK_TYPE_AUDIO)
            SettingsPanel(
                title = "Audio", options = labels, initialSelected = sel,
                onSelect = { i ->
                    val p = activity.player
                    if (p != null) collectOptions(p, C.TRACK_TYPE_AUDIO).getOrNull(i)?.let { selectOption(p, groups, it) }
                    activity.menuKind = TrackMenuKind.NONE
                },
                onClose = { activity.menuKind = TrackMenuKind.NONE }
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val t = ms / 1000
    val h = t / 3600; val m = (t % 3600) / 60; val s = t % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}
