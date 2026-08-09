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
import com.igor.istreamingtv.ui.movies.MoviesScreen
import com.igor.istreamingtv.ui.theme.IStreamingTheme

class MainActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf("home")

    private var selectedMovie by mutableStateOf<TmdbMovie?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            IStreamingTheme {

                when (currentScreen) {

                    "home" -> {

                        HomeScreen(
                            onMovieClick = { movie ->
                                selectedMovie = movie
                                currentScreen = "details"
                            },
                            onMoviesClick = {
                                currentScreen = "movies"
                            }
                        )
                    }

                    "movies" -> {

                        MoviesScreen(
                            onMovieClick = { movie ->
                                selectedMovie = movie
                                currentScreen = "details"
                            },
                            onBack = {
                                currentScreen = "home"
                            }
                        )
                    }

                    "details" -> {

                        val movie = selectedMovie

                        if (movie != null) {

                            MovieDetailsScreen(
                                movie = movie,
                                onBack = {

                                    currentScreen = "movies"
                                }
                            )

                        } else {

                            currentScreen = "home"
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Use OnBackPressedDispatcher instead")
    override fun onBackPressed() {

        when (currentScreen) {

            "details" -> {
                currentScreen = "movies"
            }

            "movies" -> {
                currentScreen = "home"
            }

            else -> {
                super.onBackPressed()
            }
        }
    }
}
