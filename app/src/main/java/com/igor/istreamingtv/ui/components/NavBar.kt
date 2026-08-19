package com.igor.istreamingtv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NavDestination { SEARCH, HOME, LIVE, MOVIES, SERIES, LIBRARY, SETTINGS }

// =====================================================================
// ✅ PILL (icon + label) — same as Apple TV+ home pill
// =====================================================================

@Composable
fun NavPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    pillFocus: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(180), label = "")

    Row(
        modifier = Modifier
            .then(if (pillFocus != null) Modifier.focusRequester(pillFocus) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
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
            Icon(icon, null, tint = Color(0xFF1C1C1E), modifier = Modifier.size(16.dp))
        }
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// =====================================================================
// Icons
// =====================================================================

@Composable
fun LiveIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val s = w * 0.09f
        drawRoundRect(Color.White, androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.30f),
            androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.50f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = Stroke(s))
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.30f), androidx.compose.ui.geometry.Offset(w * 0.32f, h * 0.10f), strokeWidth = s)
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.30f), androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.10f), strokeWidth = s)
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.92f), androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.92f), strokeWidth = s)
    }
}

@Composable
fun MoviesIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val s = w * 0.09f
        drawRoundRect(Color.White, androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.14f),
            androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.72f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = Stroke(s))
        drawPath(Path().apply {
            moveTo(w * 0.42f, h * 0.34f); lineTo(w * 0.68f, h * 0.50f); lineTo(w * 0.42f, h * 0.66f); close()
        }, Color.White)
    }
}

@Composable
fun SeriesIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val s = w * 0.09f
        drawRoundRect(Color.White, androidx.compose.ui.geometry.Offset(w * 0.24f, h * 0.12f),
            androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.40f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f), style = Stroke(s))
        drawRoundRect(Color.White, androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.48f),
            androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.40f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f), style = Stroke(s))
    }
}

@Composable
fun LibraryIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(Color.White, androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.18f), androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.64f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
        drawRoundRect(Color.White, androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.18f), androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.64f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
        drawRoundRect(Color.White, androidx.compose.ui.geometry.Offset(w * 0.70f, h * 0.18f), androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.64f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
    }
}

// =====================================================================
// ✅ Nav row
// =====================================================================

@Composable
fun NavRow(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable (Modifier) -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    selected -> Color.White
                    focused -> Color.White.copy(alpha = 0.16f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0xFF1C1C1E) else Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            icon(Modifier.size(17.dp))
        }
        Text(
            title,
            color = if (selected) Color(0xFF1C1C1E) else Color.White,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

// =====================================================================
// ✅ Shared navigation bar — `current` shows where the user is
// =====================================================================

@Composable
fun NavBarDrawer(
    open: Boolean,
    current: NavDestination,
    onDismiss: () -> Unit,
    onNavigate: (NavDestination) -> Unit
) {
    val selectedFocus = remember { FocusRequester() }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(open) {
        if (open) {
            delay(120)
            try { selectedFocus.requestFocus() } catch (_: Exception) {}
            while (open) {
                nowMs = System.currentTimeMillis()
                delay(10_000)
            }
        }
    }

    val timeText = remember(nowMs) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
    }

    AnimatedVisibility(
        visible = open,
        enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(260)),
        exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(220))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 24.dp)
                    .padding(start = 24.dp)
                    .width(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF161B26).copy(alpha = 0.96f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .padding(bottom = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(timeText, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                }

                NavRow(
                    title = "Pretraga",
                    selected = current == NavDestination.SEARCH,
                    modifier = if (current == NavDestination.SEARCH) Modifier.focusRequester(selectedFocus) else Modifier,
                    icon = { mod -> Icon(Icons.Default.Search, null, tint = if (current == NavDestination.SEARCH) Color.White else Color.White, modifier = mod) },
                    onClick = { onNavigate(NavDestination.SEARCH) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                NavRow(
                    title = "Početna",
                    selected = current == NavDestination.HOME,
                    modifier = if (current == NavDestination.HOME) Modifier.focusRequester(selectedFocus) else Modifier,
                    icon = { mod -> Icon(Icons.Default.Home, null, tint = Color.White, modifier = mod) },
                    onClick = { onNavigate(NavDestination.HOME) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                NavRow(
                    title = "Uživo TV",
                    selected = current == NavDestination.LIVE,
                    icon = { mod -> LiveIcon(mod) },
                    onClick = { onNavigate(NavDestination.LIVE) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                NavRow(
                    title = "Filmovi",
                    selected = current == NavDestination.MOVIES,
                    icon = { mod -> MoviesIcon(mod) },
                    onClick = { onNavigate(NavDestination.MOVIES) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                NavRow(
                    title = "Serije",
                    selected = current == NavDestination.SERIES,
                    icon = { mod -> SeriesIcon(mod) },
                    onClick = { onNavigate(NavDestination.SERIES) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                NavRow(
                    title = "Biblioteka",
                    selected = current == NavDestination.LIBRARY,
                    icon = { mod -> LibraryIcon(mod) },
                    onClick = { onNavigate(NavDestination.LIBRARY) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                NavRow(
                    title = "Podešavanja",
                    selected = current == NavDestination.SETTINGS,
                    icon = { mod -> Icon(Icons.Default.Settings, null, tint = Color.White, modifier = mod) },
                    onClick = { onNavigate(NavDestination.SETTINGS) }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss)
            )
        }
    }
}
