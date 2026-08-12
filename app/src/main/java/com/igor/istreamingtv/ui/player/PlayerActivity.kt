package com.igor.istreamingtv.ui.player

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * MOĆNI PLEJER — Media3 ExoPlayer (720p → 4K REMUX).
 * Za serije: kada se epizoda završi, automatski pušta SLEDEĆU.
 */
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var currentUrl: String? = null

    // Podaci za "sledeća epizoda" (samo kod serija)
    private var seriesImdb: String? = null
    private var seasonNumber: Int = -1
    private var episodeNumber: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val info = extractStreamInfo()
        if (info == null) {
            Toast.makeText(this, "Nema izvora za reprodukciju", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentUrl = info.first

        seriesImdb = intent.getStringExtra("series_imdb")
        seasonNumber = intent.getIntExtra("season", -1)
        episodeNumber = intent.getIntExtra("episode", -1)

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

        buildPlayer(url = info.first)
    }

    private fun extractStreamInfo(): Pair<String, String?>? {
        intent.getStringExtra("url")?.takeIf { it.isNotBlank() }?.let {
            return it to intent.getStringExtra("title")
        }

        intent.getStringExtra("stream")?.let { json ->
            try {
                val obj = JsonParser.parseString(json).asJsonObject
                val url = obj.get("url")?.takeIf { !it.isJsonNull }?.asString
                    ?: obj.get("externalUrl")?.takeIf { !it.isJsonNull }?.asString
                    ?: obj.get("streamUrl")?.takeIf { !it.isJsonNull }?.asString
                val title = obj.get("title")?.takeIf { !it.isJsonNull }?.asString
                    ?: obj.get("name")?.takeIf { !it.isJsonNull }?.asString
                if (!url.isNullOrBlank()) return url to title
            } catch (_: Exception) {
                if (json.startsWith("http")) return json to null
            }
        }

        intent.data?.toString()?.let { return it to null }
        return null
    }

    private fun buildPlayer(url: String) {
        val context = this

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val trackSelector = DefaultTrackSelector(context)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(20_000, 60_000, 2_000, 5_000)
            .setTargetBufferBytes(96 * 1024 * 1024)
            .setBackBuffer(30_000, false)
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(
                mapOf("User-Agent" to "IStreamingTV/1.0 (Android; Media3)")
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        val exoPlayer = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        exoPlayer.setHandleAudioBecomingNoisy(true)

        val prefs = getSharedPreferences("player_positions", MODE_PRIVATE)
        val savedPosition = prefs.getLong(url, -1L)

        val mediaItem = MediaItem.Builder().setUri(url).build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (savedPosition > 0) exoPlayer.seekTo(savedPosition)
        exoPlayer.playWhenReady = true

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(
                    context,
                    "Greška reprodukcije: ${error.errorCodeName}",
                    Toast.LENGTH_LONG
                ).show()
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

    /**
     * Kada se epizoda završi → povuci stream za sledeću (E+1) i pusti je.
     */
    private fun tryNextEpisode() {
        val imdb = seriesImdb ?: return
        if (seasonNumber < 0 || episodeNumber < 0) return
        val nextEpisode = episodeNumber + 1

        Thread {
            try {
                val url = "https://v3-cinemeta.strem.io/stream/series/$imdb:$seasonNumber:$nextEpisode.json"
                val client = OkHttpClient()
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                val body = response.body?.string()
                response.close()
                if (body.isNullOrBlank()) return@Thread

                val obj = JsonParser.parseString(body).asJsonObject
                val first = obj.getAsJsonArray("streams")?.firstOrNull()?.asJsonObject
                val nextUrl = first?.get("url")?.takeIf { !it.isJsonNull }?.asString

                if (!nextUrl.isNullOrBlank()) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Sledeća epizoda: S${seasonNumber} · E${nextEpisode}",
                            Toast.LENGTH_SHORT
                        ).show()
                        episodeNumber = nextEpisode
                        currentUrl = nextUrl
                        val item = MediaItem.Builder().setUri(nextUrl).build()
                        player?.setMediaItem(item)
                        player?.prepare()
                        player?.playWhenReady = true
                    }
                }
            } catch (_: Exception) {
                // Nema sledeće epizode — ostani na kraju
            }
        }.start()
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
