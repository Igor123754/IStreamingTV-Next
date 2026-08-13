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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * MOĆNI PLEJER — Media3 ExoPlayer (720p → 4K REMUX), optimizovan za 2GB RAM.
 * TITLOVI: automatski SRPSKI (jedini jezik), sinhronizovani najbolji match.
 * AUTO-FALLBACK izvora + automatska SLEDEĆA EPIZODA.
 */
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null

    private val candidateUrls = mutableListOf<String>()
    private var candidateIndex = 0
    private var currentUrl: String? = null
    private var currentSubtitleUrl: String? = null

    private var imdbId: String? = null
    private var seasonNumber: Int = -1
    private var episodeNumber: Int = -1

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
        currentSubtitleUrl = intent.getStringExtra("subtitle_url")

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

        // Ako titl nije stigao sa detalja → brzi fallback fetch (max 2s), pa start
        lifecycleScope.launch {
            if (currentSubtitleUrl == null && !imdbId.isNullOrBlank()) {
                val type = if (seasonNumber >= 0) "series" else "movie"
                currentSubtitleUrl = withTimeoutOrNull(2000) {
                    withContext(Dispatchers.IO) {
                        try {
                            SubtitleFetcher.getBestSerbianSubtitle(
                                type, imdbId!!, seasonNumber, episodeNumber
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
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
        buildPlayer(currentUrl!!, currentSubtitleUrl)
    }

    private fun buildPlayer(url: String, subtitleUrl: String?) {
        val context = this

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        // AUTOMATSKI SRPSKI TITL — jedini jezik koji se bira
        val trackSelector = DefaultTrackSelector(context)
        trackSelector.parameters = DefaultTrackSelector.Parameters.Builder(context)
            .setPreferredTextLanguage("sr")
            .build()

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

        // MediaItem + srpski titl (ako postoji)
        val mediaItemBuilder = MediaItem.Builder().setUri(url)
        if (!subtitleUrl.isNullOrBlank()) {
            val mime = when {
                subtitleUrl.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                subtitleUrl.endsWith(".ass", ignoreCase = true) ||
                    subtitleUrl.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
                else -> MimeTypes.APPLICATION_SUBRIP
            }
            mediaItemBuilder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                        .setMimeType(mime)
                        .setLanguage("sr")
                        .setLabel("Srpski")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            )
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

    /** Kraj epizode → sledeća (E+1) + njen srpski titl */
    private fun tryNextEpisode() {
        val imdb = imdbId ?: return
        if (seasonNumber < 0 || episodeNumber < 0) return
        val nextEpisode = episodeNumber + 1

        lifecycleScope.launch(Dispatchers.IO) {
            val candidates = StreamPicker.getCandidates("series", imdb, seasonNumber, nextEpisode)
            val urls = candidates.map { it.url }
            if (urls.isEmpty()) return@launch

            val nextSub = try {
                SubtitleFetcher.getBestSerbianSubtitle("series", imdb, seasonNumber, nextEpisode)
            } catch (_: Exception) {
                null
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@PlayerActivity,
                    "Sledeća epizoda: S$seasonNumber · E$nextEpisode",
                    Toast.LENGTH_SHORT
                ).show()
                episodeNumber = nextEpisode
                currentSubtitleUrl = nextSub
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
