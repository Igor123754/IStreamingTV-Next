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
 * ✅ UŽIVO TV — Apple TV+ stil (poboljšano):
 *    pill + sat, hero info levo + LIVE PREVIEW desno (sa fallback slikom),
 *    grupe levo, GUSTA EPG traka desno (više kanala + više programa odjednom).
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

    // ✅ Sortiranje po grupama iz M3U
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    val groups = remember(channels) { channels.mapNotNull { it.group }.distinct() }
    val visibleChannels = remember(channels, selectedGroup) {
        if (selectedGroup == null) channels else channels.filter { it.group == selectedGroup }
    }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(30_000)
        }
    }

    var focusedChannel by remember { mutableStateOf<LiveChannel?>(null) }
    var focusedProgram by remember { mutableStateOf<EpgProgram?>(null) }
    LaunchedEffect(visibleChannels) {
        if (focusedChannel == null) focusedChannel = visibleChannels.firstOrNull()
    }

    val heroChannel = focusedChannel
    val heroProgram = focusedProgram ?: heroChannel?.let { nowProgram(epg, it) }

    // =====================================================================
    // ✅ LIVE PREVIEW (muted, fallback slika ispod — nikad prazan okvir)
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

    val clockText = remember(nowMs) { fmtTime(nowMs) }

    Box(modifier = Modifier.fillMaxSize().background(LiveBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 48.dp, top = 28.dp, bottom = 20.dp)
        ) {
            // ✅ Gornja traka: pill + sat
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivePill(pillFocus = pillFocus, onOpenNav = { navOpen = true })
                Spacer(modifier = Modifier.weight(1f))
                Text(clockText, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ✅ HERO (260dp): info levo + preview desno
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // — Info
                Column(
                    modifier = Modifier.weight(1.05f),
                    verticalArrangement = Arrangement.Center
                ) {
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

                // — LIVE PREVIEW (fallback slika ISPOD videa — nikad prazno)
                Box(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardBg)
                        .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                ) {
                    // Fallback slika (uvek vidljiva dok video ne krene)
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

                    // Video preko (vidljiv tek kad je spreman)
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

            Spacer(modifier = Modifier.height(20.dp))

            // ✅ EPG ZONA: grupe levo + GUSTA traka desno
            Row(modifier = Modifier.weight(1f)) {
                // — Grupe kanala
                LazyColumn(
                    modifier = Modifier.width(210.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        GroupRow(
                            title = "Svi kanali",
                            selected = selectedGroup == null,
                            onClick = { selectedGroup = null }
                        )
                    }
                    items(groups, key = { it }) { g ->
                        GroupRow(
                            title = g,
                            selected = selectedGroup == g,
                            onClick = { selectedGroup = g }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // — EPG traka
                Column(modifier = Modifier.weight(1f)) {
                    // Vremensko zaglavlje
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "DANAS · ${fmtTime(nowMs)}",
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
                        Text(fmtTime(nowMs + 30 * 60_000), color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                        Text(fmtTime(nowMs + 60 * 60_000), color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                        Text(fmtTime(nowMs + 90 * 60_000), color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(visibleChannels, key = { _, ch -> ch.id }) { index, ch ->
                            ChannelEpgRow(
                                channel = ch,
                                epg = epg,
                                nowMs = nowMs,
                                isFirstRow = index == 0,
                                onFocused = { channel, program ->
                                    focusedChannel = channel
                                    focusedProgram = program
                                },
                                onWatch = onWatch
                            )
                        }
                    }
                }
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

/** ✅ Grupa kanala (kompaktno) */
@Composable
private fun GroupRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    selected -> Color.White
                    focused -> Color.White.copy(alpha = 0.14f)
                    else -> CardBg
                }
            )
            .then(
                if (!selected && !focused) Modifier.border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                else if (focused) Modifier.border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1C1C1E))
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            title,
            color = if (selected) Color(0xFF1C1C1E) else Color.White,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** ✅ Red kanala u EPG traci (kompaktan, 72dp) */
@Composable
private fun ChannelEpgRow(
    channel: LiveChannel,
    epg: Map<String, List<EpgProgram>>,
    nowMs: Long,
    isFirstRow: Boolean,
    onFocused: (LiveChannel, EpgProgram) -> Unit,
    onWatch: (LiveChannel, EpgProgram?) -> Unit
) {
    // ✅ Širi prozor: -3h .. +6h → više programa u traci (kao referenca)
    val progs = remember(epg, channel, nowMs) {
        epgListFor(epg, channel)
            ?.filter { it.endMs > nowMs - 3 * 3_600_000L && it.startMs < nowMs + 6 * 3_600_000L }
            ?: emptyList()
    }
    val firstFocus = remember { FocusRequester() }
    val currentIndex = progs.indexOfFirst { nowMs >= it.startMs && nowMs < it.endMs }

    LaunchedEffect(isFirstRow) {
        if (isFirstRow) {
            delay(400)
            try { firstFocus.requestFocus() } catch (_: Exception) {}
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // — Kanal (logo + naziv)
        Column(
            modifier = Modifier
                .width(150.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(channel.logoUrl).crossfade(false)
                        .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888).build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                channel.name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // — Programi
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(progs, key = { _, p -> p.startMs }) { i, p ->
                ProgramBlock(
                    program = p,
                    nowMs = nowMs,
                    modifier = if (isFirstRow && i == currentIndex.coerceAtLeast(0))
                        Modifier.focusRequester(firstFocus) else Modifier,
                    onFocused = { onFocused(channel, p) },
                    onClick = {
                        if (nowMs >= p.startMs && nowMs < p.endMs) onWatch(channel, p)
                    }
                )
            }
        }
    }
}

/** ✅ Program blok (trenutni = beli pill sa "Xm left") */
@Composable
private fun ProgramBlock(
    program: EpgProgram,
    nowMs: Long,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    val isCurrent = nowMs >= program.startMs && nowMs < program.endMs
    val isPast = nowMs >= program.endMs

    TvFocusableButton(
        onClick = onClick,
        modifier = modifier
            .width(240.dp)
            .height(64.dp)
            .onFocusChanged { if (it.hasFocus) onFocused() }
    ) { focused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .scale(if (focused) 1.04f else 1f)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        isCurrent -> Color.White
                        else -> CardBg
                    }
                )
                .then(
                    when {
                        focused && !isCurrent -> Modifier.border(2.dp, Color.White, RoundedCornerShape(10.dp))
                        !focused && !isCurrent -> Modifier.border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        else -> Modifier
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                program.title,
                color = when {
                    isCurrent -> Color(0xFF1C1C1E)
                    isPast -> Color.White.copy(alpha = 0.4f)
                    else -> Color.White.copy(alpha = 0.92f)
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                buildString {
                    append("${fmtTime(program.startMs)} – ${fmtTime(program.endMs)}")
                    if (isCurrent) {
                        val rem = remainingMin(program.endMs, nowMs)
                        if (rem > 0) append(" · ${rem}m left")
                    }
                },
                color = if (isCurrent) Color(0xFF1C1C1E).copy(alpha = 0.65f)
                else Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
