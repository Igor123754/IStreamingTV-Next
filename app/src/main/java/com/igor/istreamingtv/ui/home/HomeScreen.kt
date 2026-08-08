package com.igor.istreamingtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.igor.istreamingtv.ui.components.GlassCard
import com.igor.istreamingtv.ui.theme.Background
import com.igor.istreamingtv.ui.theme.TextPrimary
import com.igor.istreamingtv.ui.theme.TextSecondary

@Composable
fun HomeScreen() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF151820),
                        Background,
                        Background
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 52.dp,
                    end = 52.dp,
                    top = 28.dp,
                    bottom = 28.dp
                )
        ) {

            TopNavigation()

            Spacer(modifier = Modifier.height(28.dp))

            HeroSection()

            Spacer(modifier = Modifier.height(32.dp))

            ContentSection(
                title = "Nastavi gledanje"
            )

            Spacer(modifier = Modifier.height(28.dp))

            ContentSection(
                title = "Popularno"
            )
        }
    }
}
