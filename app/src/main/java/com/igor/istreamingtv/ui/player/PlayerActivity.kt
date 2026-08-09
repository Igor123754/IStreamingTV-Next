package com.igor.istreamingtv.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@UnstableApi
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Unknown"
        val headersJson = intent.getStringExtra(EXTRA_HEADERS)
        val headers: Map<String, String>? = headersJson?.let {
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(it, type)
        }

        player = ExoPlayer.Builder(this).build()

        val mediaItem = MediaItem.fromUri(url)
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("IStreamingTV/1.0")

        headers?.let { h ->
            if (h.isNotEmpty()) {
                dataSourceFactory.setDefaultRequestProperties(h)
            }
        }

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        player?.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }

        setContent {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = this@PlayerActivity.player
                        useController = true
                        controllerShowTimeoutMs = 5000
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_HEADERS = "headers"

        fun newIntent(
            context: Context,
            url: String,
            title: String = "",
            headers: Map<String, String>? = null
        ): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
                headers?.let {
                    putExtra(EXTRA_HEADERS, Gson().toJson(it))
                }
            }
        }
    }
}
