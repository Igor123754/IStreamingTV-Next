package com.igor.istreamingtv.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface

@Composable
fun TvFocusableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Surface(
        onClick = onClick,
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Enter || event.key == Key.DirectionCenter) {
                    onClick()
                    true
                } else {
                    false
                }
            },
        interactionSource = interactionSource,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            pressedContainerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        shape = ClickableSurfaceDefaults.shape(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content(isFocused)
        }
    }
}
