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

    private var currentScreen by mutableStateOf(Screen.HOME)

    /*
     * Ekran sa kojeg smo otvorili Details.
     *
     * Ovo rešava problem:
     * Home -> Details -> Nazad mora vratiti Home
     * Movies -> Details -> Nazad mora vratiti Movies
     */
    private var detailsReturnScreen by mutableStateOf(Screen.HOME)

    private var selectedMovie by mutableStateOf<TmdbMovie?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            IStreamingTheme {

                when (currentScreen) {

                    Screen.HOME -> {

                        HomeScreen(
                            onMovieClick = { movie ->

                                selectedMovie = movie

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

                                selectedMovie = movie

                                detailsReturnScreen = Screen.MOVIES

                                currentScreen = Screen.DETAILS
                            },

                            onBack = {

                                currentScreen = Screen.HOME
                            }
                        )
                    }

                    Screen.DETAILS -> {

                        val movie = selectedMovie

                        if (movie != null) {

                            MovieDetailsScreen(
                                movie = movie,

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

            Screen.DETAILS -> {

                currentScreen = detailsReturnScreen
            }

            Screen.MOVIES -> {

                currentScreen = Screen.HOME
            }

            Screen.HOME -> {

                super.onBackPressed()
            }
        }
    }
}

/*
 * Svi ekrani aplikacije.
 *
 * Kasnije ćemo ovde dodati:
 *
 * SERIES
 * SEARCH
 * LIVE_TV
 * SETTINGS
 */
private enum class Screen {

    HOME,
    MOVIES,
    DETAILS
}
