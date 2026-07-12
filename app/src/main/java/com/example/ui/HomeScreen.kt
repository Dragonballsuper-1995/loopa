package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.app.LoopTrackDialog
import com.example.model.TmdbMovie
import com.example.viewmodel.MediaUiState
import com.example.viewmodel.MediaViewModel

@Composable
fun HomeScreen(
    navController: androidx.navigation.NavController,
    viewModel: MediaViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val popularMovies by viewModel.popularMovies.collectAsState()
    val popularTv by viewModel.popularTv.collectAsState()
    val topAnime by viewModel.topAnime.collectAsState()
    val savedItems by viewModel.savedMediaItems.collectAsState()

    var activeTrackMovie by remember { mutableStateOf<TmdbMovie?>(null) }
    var hoverMovie by remember { mutableStateOf<TmdbMovie?>(null) }

    val currentlyWatching = remember(savedItems) {
        savedItems.firstOrNull { it.listName == "Watching" }
    }

    Box(modifier = Modifier.fillMaxSize().background(Loopa.Base)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── 1. Hero Section ────────────────────────────────────────────
            val heroItems = remember(uiState) {
                when (val s = uiState) {
                    is MediaUiState.Success -> s.trending.filter { it.backdropPath != null || it.posterPath != null }.take(5)
                    else -> emptyList()
                }
            }

            var currentHeroIndex by remember { mutableIntStateOf(0) }

            if (heroItems.isNotEmpty()) {
                LaunchedEffect(heroItems) {
                    while (true) {
                        kotlinx.coroutines.delay(5000)
                        currentHeroIndex = (currentHeroIndex + 1) % heroItems.size
                    }
                }
            }

            val heroItem = heroItems.getOrNull(currentHeroIndex)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(Loopa.Surface)
            ) {
                androidx.compose.animation.Crossfade(
                    targetState = heroItem,
                    animationSpec = androidx.compose.animation.core.tween(1000),
                    label = "hero_carousel_crossfade",
                    modifier = Modifier.fillMaxSize()
                ) { item ->
                    if (item != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val imageUrl = (item.backdropPath ?: item.posterPath)?.let {
                                "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w780$it"
                            }
                            if (imageUrl != null) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    alpha = 0.45f
                                )
                            }

                            // Gradient fade to Loopa base — warm, not cold
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.0f to Color.Transparent,
                                                0.55f to Loopa.Base.copy(alpha = 0.6f),
                                                1.0f to Loopa.Base
                                            )
                                        )
                                    )
                            )

                            // Content overlay
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // Continue badge (if currently watching something)
                                if (currentlyWatching != null) {
                                    Row(
                                        modifier = Modifier
                                            .clip(Loopa.PillShape)
                                            .background(Loopa.Surface.copy(alpha = 0.85f))
                                            .border(1.dp, Loopa.Border, Loopa.PillShape)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .clickable { hoverMovie = null },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(Loopa.PillShape)
                                                .background(Loopa.Amber),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                tint = Loopa.Base,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text("Continue:", color = Loopa.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(currentlyWatching.title, color = Loopa.TextSecondary, fontSize = 12.sp, maxLines = 1)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                }

                                // Hero Title
                                Text(
                                    text = item.title ?: item.name ?: "Loopa",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 34.sp,
                                    color = Loopa.TextPrimary,
                                    lineHeight = 38.sp,
                                    maxLines = 2
                                )

                                Spacer(Modifier.height(14.dp))

                                // View Details pill button
                                LoopButton(
                                    text = "View Details",
                                    onClick = { activeTrackMovie = item },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = Loopa.Base,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 2. Trending Row ────────────────────────────────────────────
            LoopSectionHeader(title = "Trending", showDivider = false)
            Spacer(Modifier.height(12.dp))
            when (val state = uiState) {
                is MediaUiState.Loading -> LoadingRow()
                is MediaUiState.Success -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.trending.take(10)) { movie ->
                            val imageUrl = movie.posterPath?.let {
                                "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w342$it"
                            }
                            HomePosterCard(
                                title = movie.title ?: movie.name ?: "Unknown",
                                imageUrl = imageUrl,
                                mediaType = movie.mediaType ?: "movie",
                                onClick = { activeTrackMovie = movie },
                                onLongPress = { hoverMovie = movie },
                                onRelease = { hoverMovie = null }
                            )
                        }
                    }
                }
                is MediaUiState.Error -> ErrorRow("Failed to load trending items")
                else -> {}
            }

            Spacer(Modifier.height(28.dp))

            // ── 3. Popular Movies Row ──────────────────────────────────────
            LoopSectionHeader(title = "Popular Movies", showDivider = false)
            Spacer(Modifier.height(12.dp))
            if (popularMovies.isEmpty()) {
                LoadingRow()
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(popularMovies.take(10)) { movie ->
                        val imageUrl = movie.posterPath?.let {
                            "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w342$it"
                        }
                        HomePosterCard(
                            title = movie.title ?: "Unknown",
                            imageUrl = imageUrl,
                            mediaType = "movie",
                            onClick = { activeTrackMovie = movie },
                            onLongPress = { hoverMovie = movie },
                            onRelease = { hoverMovie = null }
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── 4. Popular TV Row ──────────────────────────────────────────
            LoopSectionHeader(title = "Popular TV", showDivider = false)
            Spacer(Modifier.height(12.dp))
            if (popularTv.isEmpty()) {
                LoadingRow()
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(popularTv.take(10)) { tv ->
                        val imageUrl = tv.posterPath?.let {
                            "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w342$it"
                        }
                        HomePosterCard(
                            title = tv.name ?: tv.title ?: "Unknown",
                            imageUrl = imageUrl,
                            mediaType = "tv",
                            onClick = { activeTrackMovie = tv },
                            onLongPress = { hoverMovie = tv },
                            onRelease = { hoverMovie = null }
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── 5. Top Anime Row ───────────────────────────────────────────
            LoopSectionHeader(title = "Top Anime", showDivider = false)
            Spacer(Modifier.height(12.dp))
            if (topAnime.isEmpty()) {
                LoadingRow()
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(topAnime.take(10)) { anime ->
                        val imageUrl = anime.images?.jpg?.largeImageUrl ?: anime.images?.jpg?.imageUrl
                        val tmdbEquivalent = TmdbMovie(
                            id = anime.malId,
                            title = anime.title,
                            name = anime.title,
                            posterPath = imageUrl,
                            backdropPath = imageUrl,
                            releaseDate = "",
                            firstAirDate = "",
                            voteAverage = anime.score,
                            overview = anime.synopsis ?: "",
                            mediaType = "anime",
                            genreIds = null,
                            popularity = null
                        )
                        HomePosterCard(
                            title = anime.title,
                            imageUrl = imageUrl,
                            mediaType = "anime",
                            onClick = { activeTrackMovie = tmdbEquivalent },
                            onLongPress = { hoverMovie = tmdbEquivalent },
                            onRelease = { hoverMovie = null }
                        )
                    }
                }
            }

            Spacer(Modifier.height(160.dp))
        } // end Column

        // ── Track Dialog ───────────────────────────────────────────────────
        activeTrackMovie?.let { movie ->
            val title = movie.title ?: movie.name ?: "Unknown Title"
            val imageUrl = if (movie.mediaType == "anime") movie.posterPath else {
                movie.backdropPath?.let { "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w500$it" }
                    ?: movie.posterPath?.let { "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w342$it" }
            }
            val date = movie.releaseDate ?: movie.firstAirDate ?: "Unknown Date"
            val mediaTypeStr = when (movie.mediaType) { "tv" -> "SERIES"; "movie" -> "MOVIE"; "anime" -> "ANIME"; else -> "MEDIA" }
            val mediaTypeVal = if (movie.mediaType == "anime") "anime" else movie.mediaType ?: "movie"

            LoopTrackDialog(
                title = title,
                mediaTypeStr = mediaTypeStr,
                overview = movie.overview,
                onDismiss = { activeTrackMovie = null },
                onWatched = {
                    viewModel.addMediaItem(movie.id ?: 0, title, imageUrl, date, movie.voteAverage, "Watched", mediaTypeVal)
                    activeTrackMovie = null
                },
                onToWatch = {
                    viewModel.addMediaItem(movie.id ?: 0, title, imageUrl, date, movie.voteAverage, "To Watch", mediaTypeVal)
                    activeTrackMovie = null
                }
            )
        }

        // ── Hover Quick Preview ────────────────────────────────────────────
        hoverMovie?.let { movie ->
            val title = movie.title ?: movie.name ?: "Unknown"
            val imageUrl = if (movie.mediaType == "anime") movie.posterPath else {
                movie.backdropPath?.let { "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w500$it" }
                    ?: movie.posterPath?.let { "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w342$it" }
            }
            val date = movie.releaseDate ?: movie.firstAirDate
            val mediaTypeVal = movie.mediaType ?: "movie"

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { hoverMovie = null },
                contentAlignment = Alignment.Center
            ) {
                // Loopa quick-view card
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .clip(Loopa.DialogShape)
                        .background(Loopa.Surface)
                        .border(1.dp, Loopa.Border, Loopa.DialogShape)
                        .padding(20.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(Loopa.CardShape)
                            )
                        }

                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Loopa.TextPrimary,
                            lineHeight = 22.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LoopBadge(
                                text = mediaTypeVal.uppercase(),
                                textColor = if (mediaTypeVal == "tv" || mediaTypeVal == "anime") Loopa.Amber else Loopa.TextSecondary,
                                borderColor = if (mediaTypeVal == "tv" || mediaTypeVal == "anime") Loopa.Amber.copy(0.4f) else Loopa.BorderMd
                            )
                            val year = if (!date.isNullOrBlank() && date.length >= 4) date.substring(0, 4) else null
                            if (year != null) Text(year, color = Loopa.TextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            if (movie.voteAverage != null && movie.voteAverage > 0.0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Star, null, tint = Loopa.Amber, modifier = Modifier.size(12.dp))
                                    Text(String.format("%.1f", movie.voteAverage), color = Loopa.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        if (!movie.overview.isNullOrBlank()) {
                            Text(
                                text = movie.overview,
                                fontSize = 12.sp,
                                color = Loopa.TextSecondary,
                                lineHeight = 17.sp,
                                maxLines = 4
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Small helper composables ──────────────────────────────────────────────────

@Composable
private fun LoadingRow() {
    Box(modifier = Modifier.fillMaxWidth().height(195.dp), contentAlignment = Alignment.Center) {
        Text("Loading…", color = Loopa.TextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun ErrorRow(msg: String) {
    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
        Text(msg, color = Loopa.Error, fontSize = 13.sp)
    }
}

@Composable
fun HomePosterCard(
    title: String,
    imageUrl: String?,
    mediaType: String,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onRelease: (() -> Unit)? = null
) {
    LoopPosterCard(
        title = title,
        imageUrl = imageUrl,
        mediaType = mediaType,
        onClick = onClick,
        onLongPress = onLongPress,
        onRelease = onRelease,
        modifier = Modifier.width(130.dp)
    )
}
