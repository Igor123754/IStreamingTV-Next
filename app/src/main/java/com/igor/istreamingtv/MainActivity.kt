package com.igor.istreamingtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.igor.istreamingtv.data.profile.AppSession
import com.igor.istreamingtv.data.profile.Profile
import com.igor.istreamingtv.data.profile.ProfileStore
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.ui.details.MovieDetailsScreen
import com.igor.istreamingtv.ui.home.HomeScreen
import com.igor.istreamingtv.ui.movies.MoviesScreen
import com.igor.istreamingtv.ui.profile.PinLockScreen
import com.igor.istreamingtv.ui.profile.ProfileScreen
import com.igor.istreamingtv.ui.theme.IStreamingTheme

class MainActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf(Screen.HOME)
    private var detailsReturnScreen by mutableStateOf(Screen.HOME)
    private var selectedMovie by mutableStateOf<TmdbMovie?>(null)
    private var pinProfile by mutableStateOf<Profile?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ STARTUP LOGIKA:
        //  - nema profila → "Ko gleda?" (pravljenje)
        //  - poslednji profil BEZ šifre → odmah na početnu (ne pita!)
        //  - poslednji profil SA šifrom → obavezna šifra
        val profiles = ProfileStore.load(this)
        when {
            profiles.isEmpty() -> currentScreen = Screen.PROFILE
            else -> {
                val last = profiles.firstOrNull { it.id == ProfileStore.lastUsedId(this) }
                    ?: profiles.first()
                if (last.hasPin) {
                    pinProfile = last
                    currentScreen = Screen.PIN
                } else {
                    AppSession.currentProfile = last
                    currentScreen = Screen.HOME
                }
            }
        }

        setContent {
            IStreamingTheme {
                when (currentScreen) {
                    Screen.PROFILE -> {
                        ProfileScreen(
                            onSelected = { p ->
                                AppSession.currentProfile = p
                                ProfileStore.touch(this, p.id)
                                currentScreen = Screen.HOME
                            },
                            onBack = if (AppSession.currentProfile != null) {
                                { currentScreen = Screen.HOME }
                            } else null
                        )
                    }

                    Screen.PIN -> {
                        val p = pinProfile
                        if (p != null) {
                            PinLockScreen(
                                profile = p,
                                onSuccess = {
                                    AppSession.currentProfile = p
                                    ProfileStore.touch(this, p.id)
                                    pinProfile = null
                                    currentScreen = Screen.HOME
                                },
                                onCancel = {
                                    pinProfile = null
                                    currentScreen = Screen.PROFILE
                                }
                            )
                        } else {
                            currentScreen = Screen.PROFILE
                        }
                    }

                    Screen.HOME -> {
                        HomeScreen(
                            onMovieClick = { movie ->
                                selectedMovie = movie
                                detailsReturnScreen = Screen.HOME
                                currentScreen = Screen.DETAILS
                            },
                            onAddToLibrary = {
                                // TODO: implement add to library action
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
                                },
                                onMovieClick = { part ->
                                    selectedMovie = part
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
            Screen.PIN -> currentScreen = Screen.PROFILE
            Screen.PROFILE -> {
                if (AppSession.currentProfile != null) currentScreen = Screen.HOME
                else super.onBackPressed()
            }
            Screen.HOME -> super.onBackPressed()
        }
    }

    private enum class Screen {
        HOME,
        MOVIES,
        DETAILS,
        PROFILE,
        PIN
    }
}
