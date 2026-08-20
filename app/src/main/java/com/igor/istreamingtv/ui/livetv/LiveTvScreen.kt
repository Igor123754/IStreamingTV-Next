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

private val LiveBg = Color(0xFF020204)
private val CardBg = Color(0xFF14181F)

private fun nowProgram(epg: Map<String, List<EpgProgram>>, ch: LiveChannel): EpgProgram? {
    val now = System.currentTimeMillis()
    return epgListFor(epg, ch)?.firstOrNull { now >= it.startMs && now < it.endMs }
}

private fun fmtTime(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

private fun remainingMin(endMs: Long, nowMs: Long): Long =
    ((endMs - nowMs) / 60_000L).coerceAtLeast(0)

/**
 * ✅ UŽIVO TV — Apple TV+ stil:
 *    pill + sat gore, hero info levo, LIVE PREVIEW desno,
 *    grupe kanala levo, EPG traka desno.
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
    // ✅ LIVE PREVIEW (muted, mali bufferi, debounce 800ms)
    // =====================================================================
    val previewPlayer = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(2_000, 8_000, 400, 1_200)
                    .setTargetBufferBytes(4 * 1024 * 1024)
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
    var previewError by remember { mutableStateOf(false) }

    DisposableEffect(previewPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(s: Int) {
                previewBuffering = s == Player.STATE_BUFFERING
            }
            override fun onPlayerError(e: PlaybackException) {
                previewError = true
                previewBuffering = false
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
                .padding(start = 48.dp, end = 48.dp, top = 32.dp, bottom = 24.dp)
        ) {
            // ✅ Gornja traka: pill + sat (bez navigacije)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivePill(pillFocus = pillFocus, onOpenNav = { navOpen = true })
                Spacer(modifier = Modifier.weight(1f))
                Text(clockText, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ✅ HERO: info levo + LIVE PREVIEW desno
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // — Info o emisiji
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE50914))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("UŽIVO", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        heroProgram?.category?.let { cat ->
                            Text(cat, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        heroProgram?.title ?: heroChannel?.name ?: "Uživo TV",
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (heroChannel != null && heroProgram != null) {
                        Text(
                            "${heroChannel.name} · ${fmtTime(heroProgram.startMs)} – ${fmtTime(heroProgram.endMs)} · ${remainingMin(heroProgram.endMs, nowMs)} min preostalo",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    heroProgram?.description?.let { desc ->
                        Text(
                            desc,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (heroProgram != null && heroProgram.endMs > heroProgram.startMs) {
                        val prog = ((nowMs - heroProgram.startMs).toFloat() /
                            (heroProgram.endMs - heroProgram.startMs)).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.25f))
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

                // — LIVE PREVIEW (gore desno)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBg)
                ) {
                    if (previewError) {
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
                    } else {
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
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                )
                            )
                    )

                    // Logo kanala gore desno u preview-u
                    if (!heroChannel?.logoUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(8.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(heroChannel!!.logoUrl!!).crossfade(false)
                                    .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888).build(),
                                contentDescription = null,
                                modifier = Modifier.size(height = 34.dp, width = 60.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    if (previewBuffering && !previewError) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ✅ EPG ZONA: grupe levo + traka desno
            Row(modifier = Modifier.weight(1f)) {
                // — Grupe kanala (sortiranje iz M3U)
                LazyColumn(
                    modifier = Modifier.width(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

                Spacer(modifier = Modifier.width(24.dp))

                // — EPG traka po kanalima
                Column(modifier = Modifier.weight(1f)) {
                    // Vremensko zaglavlje
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("SADA · ${fmtTime(nowMs)}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(fmtTime(nowMs + 30 * 60_000), color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                        Text(fmtTime(nowMs + 60 * 60_000), color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                        Text(fmtTime(nowMs + 90 * 60_000), color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

        // ✅ Navigaciona traka (leva) — "Uživo TV" selektovana
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

/** ✅ Pill "Uživo TV" (kao na početnoj) */
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

/** ✅ Grupa kanala (levo sortiranje) */
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
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    selected -> Color.White
                    focused -> Color.White.copy(alpha = 0.14f)
                    else -> Color.White.copy(alpha = 0.05f)
                }
            )
            .then(
                if (selected && !focused) Modifier
                else if (focused) Modifier.border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1C1C1E))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            title,
            color = if (selected) Color(0xFF1C1C1E) else Color.White,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** ✅ Red kanala u EPG traci */
@Composable
private fun ChannelEpgRow(
    channel: LiveChannel,
    epg: Map<String, List<EpgProgram>>,
    nowMs: Long,
    isFirstRow: Boolean,
    onFocused: (LiveChannel, EpgProgram) -> Unit,
    onWatch: (LiveChannel, EpgProgram?) -> Unit
) {
    val progs = remember(epg, channel, nowMs) {
        epgListFor(epg, channel)
            ?.filter { it.endMs > nowMs - 3_600_000L && it.startMs < nowMs + 3 * 3_600_000L }
            ?: emptyList()
    }
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(isFirstRow) {
        if (isFirstRow) {
            delay(400)
            try { firstFocus.requestFocus() } catch (_: Exception) {}
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // — Kanal (logo + naziv)
        Column(
            modifier = Modifier
                .width(170.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(CardBg)
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(channel.logoUrl).crossfade(false)
                        .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888).build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                channel.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // — Programi (EPG traka)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(progs, key = { _, p -> p.startMs }) { i, p ->
                ProgramCard(
                    program = p,
                    nowMs = nowMs,
                    modifier = if (isFirstRow && i == 0) Modifier.focusRequester(firstFocus) else Modifier,
                    onFocused = { onFocused(channel, p) },
                    onClick = {
                        val isCurrent = nowMs >= p.startMs && nowMs < p.endMs
                        if (isCurrent) onWatch(channel, p)
                    }
                )
            }
        }
    }
}

/** ✅ Program kartica (trenutni = beli pill) */
@Composable
private fun ProgramCard(
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
            .width(280.dp)
            .height(76.dp)
            .onFocusChanged { if (it.hasFocus) onFocused() }
    ) { focused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .scale(if (focused) 1.04f else 1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        isCurrent -> Color.White
                        focused -> Color.White.copy(alpha = 0.16f)
                        else -> CardBg
                    }
                )
                .then(
                    if (focused && !isCurrent) Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                program.title,
                color = if (isCurrent) Color(0xFF1C1C1E) else Color.White.copy(alpha = if (isPast) 0.45f else 0.95f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                buildString {
                    append("${fmtTime(program.startMs)} – ${fmtTime(program.endMs)}")
                    if (isCurrent) append(" · ${remainingMin(program.endMs, nowMs)}m left")
                },
                color = if (isCurrent) Color(0xFF1C1C1E).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}
