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
import java.util.concurrent.TimeUnit

/**
 * MOĆNI PLEJER — Media3 ExoPlayer podešen za sve:
 * od lakih 720p stream-ova do 4K REMUX (MKV/HEVC visokog bitrate-a).
 *
 * Podržava: MP4, MKV (Matroska), HLS (.m3u8), DASH (.mpd), SmoothStreaming,
 * WebM, HEVC/H.265, VP9, AV1 (zavisno od HW dekodera uređaja).
 */
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var currentUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ne dozvoli gašenje ekrana tokom gledanja
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val info = extractStreamInfo()
        if (info == null) {
            Toast.makeText(this, "Nema izvora za reprodukciju", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentUrl = info.first

        // UI: PlayerView programski (bez XML layout-a)
        val view = PlayerView(this).apply {
            controllerShowTimeoutMs = 4000
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            setShowSubtitleButton(true)
            setKeepContentOnPlayerReset(true)
        }
        playerView = view
        setContentView(view)

        // Imersivni fullscreen (sakrij sistemske trake)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, view).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        buildPlayer(url = info.first)
    }

    /**
     * Prihvata stream na više načina (url extra, Gson "stream" JSON, data uri)
     * da ostane kompatibilan sa postojećim pozivima iz aplikacije.
     */
    private fun extractStreamInfo(): Pair<String, String?>? {
        // 1) Direktan URL
        intent.getStringExtra("url")?.takeIf { it.isNotBlank() }?.let {
            return it to intent.getStringExtra("title")
        }

        // 2) Gson JSON stream objekat (Stremio format: url / externalUrl)
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
                // Ako JSON nije validan, možda je sam string URL
                if (json.startsWith("http")) return json to null
            }
        }

        // 3) data uri
        intent.data?.toString()?.let { return it to null }

        return null
    }

    private fun buildPlayer(url: String) {
        val context = this

        // 1) Rendereri: HW dekoder sa automatskim fallback-om na SW
        //    (ključno za HEVC/AV1 kontejnere u MKV remux-ovima)
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        // 2) Track selector: bira NAJKVALITETNIJI video/audio koji uređaj podržava
        val trackSelector = DefaultTrackSelector(context)

        // 3) Buffer podešen za 4K REMUX (visoki bitrate-i):
        //    veliki read-ahead + povećan memorijski budžet
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 20_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 2_000,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .setTargetBufferBytes(96 * 1024 * 1024) // 96 MB — remux-ovi gutaju mnogo
            .setBackBuffer(30_000, false)           // 30s unazad za premotavanje
            .build()

        // 4) Mreža: OkHttp — bolje barata ogromnim stream-ovima,
        //    redirect-ima i custom header-ima
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

        // 5) Sklapanje plejera
        val exoPlayer = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        exoPlayer.setHandleAudioBecomingNoisy(true)

        // 6) Mini-resume: vrati se gde je korisnik stao (osnova za "Nastavi gledanje")
        val prefs = getSharedPreferences("player_positions", MODE_PRIVATE)
        val savedPosition = prefs.getLong(url, -1L)

        val mediaItem = MediaItem.Builder().setUri(url).build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (savedPosition > 0) {
            exoPlayer.seekTo(savedPosition)
        }
        exoPlayer.playWhenReady = true

        // 7) Greške: prikaži poruku umesto tihog kreša
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(
                    context,
                    "Greška reprodukcije: ${error.errorCodeName}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        player = exoPlayer
        playerView?.player = exoPlayer
    }

    override fun onStop() {
        super.onStop()
        val p = player ?: return
        val url = currentUrl ?: return

        // Sačuvaj poziciju (ako nije odgledano do kraja)
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
