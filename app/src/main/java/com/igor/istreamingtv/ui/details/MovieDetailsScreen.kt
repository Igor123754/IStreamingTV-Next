package com.igor.istreamingtv.ui.details

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.igor.istreamingtv.data.remote.*
import com.igor.istreamingtv.ui.components.TvFocusableButton
import com.igor.istreamingtv.ui.player.PlayerActivity
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.Locale

private val DetailsBackground = Color(0xFF020204)
private val SurfaceBackground = Color(0xFF0C0D12)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xB3FFFFFF)
private val CardShape = RoundedCornerShape(12.dp)

private fun startPlayer(
    context: Context,
    candidates: List<StreamPicker.Candidate>,
    imdbId: String? = null,
    season: Int = -1,
    episode: Int = -1,
    subtitleTracks: List<SubtitleTrack> = emptyList(),
    runtimeSeconds: Int = -1,
    title: String? = null,
    posterUrl: String? = null,
    backdropUrl: String? = null,
    overviewText: String? = null
) {
    val intent = Intent(context, PlayerActivity::class.java).apply {
        // ✅ Type-safe JSON
        putExtra("candidates", TmdbClient.json.encodeToString(candidates.map { it.url }))
        if (imdbId != null) putExtra("imdb_id", imdbId)
        putExtra("season", season)
        putExtra("episode", episode)
        if (subtitleTracks.isNotEmpty()) {
            putExtra("subtitle_tracks", TmdbClient.json.encodeToString(subtitleTracks))
        }
        if (runtimeSeconds > 0) putExtra("runtime_sec", runtimeSeconds)
        if (!title.isNullOrBlank()) putExtra("title", title)
        if (!posterUrl.isNullOrBlank()) putExtra("poster", posterUrl)
        if (!backdropUrl.isNullOrBlank()) putExtra("backdrop", backdropUrl)
        if (!overviewText.isNullOrBlank()) putExtra("overview", overviewText)
    }
    context.startActivity(intent)
}

