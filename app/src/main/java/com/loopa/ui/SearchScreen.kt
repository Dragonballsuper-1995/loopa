package com.loopa.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.loopa.model.TmdbMovie
import com.loopa.ui.components.RecommendationCard
import com.loopa.ui.components.RecommendationCardSkeleton
import com.loopa.util.TmdbUrlHelper
import com.loopa.viewmodel.MediaUiState
import com.loopa.viewmodel.MediaViewModel
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

// ─────────────────────────────────────────────────────────────────────────────
// Discover & Search Screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    navController: NavController,
    viewModel: MediaViewModel = viewModel(),
    hazeState: HazeState? = null
) {
    var query by remember { mutableStateOf("") }
    var hoverMovie by remember { mutableStateOf<TmdbMovie?>(null) }
    val searchState by viewModel.searchState.collectAsState()
    var selectedMediaType by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        if (query.isBlank()) {
            viewModel.search("")
        }
    }

    val localHazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. The Scrollable Content
        Box(modifier = Modifier.fillMaxSize().hazeSource(state = localHazeState)) {
            if (query.isBlank()) {
                HomeScreen(navController = navController, viewModel = viewModel)
            } else {
                when (val state = searchState) {
                    is MediaUiState.Loading -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 170.dp, end = 16.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(6) {
                                RecommendationCardSkeleton()
                            }
                        }
                    }
                    is MediaUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize().padding(top = 180.dp), contentAlignment = Alignment.TopCenter) {
                            Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                        }
                    }
                    is MediaUiState.InsufficientData -> {}
                    is MediaUiState.Success -> {
                        val filteredList = state.trending.filter { movie ->
                            when (selectedMediaType) {
                                "Movies" -> movie.mediaType == "movie"
                                "TV Shows" -> movie.mediaType == "tv"
                                "Anime" -> movie.genreIds?.contains(16) == true || movie.mediaType == "anime"
                                else -> true
                            }
                        }

                        if (filteredList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 180.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Text(
                                    text = "No results found",
                                    color = Loopa.TextMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, top = 170.dp, end = 16.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredList.size) { index ->
                                    val movie = filteredList[index]
                                    RecommendationCard(
                                        movie = movie,
                                        onLongPress = { hoverMovie = it },
                                        onRelease = { hoverMovie = null }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Search bar with progressive blur overlaid on top
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .hazeEffect(state = localHazeState) {
                    blurRadius = 24.dp
                    progressive = HazeProgressive.verticalGradient(
                        startIntensity = 1f,
                        endIntensity = 0f
                    )
                }
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xF50F0E0C),
                            0.45f to Color(0xCC0F0E0C),
                            0.75f to Color(0x660F0E0C),
                            1.0f to Color.Transparent
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Custom search bar (pinned at top)
            RadarSearchBar(
                query = query,
                onQueryChange = {
                    query = it
                    viewModel.search(it)
                },
                placeholder = "Search targets...",
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            )

            if (query.isNotBlank()) {
                // Quick filter pills matching Website
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("All", "Movies", "TV Shows", "Anime").forEach { type ->
                        val isSelected = selectedMediaType == type
                        Box(
                            modifier = Modifier
                                .clip(Loopa.PillShape)
                                .background(if (isSelected) Loopa.Amber else Loopa.Surface)
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else Loopa.Border,
                                    Loopa.PillShape
                                )
                                .clickable { selectedMediaType = type }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) Loopa.Base else Loopa.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. Hover Preview Overlay
        hoverMovie?.let { movie ->
            val title = movie.title ?: movie.name ?: "Unknown"
            val imageUrl = movie.backdropPath?.let { TmdbUrlHelper.backdropUrl(it, "w500") }
                ?: movie.posterPath?.let { TmdbUrlHelper.posterUrl(it, "w342") }
            val date = movie.releaseDate ?: movie.firstAirDate
            val mediaTypeVal = movie.mediaType ?: "movie"

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Loopa.Base.copy(alpha = 0.85f))
                    .clickable { hoverMovie = null },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .clip(Loopa.CardShape)
                        .background(Loopa.Surface)
                        .border(1.dp, Loopa.Border, Loopa.CardShape)
                        .padding(20.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Loopa.TextPrimary,
                            lineHeight = 26.sp
                        )

                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(Loopa.CardShape)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LoopBadge(
                                text = mediaTypeVal,
                                textColor = Loopa.Amber,
                                borderColor = Loopa.Amber.copy(0.4f)
                            )

                            val year = if (!date.isNullOrBlank() && date.length >= 4) {
                                date.substring(0, 4)
                            } else null
                            if (year != null) {
                                Text(
                                    text = year,
                                    color = Loopa.TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            if (movie.voteAverage != null && movie.voteAverage > 0.0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Rating",
                                        tint = Loopa.Amber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = String.format("%.1f", movie.voteAverage),
                                        color = Loopa.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (!movie.overview.isNullOrBlank()) {
                            Text(
                                text = movie.overview,
                                fontSize = 13.sp,
                                color = Loopa.TextSecondary,
                                lineHeight = 18.sp,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Radar Search Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RadarSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onFocusChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(targetValue = if (isFocused) 6.dp else 2.dp, label = "elevation")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(elevation, Loopa.PillShape)
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChange(it.isFocused)
            },
        color = Color(0x66161512),
        shape = Loopa.PillShape,
        border = BorderStroke(
            width = 1.dp,
            color = if (isFocused) Loopa.Amber.copy(alpha = 0.5f) else Color(0x1FADACAB)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search icon",
                tint = if (isFocused) Loopa.Amber else Loopa.TextMuted,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Loopa.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Loopa.Amber),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = Loopa.TextMuted.copy(alpha = 0.7f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Clear",
                        tint = Loopa.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
