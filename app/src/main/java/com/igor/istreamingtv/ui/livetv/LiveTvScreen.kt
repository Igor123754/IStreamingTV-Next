package com.igor.istreamingtv.ui.livetv

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.igor.istreamingtv.data.livetv.EpgProgram
import com.igor.istreamingtv.data.livetv.LiveChannel
import com.igor.istreamingtv.data.livetv.LiveTvSession
import com.igor.istreamingtv.data.livetv.epgListFor
import com.igor.istreamingtv.data.remote.TmdbClient
import com.igor.istreamingtv.ui.components.LiveIcon
import com.igor.istreamingtv.ui.components.NavBarDrawer
import com.igor.istreamingtv.ui.components.NavDestination
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.home.HomeViewModel
import com.igor.istreamingtv.ui.player.PlayerActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ✅ Apple TV+ paleta
private val LiveBg = Color(0xFF05070B)
private val CardBg = Color(0xFF151A21)
private val CardBorder = Color.White.copy(alpha = 0.06f)

private fun nowProgram(epg: Map<String, List<EpgProgram>>, ch: LiveChannel): EpgProgram? {
    val now = System.currentTimeMillis()
    return epgListFor(epg, ch)?.firstOrNull { now >= it.startMs && now < it.endMs }
}

private fun fmtTime(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

private fun remainingMin(endMs: Long, nowMs: Long): Long =
    ((endMs - nowMs) / 60_000L).coerceAtLeast(0)

/**
 * ✅ UŽIVO TV — Apple TV+ stil: hero sa LIVE PREVIEW +
 *    KATALOZI PO GRUPAMA IZ M3U (kao filmski redovi na početnoj).
 */
@Composable
fun LiveTvScreen(
    onOpenHome: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val homeViewModel: HomeViewModel = viewModel()
    val state by homeViewModel.uiState.collectAsState()
    val channels = state.liveChannels
    val epg = state.liveEpg

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var navOpen by remember { mutableStateOf(false) }
    val pillFocus = remember { FocusRequester() }
    val closeNav: () -> Unit = {
        navOpen = false
        scope.launch { try { pillFocus.requestFocus() } catch (_: Exception) {} }
    }

    // ✅ Grupe iz M3U (redosled kao u listi)
    val groups = remember(channels) {
        channels.groupBy { ch -> ch.group?.takeIf { it.isNotBlank() } ?: "Ostali kanali" }
    }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(30_000)
        }
    }

    var focusedChannel by remember { mutableStateOf<LiveChannel?>(null) }
    LaunchedEffect(channels) {
        if (focusedChannel == null) focusedChannel = channels.firstOrNull()
    }

    val heroChannel = focusedChannel
    val heroProgram = heroChannel?.let { nowProgram(epg, it) }

    // =====================================================================
    // ✅ LIVE PREVIEW (muted, fallback slika ispod)
    // =====================================================================
    val previewPlayer = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(3_000, 12_000, 1_000, 2_000)
                    .setTargetBufferBytes(6 * 1024 * 1024)
                    .build()
            )
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context).setDataSourceFactory(
                    OkHttpDataSource.Factory(
                        OkHttpClient.Builder()
                            .connectTimeout(6, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .followRedirects(true)
                            .build()
                    )
                )
            )
            .build()
            .apply { volume = 0f }
    }

    var previewBuffering by remember { mutableStateOf(true) }
    var previewReady by remember { mutableStateOf(false) }
    var previewError by remember { mutableStateOf(false) }

    DisposableEffect(previewPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(s: Int) {
                previewBuffering = s == Player.STATE_BUFFERING
                if (s == Player.STATE_READY) previewReady = true
            }
            override fun onPlayerError(e: PlaybackException) {
                previewError = true
                previewBuffering = false
                previewReady = false
            }
        }
        previewPlayer.addListener(listener)
        onDispose {
            previewPlayer.removeListener(listener)
            previewPlayer.release()
        }
    }

    LaunchedEffect(heroChannel?.id) {
        val ch = heroChannel ?: return@LaunchedEffect
        delay(800)
        previewError = false
        previewReady = false
        previewBuffering = true
        previewPlayer.stop()
        previewPlayer.setMediaItem(MediaItem.fromUri(ch.streamUrl))
        previewPlayer.prepare()
        previewPlayer.playWhenReady = true
    }

    val onWatch: (LiveChannel, EpgProgram?) -> Unit = { channel, program ->
        LiveTvSession.channels = channels
        LiveTvSession.epg = epg
        LiveTvSession.currentIndex = channels.indexOf(channel).coerceAtLeast(0)
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra("candidates", TmdbClient.json.encodeToString(listOf(channel.streamUrl)))
            putExtra("live", true)
            putExtra("title", channel.name)
            putExtra("poster", channel.logoUrl ?: "")
            putExtra("live_program", program?.title ?: "")
            putExtra("live_start", program?.startMs ?: 0L)
            putExtra("live_end", program?.endMs ?: 0L)
        }
        context.startActivity(intent)
    }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val clockText = remember(nowMs) { fmtTime(nowMs) }
    val firstCardFocus = remember { FocusRequester() }
    var firstFocusUsed by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(LiveBg)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ✅ HERO
            item(key = "hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((screenHeightDp * 0.55f).dp)
                ) {
                    // Pozadina: fallback slika + gradient
                    val bgUrl = heroProgram?.iconUrl ?: heroChannel?.logoUrl
                    if (!bgUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(bgUrl).crossfade(false)
                                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.35f
                        )
                    }
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, LiveBg)
                            )
                        )
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                listOf(LiveBg.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                    )

                    // Pill + sat
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .padding(start = 48.dp, end = 48.dp, top = 28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LivePill(pillFocus = pillFocus, onOpenNav = { navOpen = true })
                        Spacer(modifier = Modifier.weight(1f))
                        Text(clockText, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    // Info levo + preview desno
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 48.dp, end = 48.dp, top = 80.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.05f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE50914))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("UŽIVO", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                heroProgram?.category?.let { cat ->
                                    Text(cat, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                heroProgram?.title ?: heroChannel?.name ?: "Uživo TV",
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (heroChannel != null && heroProgram != null) {
                                val rem = remainingMin(heroProgram.endMs, nowMs)
                                Text(
                                    "${heroChannel.name} · ${fmtTime(heroProgram.startMs)} – ${fmtTime(heroProgram.endMs)} · " +
                                        if (rem > 0) "$rem min preostalo" else "kraj uskoro",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            heroProgram?.description?.let { desc ->
                                Text(
                                    desc,
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (heroProgram != null && heroProgram.endMs > heroProgram.startMs) {
                                val prog = ((nowMs - heroProgram.startMs).toFloat() /
                                    (heroProgram.endMs - heroProgram.startMs)).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(prog)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.White)
                                    )
                                }
                            }
                        }

                        // — LIVE PREVIEW
                        Box(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .background(CardBg)
                                .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                        ) {
                            val fallback = heroProgram?.iconUrl ?: heroChannel?.logoUrl
                            if (!fallback.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(fallback).crossfade(false)
                                        .bitmapConfig(android.graphics.Bitmap.Config.RGB_565).build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            if (!previewError) {
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            useController = false
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            player = previewPlayer
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                                        )
                                    )
                            )

                            if (!heroChannel?.logoUrl.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(6.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(heroChannel!!.logoUrl!!).crossfade(false)
                                            .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888).build(),
                                        contentDescription = null,
                                        modifier = Modifier.size(height = 30.dp, width = 56.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            if (previewBuffering && !previewError && !previewReady) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }

            // ✅ KATALOZI PO GRUPAMA IZ M3U (kao filmski redovi)
            groups.forEach { (group, groupChannels) ->
                item(key = "group_$group") {
                    Column(modifier = Modifier.padding(top = 36.dp)) {
                        Text(
                            group,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 48.dp, bottom = 16.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(start = 48.dp, end = 48.dp)
                        ) {
                            itemsIndexed(groupChannels, key = { _, ch -> ch.id }) { index, ch ->
                                val needFirstFocus = !firstFocusUsed &&
                                    groups.keys.firstOrNull() == group && index == 0
                                LiveChannelCard(
                                    channel = ch,
                                    program = nowProgram(epg, ch),
                                    nowMs = nowMs,
                                    initialFocus = if (needFirstFocus) firstCardFocus else null,
                                    onFirstFocused = { firstFocusUsed = true },
                                    onFocus = { focusedChannel = ch },
                                    onWatch = onWatch
                                )
                            }
                        }
                    }
                }
            }

            item(key = "bottom-spacer") { Spacer(modifier = Modifier.height(60.dp)) }
        }

        LaunchedEffect(channels.isNotEmpty()) {
            if (channels.isNotEmpty()) {
                delay(500)
                try { firstCardFocus.requestFocus() } catch (_: Exception) {}
            }
        }

        // ✅ Navigaciona traka
        NavBarDrawer(
            open = navOpen,
            current = NavDestination.LIVE,
            onDismiss = closeNav,
            onNavigate = { dest ->
                closeNav()
                when (dest) {
                    NavDestination.HOME -> onOpenHome()
                    NavDestination.SEARCH -> onOpenSearch()
                    else -> {}
                }
            }
        )
    }
}

/** ✅ Pill "Uživo TV" */
@Composable
private fun LivePill(
    pillFocus: FocusRequester,
    onOpenNav: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(180), label = "")

    Row(
        modifier = Modifier
            .focusRequester(pillFocus)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onOpenNav)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF48484A).copy(alpha = if (focused) 0.95f else 0.75f))
            .then(if (focused) Modifier.border(2.dp, Color.White, RoundedCornerShape(22.dp)) else Modifier)
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            LiveIcon(Modifier.size(16.dp))
        }
        Text("Uživo TV", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** ✅ Kartica kanala (kao poster kartice na početnoj) */
@Composable
private fun LiveChannelCard(
    channel: LiveChannel,
    program: EpgProgram?,
    nowMs: Long,
    initialFocus: FocusRequester?,
    onFirstFocused: () -> Unit,
    onFocus: () -> Unit,
    onWatch: () -> Unit
) {
    val progress = if (program != null && program.endMs > program.startMs)
        ((nowMs - program.startMs).toFloat() / (program.endMs - program.startMs)).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.width(240.dp)) {
        TvFocusableButton(
            onClick = onWatch,
            modifier = Modifier
                .width(240.dp)
                .height(135.dp)
                .then(if (initialFocus != null) Modifier.focusRequester(initialFocus) else Modifier)
                .onFocusChanged {
                    if (it.hasFocus) {
                        onFocus()
                        if (initialFocus != null) onFirstFocused()
                    }
                }
        ) { f ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(if (f) 1.06f else 1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .then(
                        if (f) Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp))
                        else Modifier.border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    )
            ) {
                // Logo kanala
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(channel.logoUrl).crossfade(false)
                            .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888).build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(channel.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // UŽIVO bedž
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE50914))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("UŽIVO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Donji gradient + trenutna emisija
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 12.dp)
                ) {
                    Text(
                        program?.title ?: channel.group ?: "Uživo",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (program != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            channel.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            program?.let { "${fmtTime(it.startMs)} – ${fmtTime(it.endMs)}" } ?: " ",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}
