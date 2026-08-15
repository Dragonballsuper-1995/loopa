package com.loopa.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loopa.db.MediaItemEntity
import com.loopa.model.TmdbMovie
import com.loopa.ui.*
import com.loopa.util.TmdbUrlHelper
import com.loopa.viewmodel.MediaViewModel

// ─────────────────────────────────────────────────────────────────────────────
// 1. RecommendationCardSkeleton
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RecommendationCardSkeleton() {
    LoopCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. RecommendationCard
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecommendationCard(
    movie: TmdbMovie,
    viewModel: MediaViewModel = viewModel(),
    onLongPress: ((TmdbMovie) -> Unit)? = null,
    onRelease: (() -> Unit)? = null
) {
    val title = movie.title ?: movie.name ?: "Unknown Title"
    val imageUrl = movie.backdropPath?.let { TmdbUrlHelper.backdropUrl(it, "w500") }
        ?: movie.posterPath?.let { TmdbUrlHelper.posterUrl(it, "w342") }

    var showDetails by remember { mutableStateOf(false) }
    val isAiMatch = movie.overview?.startsWith("[AI Match]") == true

    LoopPosterCard(
        title = title,
        imageUrl = imageUrl,
        mediaType = movie.mediaType ?: "movie",
        onClick = { showDetails = true },
        onLongPress = { if (onLongPress != null) onLongPress(movie) },
        onRelease = onRelease,
        score = movie.voteAverage,
        isAiMatch = isAiMatch,
        modifier = Modifier.fillMaxWidth()
    )

    if (showDetails) {
        LaunchedEffect(showDetails) {
            viewModel.setDetailOpen(true)
        }
        MediaDetailSheet(
            initialMovie = movie,
            viewModel = viewModel,
            onDismiss = {
                showDetails = false
                viewModel.setDetailOpen(false)
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. MediaItemCard (Saved Watchlist Item)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaItemCard(item: MediaItemEntity, viewModel: MediaViewModel) {
    var showDetails by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }

    val statusLabel = if (item.listName.equals("Watched", ignoreCase = true)) "Watched" else "Watching"

    val progressText = if (item.listName == "Watching" && (item.mediaType == "tv" || item.mediaType == "anime")) {
        "S${item.currentSeason} E${item.currentEpisode}"
    } else {
        item.progressString
    }

    LoopPosterCard(
        title = item.title,
        imageUrl = item.imageUrl,
        mediaType = item.mediaType,
        onClick = { showDetails = true },
        onLongPress = { showQuickAdd = true },
        score = item.score,
        statusLabel = statusLabel,
        progressText = progressText,
        totalEpisodes = item.totalEpisodes ?: 0,
        currentEpisode = item.currentEpisode ?: 0,
        modifier = Modifier.fillMaxWidth()
    )

    if (showDetails) {
        val tmdbMovie = TmdbMovie(
            id = item.id,
            title = item.title,
            name = item.title,
            overview = item.personalNotes ?: "",
            posterPath = item.imageUrl,
            backdropPath = item.imageUrl,
            voteAverage = item.score,
            releaseDate = item.date,
            firstAirDate = item.date,
            mediaType = item.mediaType,
            popularity = null,
            genreIds = null
        )
        MediaDetailSheet(
            initialMovie = tmdbMovie,
            viewModel = viewModel,
            onDismiss = { showDetails = false }
        )
    }

    if (showQuickAdd) {
        LoopQuickActionDialog(
            title = item.title,
            onDismiss = { showQuickAdd = false },
            onWatched = {
                viewModel.updateMediaItem(item.copy(listName = "Watched"))
                showQuickAdd = false
            },
            onToWatch = {
                viewModel.updateMediaItem(item.copy(listName = "Watching"))
                showQuickAdd = false
            },
            onRemove = {
                viewModel.removeMediaItem(item.id, item.mediaType)
                showQuickAdd = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. LoopTrackDialog (used in Discover / HomeScreen)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoopTrackDialog(
    title: String,
    mediaTypeStr: String,
    overview: String?,
    onDismiss: () -> Unit,
    onWatched: () -> Unit,
    onToWatch: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
        ) {
            LoopDialogContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Loopa.TextPrimary,
                        lineHeight = 28.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Media type badge
                    LoopBadge(
                        text = mediaTypeStr,
                        textColor = Loopa.Amber,
                        borderColor = Loopa.Amber.copy(0.4f)
                    )

                    if (!overview.isNullOrBlank()) {
                        Text(
                            text = overview,
                            style = MaterialTheme.typography.bodySmall,
                            color = Loopa.TextSecondary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(color = Loopa.Border)

                    Text(
                        text = "ADD TO LIST",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Loopa.TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LoopButton(
                            text = "Watched",
                            onClick = onWatched,
                            isSecondary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LoopButton(
                            text = "Watching",
                            onClick = onToWatch,
                            isSecondary = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. LoopQuickActionDialog (long-press actions)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoopQuickActionDialog(
    title: String,
    onDismiss: () -> Unit,
    onWatched: () -> Unit,
    onToWatch: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            LoopDialogContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Loopa.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "MOVE TO LIST",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Loopa.TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    HorizontalDivider(color = Loopa.Border)

                    LoopButton(
                        text = "Watched",
                        onClick = onWatched,
                        isSecondary = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LoopButton(
                        text = "Watching",
                        onClick = onToWatch,
                        isSecondary = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (onRemove != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Loopa.PillShape)
                                .background(Loopa.Error.copy(alpha = 0.1f))
                                .border(1.dp, Loopa.Error.copy(alpha = 0.4f), Loopa.PillShape)
                                .clickable(onClick = onRemove)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Remove", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Loopa.Error)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. ResponsiveGrid
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun <T> ResponsiveGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (T) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val columns = if (maxWidth > 600.dp) {
            (maxWidth / 320.dp).toInt().coerceAtLeast(2)
        } else {
            1
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = items.size,
                key = { index -> items[index].hashCode() }
            ) { index ->
                Box(modifier = Modifier.animateItem()) {
                    itemContent(items[index])
                }
            }
        }
    }
}
