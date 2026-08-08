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
import androidx.compose.material3.MaterialTheme
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

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            HeroSection()

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            ContentSection(
                title = "Nastavi gledanje"
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            ContentSection(
                title = "Popularno"
            )
        }
    }
}

@Composable
private fun TopNavigation() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "IStreamingTV",
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.width(52.dp)
        )

        NavigationItem("Home")

        NavigationItem("Movies")

        NavigationItem("Series")

        Spacer(
            modifier = Modifier.weight(1f)
        )

        NavigationItem("Search")
    }
}

@Composable
private fun NavigationItem(
    title: String
) {

    var focused by remember {
        mutableStateOf(false)
    }

    Text(
        text = title,
        color = if (focused) {
            TextPrimary
        } else {
            TextSecondary
        },
        style = MaterialTheme.typography.labelLarge,

        modifier = Modifier
            .padding(
                horizontal = 14.dp,
                vertical = 10.dp
            )
            .clip(
                RoundedCornerShape(14.dp)
            )
            .background(
                if (focused) {
                    Color.White.copy(alpha = 0.10f)
                } else {
                    Color.Transparent
                }
            )
            .border(
                width = if (focused) {
                    1.dp
                } else {
                    0.dp
                },
                color = Color.White.copy(alpha = 0.22f),
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged {
                focused = it.isFocused
            }
            .focusable()
    )
}

@Composable
private fun HeroSection() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .clip(
                RoundedCornerShape(28.dp)
            )
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF242936),
                        Color(0xFF111318),
                        Color(0xFF090A0D)
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(42.dp)
        ) {

            Text(
                text = "FEATURED",
                color = TextSecondary,
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "The Next Journey",
                color = TextPrimary,
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "2026  •  2h 14m  •  Drama  •  16+",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = "A new story begins where everything you know comes to an end.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(
                modifier = Modifier.height(26.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                GlassCard(
                    modifier = Modifier
                        .width(150.dp)
                        .height(52.dp)
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "▶  Gledaj",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                GlassCard(
                    modifier = Modifier
                        .width(150.dp)
                        .height(52.dp)
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "＋  Moja lista",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentSection(
    title: String
) {

    Column {

        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            repeat(6) {

                GlassCard(
                    modifier = Modifier
                        .width(150.dp)
                        .height(210.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF303541),
                                        Color(0xFF14161B)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
