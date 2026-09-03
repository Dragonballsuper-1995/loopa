package com.loopa.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loopa.db.MediaItemEntity
import com.loopa.model.TmdbMovie
import com.loopa.ui.components.EpisodeProgressSection
import com.loopa.util.TmdbUrlHelper
import com.loopa.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailSheet(
    initialMovie: TmdbMovie,
    viewModel: MediaViewModel,
    onDismiss: () -> Unit
) {
    // ── History Stack Navigation for Smooth Back Transition ─────────────────
    val history = remember { mutableStateListOf<TmdbMovie>() }

    LaunchedEffect(initialMovie) {
        if (history.isEmpty()) {
            history.add(initialMovie)
        }
    }

    // Intercept back button if user navigated into "More Like This"
    BackHandler(enabled = history.size > 1) {
        history.removeAt(history.lastIndex)
    }

    val currentMovie = history.lastOrNull() ?: initialMovie

    val savedItems by viewModel.savedMediaItems.collectAsState()
    val mediaTypeVal = if (currentMovie.mediaType == "anime") "anime" else currentMovie.mediaType ?: "movie"
    val dbEntry = remember(savedItems, currentMovie.id, mediaTypeVal) {
        savedItems.find { it.id == currentMovie.id && it.mediaType == mediaTypeVal }
    }

    LaunchedEffect(currentMovie.id) {
        viewModel.fetchSimilarItems(currentMovie.id, currentMovie.mediaType)
    }
    val similarItems by viewModel.similarItems.collectAsState()

    var genres by remember(currentMovie.id) { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(currentMovie.id, mediaTypeVal) {
        genres = viewModel.fetchGenres(currentMovie.id, mediaTypeVal)
    }

    var castMembers by remember(currentMovie.id) { mutableStateOf<List<com.loopa.model.TmdbCast>>(emptyList()) }
    LaunchedEffect(currentMovie.id, mediaTypeVal) {
        castMembers = viewModel.fetchCredits(currentMovie.id, mediaTypeVal)
    }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Loopa.Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(Loopa.PillShape)
                    .background(Loopa.BorderMd)
            )
        }
    ) {
        Crossfade(
            targetState = currentMovie,
            animationSpec = tween(300),
            label = "detail_modal_crossfade"
        ) { movie ->
            val title = movie.title ?: movie.name ?: "Unknown"
            val date = movie.releaseDate ?: movie.firstAirDate ?: ""
            val year = date.take(4)
            val backdropUrl = (movie.backdropPath ?: movie.posterPath)?.let {
                TmdbUrlHelper.backdropUrl(it, "w1280")
            }
            val posterUrl = movie.posterPath?.let {
                TmdbUrlHelper.posterUrl(it, "w500")
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
            ) {
                // ── 1. Cinematic Backdrop Header ────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(255.dp)
                    ) {
                        if (backdropUrl != null) {
                            AsyncImage(
                                model = backdropUrl,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Loopa.Raised)
                            )
                        }

                        // 4-zone cinematic gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.00f to Loopa.Base.copy(alpha = 0.45f),
                                            0.22f to Color.Transparent,
                                            0.60f to Loopa.Surface.copy(alpha = 0.75f),
                                            1.00f to Loopa.Surface
                                        )
                                    )
                                )
                        )

                        // Top Action Buttons (Back and Close)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (history.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Loopa.Base.copy(alpha = 0.75f))
                                        .border(1.dp, Loopa.Border, CircleShape)
                                        .clickable {
                                            history.removeAt(history.lastIndex)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Loopa.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(36.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Loopa.Base.copy(alpha = 0.75f))
                                    .border(1.dp, Loopa.Border, CircleShape)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close",
                                    tint = Loopa.TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Title & Tags Overlay at bottom of Backdrop
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            // Metadata Badges Row (★ Rating · Year · Type · 4K UHD · DOLBY)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (movie.voteAverage != null && movie.voteAverage > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(Loopa.PillShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .border(1.dp, Loopa.Gold.copy(alpha = 0.4f), Loopa.PillShape)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Star,
                                                null,
                                                tint = Loopa.Gold,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = String.format("%.1f", movie.voteAverage),
                                                color = Loopa.Gold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (year.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.10f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = year,
                                            color = Loopa.TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                val typeLabel = when (mediaTypeVal) {
                                    "tv" -> "TV"
                                    "anime" -> "ANIME"
                                    else -> "MOVIE"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = typeLabel,
                                        color = Loopa.TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // High-tech 4K UHD badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text("4K UHD", color = Color.White.copy(alpha = 0.9f), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                }

                                // High-tech DOLBY badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(1.dp, Loopa.Cyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text("DOLBY", color = Loopa.Cyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                }

                                // In-List Tracking Badge
                                if (dbEntry != null) {
                                    val isWatching = dbEntry.listName.equals("Watching", ignoreCase = true)
                                    val pillBg = if (isWatching) Loopa.AmberSubtle else Loopa.Surface
                                    val pillBorder = if (isWatching) Loopa.Amber.copy(alpha = 0.4f) else Loopa.Border
                                    val pillText = if (isWatching) Loopa.Amber else Loopa.TextSecondary

                                    Box(
                                        modifier = Modifier
                                            .clip(Loopa.BadgeShape)
                                            .background(pillBg)
                                            .border(1.dp, pillBorder, Loopa.BadgeShape)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "● ${dbEntry.listName}",
                                            color = pillText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            // Large Title
                            Text(
                                text = title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 25.sp,
                                color = Loopa.TextPrimary,
                                lineHeight = 29.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // ── 2. Scrollable Bento Body Content ────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Genre Pills
                        if (genres.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                genres.take(4).forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(Loopa.PillShape)
                                            .background(Loopa.Base)
                                            .border(1.dp, Loopa.Border, Loopa.PillShape)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            fontSize = 11.sp,
                                            color = Loopa.TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Bento Card 1: Synopsis
                        if (!movie.overview.isNullOrBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(Loopa.CardShape)
                                    .background(Loopa.Raised.copy(alpha = 0.4f))
                                    .border(1.dp, Loopa.Border, Loopa.CardShape)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .width(3.dp)
                                            .height(13.dp)
                                            .clip(CircleShape)
                                            .background(Loopa.Amber)
                                    )
                                    Text(
                                        text = "SYNOPSIS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Loopa.Amber,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Text(
                                    text = movie.overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Loopa.TextSecondary,
                                    lineHeight = 20.sp,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Bento Card 2: Experience Specs (3 Columns)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Loopa.CardShape)
                                .background(Loopa.Raised.copy(alpha = 0.4f))
                                .border(1.dp, Loopa.Border, Loopa.CardShape)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("VIDEO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Loopa.TextMuted)
                                Spacer(Modifier.height(3.dp))
                                Text("4K UHD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Loopa.TextPrimary)
                            }
                            Box(Modifier.width(1.dp).height(28.dp).background(Loopa.Border))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("AUDIO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Loopa.TextMuted)
                                Spacer(Modifier.height(3.dp))
                                Text("Atmos 5.1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Loopa.Cyan)
                            }
                            Box(Modifier.width(1.dp).height(28.dp).background(Loopa.Border))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ASPECT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Loopa.TextMuted)
                                Spacer(Modifier.height(3.dp))
                                Text("2.39:1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Loopa.TextPrimary)
                            }
                        }

                        // Bento Card 3: Cast & Key Roles (Apple TV Circular Avatars)
                        if (castMembers.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(Loopa.CardShape)
                                    .background(Loopa.Raised.copy(alpha = 0.4f))
                                    .border(1.dp, Loopa.Border, Loopa.CardShape)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .width(3.dp)
                                            .height(13.dp)
                                            .clip(CircleShape)
                                            .background(Loopa.Amber)
                                    )
                                    Text(
                                        text = "CAST & KEY ROLES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Loopa.TextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(castMembers.take(10)) { cast ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(68.dp)
                                        ) {
                                            val profileUrl = cast.profilePath?.let {
                                                TmdbUrlHelper.posterUrl(it, "w185")
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(Loopa.Base)
                                                    .border(1.dp, Loopa.Border, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (profileUrl != null) {
                                                    AsyncImage(
                                                        model = profileUrl,
                                                        contentDescription = cast.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Text(
                                                        text = cast.name.take(1),
                                                        color = Loopa.TextSecondary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = cast.name,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Loopa.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!cast.character.isNullOrBlank()) {
                                                Text(
                                                    text = cast.character,
                                                    fontSize = 8.sp,
                                                    color = Loopa.TextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── Bento Card 4: Watchlist Control Hub ─────────────
                        if (dbEntry == null) {
                            // Primary Add To List CTA
                            LoopButton(
                                text = "Add to List",
                                onClick = {
                                    viewModel.addMediaItem(
                                        id = movie.id,
                                        title = title,
                                        imageUrl = posterUrl ?: backdropUrl,
                                        date = date,
                                        score = movie.voteAverage,
                                        listName = "Watching",
                                        mediaType = mediaTypeVal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // Unified User Control Card (Status + Rating + Notes)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(Loopa.CardShape)
                                    .background(Loopa.Surface)
                                    .border(1.dp, Loopa.Border, Loopa.CardShape)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Status Selector Pills (Watching | Watched)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val isWatching = dbEntry.listName.equals("Watching", ignoreCase = true)
                                    val isWatched = dbEntry.listName.equals("Watched", ignoreCase = true)

                                    // Watching Button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(Loopa.PillShape)
                                            .background(if (isWatching) Loopa.AmberSubtle else Loopa.Base)
                                            .border(
                                                1.dp,
                                                if (isWatching) Loopa.Amber.copy(alpha = 0.5f) else Loopa.Border,
                                                Loopa.PillShape
                                            )
                                            .clickable {
                                                if (!isWatching) {
                                                    viewModel.updateMediaItem(dbEntry.copy(listName = "Watching"), oldItem = dbEntry)
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Watching",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isWatching) Loopa.Amber else Loopa.TextSecondary
                                        )
                                    }

                                    // Watched Button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(Loopa.PillShape)
                                            .background(if (isWatched) Loopa.Raised else Loopa.Base)
                                            .border(
                                                1.dp,
                                                if (isWatched) Loopa.BorderMd else Loopa.Border,
                                                Loopa.PillShape
                                            )
                                            .clickable {
                                                if (!isWatched) {
                                                    viewModel.updateMediaItem(dbEntry.copy(listName = "Watched"), oldItem = dbEntry)
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Watched",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isWatched) Loopa.TextPrimary else Loopa.TextSecondary
                                        )
                                    }
                                }

                                HorizontalDivider(color = Loopa.Border)

                                // Your Rating Section
                                var currentRating by remember(dbEntry.userRating) {
                                    mutableIntStateOf(dbEntry.userRating ?: 0)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = Loopa.Amber,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Your Rating",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Loopa.TextPrimary
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (currentRating > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(Loopa.BadgeShape)
                                                    .background(Loopa.AmberSubtle)
                                                    .border(1.dp, Loopa.Amber.copy(alpha = 0.3f), Loopa.BadgeShape)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "$currentRating/10",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Loopa.Amber
                                                )
                                            }
                                        }

                                        // 5 Interactive Stars
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            for (i in 1..5) {
                                                val starScore = i * 2
                                                val isActive = currentRating >= starScore - 1
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = "Rating $starScore",
                                                    tint = if (isActive) Loopa.Amber else Loopa.Raised,
                                                    modifier = Modifier
                                                        .size(17.dp)
                                                        .clickable {
                                                            val newRating = if (currentRating == starScore) 0 else starScore
                                                            currentRating = newRating
                                                            viewModel.updateMediaItem(dbEntry.copy(userRating = if (newRating > 0) newRating else null))
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = Loopa.Border)

                                // Personal Notes Section
                                var notesText by remember(dbEntry.personalNotes) {
                                    mutableStateOf(dbEntry.personalNotes ?: "")
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "PERSONAL NOTES",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Loopa.TextMuted,
                                            letterSpacing = 0.5.sp
                                        )

                                        if (notesText != (dbEntry.personalNotes ?: "")) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(Loopa.BadgeShape)
                                                    .background(Loopa.Amber)
                                                    .clickable {
                                                        viewModel.updateMediaItem(dbEntry.copy(personalNotes = notesText.trim()))
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "Save",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Loopa.Base
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = notesText,
                                        onValueChange = { notesText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = {
                                            Text("Add personal notes, thoughts...", color = Loopa.TextMuted, fontSize = 12.sp)
                                        },
                                        shape = Loopa.InputShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Loopa.Base,
                                            unfocusedContainerColor = Loopa.Base,
                                            focusedBorderColor = Loopa.Amber.copy(alpha = 0.4f),
                                            unfocusedBorderColor = Loopa.Border,
                                            focusedTextColor = Loopa.TextPrimary,
                                            unfocusedTextColor = Loopa.TextPrimary,
                                            cursorColor = Loopa.Amber
                                        ),
                                        minLines = 2,
                                        maxLines = 4
                                    )
                                }

                                // Remove from My List Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(Loopa.PillShape)
                                        .background(Loopa.Error.copy(alpha = 0.10f))
                                        .border(1.dp, Loopa.Error.copy(alpha = 0.30f), Loopa.PillShape)
                                        .clickable {
                                            viewModel.removeMediaItem(dbEntry.id, dbEntry.mediaType)
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = null,
                                            tint = Loopa.Error,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Remove from My List",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Loopa.Error
                                        )
                                    }
                                }
                            }
                        }

                        // ── Bento Card 5: Season / Episode Progress (TV & Anime) ────
                        if (mediaTypeVal == "tv" || mediaTypeVal == "anime") {
                            if (dbEntry != null) {
                                EpisodeProgressSection(
                                    mediaId = dbEntry.id,
                                    mediaType = dbEntry.mediaType,
                                    totalSeasons = dbEntry.totalSeasons ?: 1,
                                    totalEpisodes = dbEntry.totalEpisodes ?: 0,
                                    currentSeason = dbEntry.currentSeason ?: 1,
                                    viewModel = viewModel,
                                    onSeasonChange = { newSeason ->
                                        viewModel.updateMediaItem(dbEntry.copy(currentSeason = newSeason))
                                    }
                                )
                            }
                        }

                        // ── Bento Card 6: More Like This Row ────────────────
                        if (similarItems.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    Modifier
                                        .width(3.dp)
                                        .height(14.dp)
                                        .clip(CircleShape)
                                        .background(Loopa.Amber)
                                )
                                Text(
                                    text = "More Like This",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Loopa.TextPrimary
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(similarItems.take(8)) { simMovie ->
                                    val simImg = simMovie.posterPath?.let {
                                        TmdbUrlHelper.posterUrl(it, "w342")
                                    }
                                    HomePosterCard(
                                        title = simMovie.title ?: simMovie.name ?: "Unknown",
                                        imageUrl = simImg,
                                        mediaType = simMovie.mediaType ?: "movie",
                                        onClick = {
                                            history.add(simMovie)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
