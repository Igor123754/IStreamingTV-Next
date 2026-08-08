package com.igor.istreamingtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.igor.istreamingtv.ui.components.GlassCard
import com.igor.istreamingtv.ui.theme.Background
import com.igor.istreamingtv.ui.theme.IStreamingTheme
import com.igor.istreamingtv.ui.theme.TextPrimary
import com.igor.istreamingtv.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IStreamingTheme {
                DesignSystemPreview()
            }
        }
    }
}

@Composable
private fun DesignSystemPreview() {

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(48.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "IStreamingTV",
                color = TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "A new way to experience streaming",
                color = TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                GlassCard(
                    modifier = Modifier
                        .width(220.dp)
                        .height(100.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Movies",
                            color = TextPrimary,
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = "Explore",
                            color = TextSecondary,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                GlassCard(
                    modifier = Modifier
                        .width(220.dp)
                        .height(100.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Series",
                            color = TextPrimary,
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = "Explore",
                            color = TextSecondary,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
