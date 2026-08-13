package com.igor.istreamingtv.ui.player

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * MOĆNI PLEJER — Media3 ExoPlayer, optimizovan za 2GB RAM.
 * TITLOVI: SRPSKI default (sinhronizovan), HRVATSKI kao fallback —
 *          jezik putuje UZ traku, player ne nagađa.
 * AUDIO: engleski/original (JEDAN poziv setPreferredAudioLanguage).
 * AUTO-FALLBACK izvora + automatska SLEDEĆA EPIZODA.
 */
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
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

        val view = PlayerView(this).apply {
            controllerShowTimeoutMs = 4000
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            setShowSubtitleButton(true)
            setKeepContentOnPlayerReset(true)
        }
        playerView = view
        setContentView(view)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, view).apply {
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

        // NOVI format: trake sa jezikom {url, lang}
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
        // Backward kompatibilnost sa starim formatima
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

        // TRACK SELECTOR — JEDAN poziv po preferenci (setter prepisuje vrednost!):
        //  - AUDIO: engleski (nikad Hindi/regionalni)
        //  - TEXT:  jezik NAJBOLJE trake (sr ako postoji sr, inače hr)
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

        // Titlovi: tačne labele "Srpski" / "Hrvatski" (+ broj za duplikate)
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

        val mediaItem = mediaItemBuilder.build()
        exoPlayer.setMediaItem(mediaItem)
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

    /** Kraj epizode → sledeća (E+1) + njeni titlovi (sr > hr) */
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
