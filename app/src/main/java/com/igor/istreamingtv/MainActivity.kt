package com.igor.istreamingtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.igor.istreamingtv.ui.details.MovieDetailsScreen
import com.igor.istreamingtv.ui.home.HomeScreen
import com.igor.istreamingtv.ui.movies.MoviesScreen
import com.igor.istreamingtv.ui.theme.IStreamingTheme

class MainActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf(Screen.HOME)
    private var detailsReturnScreen by mutableStateOf(Screen.HOME)
    private var selectedMovieId by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IStreamingTheme {
                when (currentScreen) {
                    Screen.HOME -> {
                        HomeScreen(
                            onMovieClick = { movie ->
                                selectedMovieId = movie.id
                                detailsReturnScreen = Screen.HOME
                                currentScreen = Screen.DETAILS
                            },
                            onMoviesClick = {
                                currentScreen = Screen.MOVIES
                            }
                        )
                    }

                    Screen.MOVIES -> {
                        MoviesScreen(
                            onMovieClick = { movie ->
                                selectedMovieId = movie.id
                                detailsReturnScreen = Screen.MOVIES
                                currentScreen = Screen.DETAILS
                            },
                            onBack = {
                                currentScreen = Screen.HOME
                            }
                        )
                    }

                    Screen.DETAILS -> {
                        if (selectedMovieId != 0) {
                            MovieDetailsScreen(
                                movieId = selectedMovieId,
                                onBack = {
                                    currentScreen = detailsReturnScreen
                                }
                            )
                        } else {
                            currentScreen = Screen.HOME
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Use OnBackPressedDispatcher instead")
    override fun onBackPressed() {
        when (currentScreen) {
            Screen.DETAILS -> currentScreen = detailsReturnScreen
            Screen.MOVIES -> currentScreen = Screen.HOME
            Screen.HOME -> super.onBackPressed()
        }
    }
}

private enum class Screen {
    HOME,
    MOVIES,
    DETAILS
}
