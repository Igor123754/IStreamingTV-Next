package com.igor.istreamingtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.igor.istreamingtv.data.remote.TmdbMovie
import com.igor.istreamingtv.ui.details.MovieDetailsScreen
import com.igor.istreamingtv.ui.home.HomeScreen
import com.igor.istreamingtv.ui.theme.IStreamingTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {

            IStreamingTheme {

                var selectedMovie by rememberSaveable(
                    stateSaver = androidx.compose.runtime.saveable.Saver(
                        save = {
                            listOf(
                                it.id,
                                it.displayTitle,
                                it.displayDate,
                                it.posterPath,
                                it.backdropPath
                            )
                        },
                        restore = {
                            TmdbMovie(
                                id = it[0] as Int,
                                posterPath = it[3] as String?,
                                backdropPath = it[4] as String?,
                                displayTitle = it[1] as String,
                                displayDate = it[2] as String
                            )
                        }
                    )
                ) {
                    mutableStateOf<TmdbMovie?>(null)
                }

                if (selectedMovie == null) {

                    HomeScreen(
                        onMovieClick = { movie ->
                            selectedMovie = movie
                        }
                    )

                } else {

                    MovieDetailsScreen(
                        movie = selectedMovie!!,
                        onBack = {
                            selectedMovie = null
                        }
                    )
                }
            }
        }
    }
}
