package com.loopa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.loopa.ui.components.LoopTrackDialog
import com.loopa.db.MediaItemEntity
import com.loopa.model.TmdbMovie
import com.loopa.ui.theme.DmSans
import com.loopa.util.TmdbUrlHelper
import com.loopa.viewmodel.MediaUiState
import com.loopa.viewmodel.MediaViewModel

@Composable
fun HomeScreen(
    navController: androidx.navigation.NavController,
    viewModel: MediaViewModel,
    onSeeAllCategory: ((SeeAllCategory) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val top10Today by viewModel.top10Today.collectAsState()
    val popularMovies by viewModel.popularMovies.collectAsState()
    val popularTv by viewModel.popularTv.collectAsState()
    val topAnime by viewModel.topAnime.collectAsState()
    val topRatedMovies by viewModel.topRatedMovies.collectAsState()
    val upcomingMovies by viewModel.upcomingMovies.collectAsState()
    val topRatedTv by viewModel.topRatedTv.collectAsState()
    val airingTodayTv by viewModel.airingTodayTv.collectAsState()
    val upcomingAnime by viewModel.upcomingAnime.collectAsState()
    val savedItems by viewModel.savedMediaItems.collectAsState()

    val topAnimeMapped = remember(topAnime) {
        topAnime.take(10).map { anime ->
            val imageUrl = anime.images?.jpg?.largeImageUrl ?: anime.images?.jpg?.imageUrl
            TmdbMovie(
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
        }
    }

    val upcomingAnimeMapped = remember(upcomingAnime) {
        upcomingAnime.take(10).map { anime ->
            val imageUrl = anime.images?.jpg?.largeImageUrl ?: anime.images?.jpg?.imageUrl
            TmdbMovie(
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
        }
    }

    var activeTrackMovie by remember { mutableStateOf<TmdbMovie?>(null) }
    var hoverMovie by remember { mutableStateOf<TmdbMovie?>(null) }
    var internalSeeAllCategory by remember { mutableStateOf<SeeAllCategory?>(null) }

    val handleSeeAll = { category: SeeAllCategory ->
        if (onSeeAllCategory != null) {
            onSeeAllCategory(category)
        } else {
            internalSeeAllCategory = category
        }
    }


    if (onSeeAllCategory == null && internalSeeAllCategory != null) {
        SeeAllScreen(
            category = internalSeeAllCategory!!,
            onBack = { internalSeeAllCategory = null },
            viewModel = viewModel
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Loopa.Base)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            // ── 1. Hero Section ────────────────────────────────────────────
            item {
                val heroItems = remember(uiState) {
                    when (val s = uiState) {
                        is MediaUiState.Success -> s.trending.filter { it.backdropPath != null || it.posterPath != null }.take(5)
                        else -> emptyList()
                    }
                }
                HeroCarousel(
                    heroItems = heroItems,
                    viewModel = viewModel,
                    onActiveTrackMovieChange = { activeTrackMovie = it },
                    onHoverMovieChange = { hoverMovie = it }
                )
                Spacer(Modifier.height(24.dp))
            }

            // ── 2. Trending Row ────────────────────────────────────────────
            item {
                LoopSectionHeader(
                    title = "Trending",
                    highlightPrefix = "Trending",
                    showDivider = false,
                    onSeeAll = {
                        val state = uiState
                        if (state is MediaUiState.Success && state.trending.isNotEmpty()) {
                            handleSeeAll(SeeAllCategory("Trending", state.trending))
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                when (val state = uiState) {
                    is MediaUiState.Loading -> LoadingRow()
                    is MediaUiState.Success -> {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.trending.take(10), key = { "trending_${it.id}_${it.mediaType}" }) { movie ->
                                val imageUrl = TmdbUrlHelper.posterUrl(movie.posterPath, "w342")
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
            }

            // ── Top 10 Today Row ─────────────────────────────────
            item {
                LoopSectionHeader(
                    title = "Top 10 Today",
                    highlightPrefix = "Top 10",
                    showDivider = false,
                    onSeeAll = {
                        if (top10Today.isNotEmpty()) {
                            handleSeeAll(SeeAllCategory("Top 10 Today", top10Today))
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (top10Today.isEmpty()) {
                    LoadingRow()
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        itemsIndexed(top10Today, key = { index, movie -> "top10_${movie.id}_$index" }) { index, movie ->
                            val imageUrl = TmdbUrlHelper.posterUrl(movie.posterPath, "w342")
                            Top10PosterCard(
                                rank = index + 1,
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
                Spacer(Modifier.height(28.dp))
            }

            // ── 3. Anime Row (matches Website order) ─────────────────────────
            item {
                LoopSectionHeader(
                    title = "Anime",
                    highlightPrefix = "Anime",
                    showDivider = false,
                    onSeeAll = {
                        if (topAnime.isNotEmpty()) {
                            handleSeeAll(SeeAllCategory("Top Anime", mapAnimeListToTmdb(topAnime)))
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (topAnime.isEmpty()) {
                    LoadingRow()
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(topAnimeMapped, key = { "anime_${it.id}" }) { tmdbEquivalent ->
                            HomePosterCard(
                                title = tmdbEquivalent.title ?: "",
                                imageUrl = tmdbEquivalent.posterPath,
                                mediaType = "anime",
                                onClick = { activeTrackMovie = tmdbEquivalent },
                                onLongPress = { hoverMovie = tmdbEquivalent },
                                onRelease = { hoverMovie = null }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            // ── 4. Popular Movies Row ──────────────────────────────────────
            item {
                LoopSectionHeader(
                    title = "Popular Movies",
                    highlightPrefix = "Popular",
                    showDivider = false,
                    onSeeAll = {
                        if (popularMovies.isNotEmpty()) {
                            handleSeeAll(SeeAllCategory("Popular Movies", popularMovies))
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (popularMovies.isEmpty()) {
                    LoadingRow()
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(popularMovies.take(10), key = { "pop_mov_${it.id}" }) { movie ->
                            val imageUrl = TmdbUrlHelper.posterUrl(movie.posterPath, "w342")
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
            }

            // ── 5. Popular TV Row ──────────────────────────────────────────
            item {
                LoopSectionHeader(
                    title = "Popular TV",
                    highlightPrefix = "Popular",
                    showDivider = false,
                    onSeeAll = {
                        if (popularTv.isNotEmpty()) {
                            handleSeeAll(SeeAllCategory("Popular TV", popularTv))
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (popularTv.isEmpty()) {
                    LoadingRow()
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(popularTv.take(10), key = { "pop_tv_${it.id}" }) { tv ->
                            val imageUrl = TmdbUrlHelper.posterUrl(tv.posterPath, "w342")
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
            }

            // ── 6. Top Rated Movies ───────────────────────────────────────────
            item {
                LoopSectionHeader(
                    title = "Top Rated Movies",
                    highlightPrefix = "Top Rated",
                    showDivider = false,
                    onSeeAll = {
                        if (topRatedMovies.isNotEmpty()) {
                            handleSeeAll(SeeAllCategory("Top Rated Movies", topRatedMovies))
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (topRatedMovies.isEmpty()) {
                    LoadingRow()
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(topRatedMovies.take(10), key = { "top_mov_${it.id}" }) { movie ->
                            val imageUrl = TmdbUrlHelper.posterUrl(movie.posterPath, "w342")
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
            }

            // ── 7. Upcoming Movies ───────────────────────────────────────────
            item {
                LoopSectionHeader(
                    title = "Upcoming Movies",
                    highlightPrefix = "Upcoming",
                    showDivider = false,
                    onSeeAll = {
                        if (upcomingMovies.isNotEmpty()) {
                            handleSeeAll(SeeAllCategory("Upcoming Movies", upcomingMovies))
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (upcomingMovies.isEmpty()) {
                    LoadingRow()
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(upcomingMovies.take(10), key = { "up_mov_${it.id}" }) { movie ->
                            val imageUrl = TmdbUrlHelper.posterUrl(movie.posterPath, "w342")
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
            }

            // ── 8. Top Rated TV ───────────────────────────────────────────
            item {
                LoopSectionHeader(
                    title = "Top Rated TV",
                    highlightPrefix = "Top Rated",
                    showDivider = false,
                    onSeeAll = {
                        if (topRatedTv.isNotEmpty()) {
                            handleSeeAll(SeeAllCategory("Top Rated TV", topRatedTv))
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (topRatedTv.isEmpty()) {
                    LoadingRow()
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(topRatedTv.take(10), key = { "top_tv_${it.id}" }) { tv ->
                            val imageUrl = TmdbUrlHelper.posterUrl(tv.posterPath, "w342")
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
            }

            // ── 9. Airing Today TV ───────────────────────────────────────────
            item {
                LoopSectionHeader(
                    title = "Airing Today TV",
                    highlightPrefix = "Airing Today",
                    showDivider = false,
                    onSeeAll = {
                        if (airingTodayTv.isNotEmpty()) {
                            handleSeeAll(SeeAllCategory("Airing Today TV", airingTodayTv))
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (airingTodayTv.isEmpty()) {
                    LoadingRow()
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(airingTodayTv.take(10), key = { "air_tv_${it.id}" }) { tv ->
                            val imageUrl = TmdbUrlHelper.posterUrl(tv.posterPath, "w342")
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
            }

            // ── 10. Upcoming Anime ───────────────────────────────────────────
            item {
                LoopSectionHeader(
                    title = "Upcoming Anime",
                    highlightPrefix = "Upcoming",
                    showDivider = false,
                    onSeeAll = {
                        if (upcomingAnime.isNotEmpty()) {
                            handleSeeAll(SeeAllCategory("Upcoming Anime", mapAnimeListToTmdb(upcomingAnime)))
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (upcomingAnime.isEmpty()) {
                    LoadingRow()
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(upcomingAnimeMapped, key = { "up_anime_${it.id}" }) { tmdbEquivalent ->
                            HomePosterCard(
                                title = tmdbEquivalent.title ?: "",
                                imageUrl = tmdbEquivalent.posterPath,
                                mediaType = "anime",
                                onClick = { activeTrackMovie = tmdbEquivalent },
                                onLongPress = { hoverMovie = tmdbEquivalent },
                                onRelease = { hoverMovie = null }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(160.dp))
            }
        } // end LazyColumn

        // ── Track Dialog ───────────────────────────────────────────────────
        // Pause/resume hero carousel based on dialog state
        LaunchedEffect(activeTrackMovie) {
            viewModel.setDetailOpen(activeTrackMovie != null)
        }

        // ── Unified Media Detail Sheet ──────────────────────────────────────
        activeTrackMovie?.let { movie ->
            MediaDetailSheet(
                initialMovie = movie,
                viewModel = viewModel,
                onDismiss = {
                    activeTrackMovie = null
                    hoverMovie = null
                }
            )
        }
    }
}

private fun mapAnimeListToTmdb(animeList: List<com.loopa.model.JikanAnime>): List<TmdbMovie> {
    return animeList.map { anime ->
        val imageUrl = anime.images?.jpg?.largeImageUrl ?: anime.images?.jpg?.imageUrl
        TmdbMovie(
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
    }
}

// ── HeroCarousel Composable ──────────────────────────────────────────────────
@Composable
fun HeroCarousel(
    heroItems: List<TmdbMovie>,
    viewModel: MediaViewModel,
    onActiveTrackMovieChange: (TmdbMovie) -> Unit,
    onHoverMovieChange: (TmdbMovie?) -> Unit
) {
    // Collect here so only HeroCarousel recomposes when detail sheet opens/closes,
    // not the entire HomeScreen LazyColumn.
    val isDetailOpen by viewModel.isDetailOpen.collectAsState()
    var currentHeroIndex by remember { mutableIntStateOf(0) }

    if (heroItems.isNotEmpty()) {
        LaunchedEffect(heroItems, isDetailOpen) {
            while (true) {
                kotlinx.coroutines.delay(5000)
                // Don't advance carousel while a detail dialog is open
                if (!isDetailOpen) {
                    currentHeroIndex = (currentHeroIndex + 1) % heroItems.size
                }
            }
        }
    }

    val heroItem = heroItems.getOrNull(currentHeroIndex)

    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(540.dp)  // Cinematic Hero Height
            .background(Loopa.Surface)
            .pointerInput(heroItems) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffsetX < -50f && heroItems.isNotEmpty()) {
                            currentHeroIndex = (currentHeroIndex + 1) % heroItems.size
                        } else if (dragOffsetX > 50f && heroItems.isNotEmpty()) {
                            currentHeroIndex = (currentHeroIndex - 1 + heroItems.size) % heroItems.size
                        }
                        dragOffsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffsetX += dragAmount
                    }
                )
            }
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
                        TmdbUrlHelper.backdropUrl(it, "w780")
                    }
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.85f
                        )
                    }

                    // Multi-zone cinematic gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Loopa.Base.copy(alpha = 0.40f),
                                        0.18f to Color.Transparent,
                                        0.50f to Color.Transparent,
                                        0.76f to Loopa.Base.copy(alpha = 0.70f),
                                        1.00f to Loopa.Base
                                    )
                                )
                            )
                    )

                    // Left-side vignette for text contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Loopa.Base.copy(alpha = 0.78f),
                                        0.48f to Color.Transparent
                                    )
                                )
                            )
                    )

                    // Content overlay — bottom-aligned, left-padded
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 20.dp, end = 40.dp, bottom = 24.dp, top = 70.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Metadata row — ★ Rating · Year · Type · 4K UHD · DOLBY
                        val year = (item.releaseDate ?: item.firstAirDate)?.take(4)
                        val mediaType = (item.mediaType ?: if (item.name != null) "TV" else "MOVIE").uppercase()
                        val rating = item.voteAverage?.takeIf { it > 0.0 }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            if (rating != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(Loopa.PillShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .border(1.dp, Loopa.Gold.copy(alpha = 0.4f), Loopa.PillShape)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = Loopa.Gold,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = String.format("%.1f", rating),
                                            color = Loopa.Gold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            if (!year.isNullOrBlank()) {
                                Text(year, color = Loopa.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (mediaType.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(mediaType, color = Loopa.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text("4K UHD", color = Color.White.copy(alpha = 0.9f), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Loopa.Cyan.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text("DOLBY", color = Loopa.Cyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        // Hero Title — 44sp ExtraBold, tight tracking
                        Text(
                            text = item.title ?: item.name ?: "Loopa",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 44.sp,
                            color = Loopa.TextPrimary,
                            lineHeight = 48.sp,
                            letterSpacing = (-1.5).sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Synopsis Overview Snippet
                        if (!item.overview.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = item.overview,
                                color = Loopa.TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Dual CTA: Primary Track + Ghost More Info
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LoopButton(
                                text = "Track This",
                                onClick = { onActiveTrackMovieChange(item) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                            LoopButton(
                                text = "More Info",
                                isSecondary = true,
                                onClick = { onActiveTrackMovieChange(item) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = Loopa.TextPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        // Carousel pagination dots
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            heroItems.indices.forEach { i ->
                                val isActive = i == currentHeroIndex
                                Box(
                                    Modifier
                                        .height(6.dp)
                                        .width(if (isActive) 26.dp else 6.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            if (isActive) Loopa.Amber
                                            else Loopa.TextMuted.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Small helper composables ──────────────────────────────────────────────────

@Composable
fun LoadingRow() {
    Box(modifier = Modifier.fillMaxWidth().height(195.dp), contentAlignment = Alignment.Center) {
        Text("Loading…", color = Loopa.TextMuted, fontSize = 13.sp)
    }
}

@Composable
fun ErrorRow(msg: String) {
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

// Top 10 Poster Card with giant monumental rank number & Cineby ribbon
@Composable
fun Top10PosterCard(
    rank: Int,
    title: String,
    imageUrl: String?,
    mediaType: String,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onRelease: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.width(172.dp)
    ) {
        // Monumental 3D Rank Numeral Layered Behind Poster
        val rankColor = if (rank <= 3) Loopa.Amber else Loopa.TextMuted.copy(alpha = 0.38f)
        Text(
            text = rank.toString(),
            fontSize = 112.sp,
            fontWeight = FontWeight.Black,
            fontFamily = DmSans,
            color = rankColor,
            lineHeight = 100.sp,
            letterSpacing = (-8).sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-6).dp, y = 14.dp)
        )

        // Poster Card with Cineby-style Top Rank Ribbon
        Box(
            modifier = Modifier
                .padding(start = 44.dp)
                .zIndex(1f)
        ) {
            HomePosterCard(
                title = title,
                imageUrl = imageUrl,
                mediaType = mediaType,
                onClick = onClick,
                onLongPress = onLongPress,
                onRelease = onRelease
            )

            // Cineby-style TOP 01 Ribbon at top-left of poster
            val ribbonText = if (rank < 10) "TOP 0$rank" else "TOP $rank"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomEnd = 8.dp))
                    .background(
                        if (rank <= 3) Brush.linearGradient(listOf(Loopa.Amber, Loopa.Terracotta))
                        else Brush.linearGradient(listOf(Loopa.Raised, Loopa.Surface))
                    )
                    .border(
                        1.dp,
                        if (rank <= 3) Loopa.Amber.copy(alpha = 0.5f) else Loopa.Border,
                        RoundedCornerShape(topStart = 16.dp, bottomEnd = 8.dp)
                    )
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = ribbonText,
                    color = if (rank <= 3) Color.White else Loopa.TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