@Composable
fun MovieDetailsScreen(
    movie: TmdbMovie,
    onBack: () -> Unit,
    onMovieClick: (TmdbMovie) -> Unit = {}
) {
    val viewModel: MovieDetailsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val isTv = movie.name != null

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val playEpisode: (TmdbEpisode) -> Unit = { episode ->
        scope.launch {
            val key = "${episode.seasonNumber}:${episode.episodeNumber}"
            val cached = state.episodeCandidates[key]
            val candidates = cached ?: viewModel.candidatesForEpisode(
                episode.seasonNumber, episode.episodeNumber
            )
            if (candidates.isNotEmpty()) {
                val subs = state.episodeSubtitleTracks[key]
                    ?: viewModel.subtitlesForEpisode(episode.seasonNumber, episode.episodeNumber)
                val epOverview = episode.overview?.takeIf { it.isNotBlank() }
                    ?: state.details?.pickSerbianOverview()
                    ?: state.details?.overview
                val seriesBackdrop = state.details?.backdropPath
                    ?.let { "https://image.tmdb.org/t/p/w780$it" }
                startPlayer(
                    context, candidates,
                    imdbId = state.details?.pickImdbId(),
                    season = episode.seasonNumber,
                    episode = episode.episodeNumber,
                    subtitleTracks = subs,
                    runtimeSeconds = (episode.runtime ?: 0) * 60,
                    title = movie.displayTitle,
                    posterUrl = movie.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
                    backdropUrl = seriesBackdrop,
                    overviewText = epOverview
                )
            } else {
                Toast.makeText(context, "Nema dostupnih izvora za ovu epizodu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(movie.id) {
        viewModel.load(movie, isTv)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailsBackground)
    ) {
        when {
            state.isLoading -> DetailsShimmer()
            state.error != null -> DetailsError(
                message = state.error ?: "Nepoznata greška",
                onBack = onBack
            )
            else -> DetailsScrollContent(
                movie = movie,
                state = state,
                isTv = isTv,
                onMovieClick = onMovieClick,
                onSelectSeason = { season -> viewModel.selectSeason(movie.id, season) },
                onPlayEpisode = playEpisode
            )
        }
    }
}

@Composable
private fun DetailsScrollContent(
    movie: TmdbMovie,
    state: DetailsUiState,
    isTv: Boolean,
    onMovieClick: (TmdbMovie) -> Unit,
    onSelectSeason: (Int) -> Unit,
    onPlayEpisode: (TmdbEpisode) -> Unit
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

    val listState = rememberLazyListState()

    val scrollProgressState = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull() }
            .collect { item ->
                val size = item?.size?.coerceAtLeast(1) ?: 1
                scrollProgressState.value =
                    (-(item?.offset ?: 0).toFloat() / size).coerceIn(0f, 1f)
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {

        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeightDp)
            ) {
                DetailsHero(
                    movie = movie,
                    details = state.details,
                    isTv = isTv,
                    preparingStreams = state.preparingStreams,
                    movieCandidates = state.movieCandidates,
                    movieSubtitleTracks = state.movieSubtitleTracks,
                    firstEpisode = state.episodes.firstOrNull(),
                    scrollProgressState = scrollProgressState
                )
            }
        }

        if (!isTv && state.collectionParts.isNotEmpty()) {
            item(key = "collection") {
                EnterAnimatedRow {
                    PosterRowSection(
                        title = state.collectionName ?: "Nastavci i franšiza",
                        items = state.collectionParts,
                        onMovieClick = onMovieClick
                    )
                }
            }
        }

        if (isTv && state.seasons.isNotEmpty()) {
            item(key = "seasons") {
                EnterAnimatedRow {
                    SeasonRowSection(
                        seasons = state.seasons,
                        selectedSeason = state.selectedSeasonNumber,
                        onSelectSeason = onSelectSeason
                    )
                }
            }
        }

        if (isTv && state.episodes.isNotEmpty()) {
            item(key = "episodes") {
                EnterAnimatedRow {
                    EpisodeRowSection(
                        seasonNumber = state.selectedSeasonNumber,
                        episodes = state.episodes,
                        onEpisodeClick = onPlayEpisode
                    )
                }
            }
        }

        if (state.similar.isNotEmpty()) {
            item(key = "similar") {
                EnterAnimatedRow {
                    PosterRowSection(
                        title = if (isTv) "Slične serije" else "Slični filmovi",
                        items = state.similar,
                        onMovieClick = onMovieClick
                    )
                }
            }
        }

        item(key = "bottom-spacer") {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun EnterAnimatedRow(content: @Composable () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val rowAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(600), label = "row-alpha")
    val rowOffsetY by animateFloatAsState(if (entered) 0f else 80f, tween(600), label = "row-offset")

    Column(
        modifier = Modifier
            .graphicsLayer {
                alpha = rowAlpha
                translationY = rowOffsetY
            }
            .padding(top = 40.dp)
    ) {
        content()
    }
}

// ================= HERO =================

@Composable
private fun DetailsHero(
    movie: TmdbMovie,
    details: TmdbHeroDetails?,
    isTv: Boolean,
    preparingStreams: Boolean,
    movieCandidates: List<StreamPicker.Candidate>,
    movieSubtitleTracks: List<SubtitleTrack>,
    firstEpisode: TmdbEpisode?,
    scrollProgressState: State<Float>
) {
    val context = LocalContext.current

    var inLibrary by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    val logoUrl = details?.pickClearLogoUrl()
    val overview = details?.pickSerbianOverview()
        ?: details?.overview?.takeIf { it.isNotBlank() }
        ?: movie.displayOverview
    val genres = details?.genres?.joinToString(", ") { it.name } ?: ""
    val date = details?.releaseDate ?: details?.firstAirDate ?: movie.displayDate
    val runtimeMin = details?.runtime ?: details?.episodeRunTime?.firstOrNull()
    val cert = details?.pickCertification()
    val cast = details?.credits?.cast
        ?.mapNotNull { it.name }
        ?.take(3)
        ?.joinToString(", ")
        ?: ""

    val backdropUrl = "https://image.tmdb.org/t/p/w1280" +
        (details?.backdropPath ?: movie.backdropPath ?: details?.posterPath ?: movie.posterPath ?: "")

    Box(modifier = Modifier.fillMaxSize()) {

        AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = scrollProgressState.value
                    scaleX = 1f + p * 0.08f
                    scaleY = 1f + p * 0.08f
                },
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.92f)
                        ),
                        startY = 300f
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 48.dp, top = 44.dp)
        ) {
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = movie.displayTitle,
                    modifier = Modifier
                        .width(340.dp)
                        .height(120.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart
                )
            } else {
                Text(
                    text = movie.displayTitle,
                    color = TextPrimary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 500.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, bottom = 40.dp)
                .graphicsLayer {
                    val p = scrollProgressState.value
                    alpha = (1f - p * 1.2f).coerceIn(0f, 1f)
                    translationY = p * 240f
                }
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvFocusableButton(onClick = {
                        when {
                            !isTv && movieCandidates.isNotEmpty() ->
                                startPlayer(
                                    context, movieCandidates,
                                    imdbId = details?.pickImdbId(),
                                    subtitleTracks = movieSubtitleTracks,
                                    runtimeSeconds = (runtimeMin ?: 0) * 60,
                                    title = movie.displayTitle,
                                    posterUrl = movie.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
                                    backdropUrl = backdropUrl,
                                    overviewText = overview
                                )

                            !isTv && preparingStreams ->
                                notice = "Pripremamo najbolji izvor..."

                            isTv && firstEpisode != null -> firstEpisode

                            else -> notice = "Nema dostupnih izvora za ovaj naslov"
                        }
                    }) { focused ->
                        val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(160), label = "")
                        Row(
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFE9E9F2))
                                .padding(horizontal = 32.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Gledaj", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    TvFocusableButton(onClick = { inLibrary = !inLibrary }) { focused ->
                        val scale by animateFloatAsState(if (focused) 1.05f else 1f, tween(160), label = "")
                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (focused) Color.White.copy(alpha = 0.25f)
                                    else Color.Black.copy(alpha = 0.6f)
                                )
                                .border(
                                    width = if (focused) 3.dp else 1.dp,
                                    color = if (focused) Color.White
                                    else Color.White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 26.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = if (inLibrary) "✓ U biblioteci" else "＋ U biblioteku",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    notice?.let {
                        Text(text = it, color = TextSecondary, fontSize = 13.sp)
                    }
                }

                Text(
                    text = overview,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1.2f)
                )

                if (cast.isNotBlank()) {
                    Text(
                        text = "Uloge: $cast",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (genres.isNotBlank()) {
                    Text(genres, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                val dateText = formatDate(date)
                if (dateText.isNotBlank()) {
                    Text(dateText, color = TextSecondary, fontSize = 14.sp)
                }
                val runtimeText = formatRuntime(runtimeMin)
                if (runtimeText.isNotBlank()) {
                    Text(runtimeText, color = TextSecondary, fontSize = 14.sp)
                }
                if (!cert.isNullOrBlank()) {
                    Text(
                        text = cert,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// ================= REDOVI =================

@Composable
private fun PosterRowSection(
    title: String,
    items: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit
) {
    Text(
        text = title,
        color = TextPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 48.dp, bottom = 16.dp)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 48.dp, end = 48.dp)
    ) {
        items(items, key = { it.id }) { movie ->
            PosterCard(movie = movie, onClick = { onMovieClick(movie) })
        }
    }
}

@Composable
private fun PosterCard(movie: TmdbMovie, onClick: () -> Unit) {
    Column(modifier = Modifier.width(150.dp)) {
        TvFocusableButton(
            onClick = onClick,
            modifier = Modifier
                .width(150.dp)
                .height(225.dp)
        ) { focused ->
            val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(220), label = "")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(CardShape)
                    .background(SurfaceBackground)
                    .then(
                        if (focused) Modifier.border(3.dp, Color.White, CardShape)
                        else Modifier
                    )
            ) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w342" + movie.posterPath,
                    contentDescription = movie.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = movie.displayTitle,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp,
            letterSpacing = 0.15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SeasonRowSection(
    seasons: List<TmdbSeason>,
    selectedSeason: Int,
    onSelectSeason: (Int) -> Unit
) {
    Text(
        text = "Sezone",
        color = TextPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 48.dp, bottom = 16.dp)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 48.dp, end = 48.dp)
    ) {
        items(seasons, key = { it.seasonNumber }) { season ->
            SeasonCard(
                season = season,
                selected = season.seasonNumber == selectedSeason,
                onClick = { onSelectSeason(season.seasonNumber) }
            )
        }
    }
}

@Composable
private fun SeasonCard(
    season: TmdbSeason,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.width(150.dp)) {
        TvFocusableButton(
            onClick = onClick,
            modifier = Modifier
                .width(150.dp)
                .height(225.dp)
        ) { focused ->
            val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(220), label = "")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(CardShape)
                    .background(SurfaceBackground)
                    .then(
                        if (focused || selected) Modifier.border(3.dp, Color.White, CardShape)
                        else Modifier
                    )
            ) {
                if (!season.posterPath.isNullOrBlank()) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w342" + season.posterPath,
                        contentDescription = season.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "S${season.seasonNumber}",
                            color = TextSecondary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = season.name?.takeIf { it.isNotBlank() } ?: "${season.seasonNumber}. sezona",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (season.episodeCount > 0) {
            Text(
                text = "${season.episodeCount} ep.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EpisodeRowSection(
    seasonNumber: Int,
    episodes: List<TmdbEpisode>,
    onEpisodeClick: (TmdbEpisode) -> Unit
) {
    Text(
        text = "Sezona $seasonNumber · Epizode",
        color = TextPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 48.dp, bottom = 16.dp)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(start = 48.dp, end = 48.dp)
    ) {
        items(episodes, key = { it.id }) { episode ->
            EpisodeCard(episode = episode, onClick = { onEpisodeClick(episode) })
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: TmdbEpisode,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.width(300.dp)) {
        TvFocusableButton(
            onClick = onClick,
            modifier = Modifier
                .width(300.dp)
                .height(169.dp)
        ) { focused ->
            val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(220), label = "")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(CardShape)
                    .background(SurfaceBackground)
                    .then(
                        if (focused) Modifier.border(3.dp, Color.White, CardShape)
                        else Modifier
                    )
            ) {
                if (!episode.stillPath.isNullOrBlank()) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w300" + episode.stillPath,
                        contentDescription = episode.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "E${episode.episodeNumber}",
                            color = TextSecondary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "S${episode.seasonNumber} · E${episode.episodeNumber}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (focused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = episode.name?.takeIf { it.isNotBlank() } ?: "Epizoda ${episode.episodeNumber}",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (episode.voteAverage > 0) {
                Text(
                    text = "★ %.1f".format(episode.voteAverage),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            val dateText = formatDate(episode.airDate)
            if (dateText.isNotBlank()) {
                Text(dateText, color = TextSecondary, fontSize = 12.sp)
            }
            val runtimeText = formatRuntime(episode.runtime)
            if (runtimeText.isNotBlank()) {
                Text(runtimeText, color = TextSecondary, fontSize = 12.sp)
            }
        }

        val desc = episode.overview ?: ""
        if (desc.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ================= POMOĆNE =================

private fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val output = SimpleDateFormat("d. MMMM yyyy.", Locale("sr"))
        output.format(input.parse(iso)!!)
    } catch (_: Exception) {
        iso
    }
}

private fun formatRuntime(minutes: Int?): String {
    if (minutes == null || minutes <= 0) return ""
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h} h ${m} min" else "${m} min"
}

@Composable
private fun DetailsShimmer() {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val progress by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = ""
    )

    val brush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF111113), Color(0xFF29292D), Color(0xFF111113)),
        startX = -700f + progress * 2400f, endX = progress * 2400f
    )

    Box(modifier = Modifier.fillMaxSize().background(brush))
}

@Composable
private fun DetailsError(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(DetailsBackground),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "⚠", color = Color.White, fontSize = 46.sp)
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = message, color = TextSecondary, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(28.dp))
        TvFocusableButton(onClick = onBack) { focused ->
            val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(180), label = "")
            Box(
                modifier = Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color.White)
                    .padding(horizontal = 30.dp, vertical = 13.dp)
            ) {
                Text("Nazad", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
