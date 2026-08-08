package com.igor.istreamingtv.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.theme.Background
import com.igor.istreamingtv.ui.theme.TextPrimary
import com.igor.istreamingtv.ui.theme.TextSecondary

private const val BACKDROP_URL =
    "https://image.tmdb.org/t/p/w1280"

@Composable
fun MovieDetailsScreen(
    movie: TmdbMovie,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        if (movie.backdropPath != null) {

            AsyncImage(
                model = BACKDROP_URL + movie.backdropPath,
                contentDescription = movie.displayTitle,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.96f),
                            Color.Black.copy(alpha = 0.82f),
                            Color.Black.copy(alpha = 0.48f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Background.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 60.dp,
                    vertical = 42.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            TvFocusableButton(
                modifier = Modifier
                    .width(110.dp)
                    .height(48.dp),
                onClick = onBack
            ) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "‹  Nazad",
                        color = TextPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = 30.dp
                    )
            ) {

                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(12.dp)
                        )
                        .background(
                            Color.White.copy(alpha = 0.10f)
                        )
                        .padding(
                            horizontal = 14.dp,
                            vertical = 7.dp
                        )
                ) {

                    Text(
                        text = "FILM",
                        color = TextSecondary
                    )
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = movie.displayTitle,
                    color = TextPrimary
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                if (movie.displayDate.isNotEmpty()) {

                    Text(
                        text = movie.displayDate,
                        color = TextSecondary
                    )
                }

                Spacer(
                    modifier = Modifier.height(26.dp)
                )

                Text(
                    text = "Film je dostupan kroz izvore povezane sa aplikacijom.",
                    color = TextSecondary
                )

                Spacer(
                    modifier = Modifier.height(30.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    TvFocusableButton(
                        modifier = Modifier
                            .width(160.dp)
                            .height(56.dp),
                        onClick = {
                            // Stremio addon/player dolazi u sledećem koraku.
                        }
                    ) {

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {

                            Text(
                                text = "▶  Gledaj",
                                color = TextPrimary
                            )
                        }
                    }

                    TvFocusableButton(
                        modifier = Modifier
                            .width(160.dp)
                            .height(56.dp),
                        onClick = {
                            // Moja lista dolazi kasnije.
                        }
                    ) {

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {

                            Text(
                                text = "+  Moja lista",
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
