package com.igor.istreamingtv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.onFocusChanged
import com.igor.istreamingtv.ui.theme.GlassBorder
import com.igor.istreamingtv.ui.theme.GlassSurface
import com.igor.istreamingtv.ui.theme.GlassSurfaceStrong

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.035f else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 400f
        ),
        label = "glassCardScale"
    )

    val backgroundBrush = Brush.verticalGradient(
        colors = if (isFocused) {
            listOf(
                GlassSurfaceStrong,
                GlassSurface
            )
        } else {
            listOf(
                GlassSurface,
                Color(0x0FFFFFFF)
            )
        }
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundBrush)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) {
                    Color.White.copy(alpha = 0.55f)
                } else {
                    GlassBorder
                },
                shape = RoundedCornerShape(22.dp)
            )
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .focusable(enabled = enabled),
        content = content
    )
}
