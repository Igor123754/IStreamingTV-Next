package com.igor.istreamingtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.ui.details.MovieDetailsScreen
import com.igor.istreamingtv.ui.home.HomeScreen
import com.igor.istreamingtv.ui.theme.IStreamingTheme

class MainActivity : ComponentActivity() {

    private var selectedMovie by mutableStateOf<TmdbMovie?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            IStreamingTheme {

                val movie = selectedMovie

                if (movie == null) {

                    HomeScreen(
                        onMovieClick = { selected ->
                            selectedMovie = selected
                        }
                    )

                } else {

                    MovieDetailsScreen(
                        movie = movie,
                        onBack = {
                            selectedMovie = null
                        }
                    )
                }
            }
        }
    }
}
