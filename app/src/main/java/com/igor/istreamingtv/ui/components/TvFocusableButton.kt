package com.igor.istreamingtv.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role

@Composable
fun TvFocusableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Enter || event.key == Key.DirectionCenter) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(isFocused)
    }
}

// Overload za postojeće pozive bez isFocused parametra
@Composable
fun TvFocusableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    TvFocusableButton(
        onClick = onClick,
        modifier = modifier
    ) { _ ->
        content()
    }
}
