package com.loopa.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loopa.model.TmdbSeasonResponse
import com.loopa.ui.Loopa
import com.loopa.viewmodel.MediaViewModel

@Composable
fun EpisodeProgressSection(
    mediaId: Int,
    mediaType: String,
    totalSeasons: Int,
    totalEpisodes: Int,
    currentSeason: Int,
    viewModel: MediaViewModel,
    onSeasonChange: (Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedSeason by remember(mediaId) { mutableStateOf(currentSeason) }
    var seasonData by remember(mediaId, selectedSeason) { mutableStateOf<TmdbSeasonResponse?>(null) }
    var isLoading by remember(mediaId, selectedSeason) { mutableStateOf(true) }
    
    val watchedEpisodes by viewModel.getWatchedEpisodesForMedia(mediaId, mediaType).collectAsState(initial = emptyList())

    LaunchedEffect(mediaId, selectedSeason) {
        isLoading = true
        if (totalSeasons == 0 || totalEpisodes == 0) {
            viewModel.repairMediaItem(mediaId, mediaType)
        }
        seasonData = viewModel.fetchTvSeasonDetails(mediaId, selectedSeason)
        isLoading = false
    }

    val episodesInSeason = seasonData?.episodes ?: emptyList()
    val watchedInCurSeasonCount = watchedEpisodes.count { it.seasonNumber == selectedSeason }
    val totalEpCount = episodesInSeason.size
    val watchedAllTimeCount = watchedEpisodes.size

    val seasonProgressFraction = if (totalSeasons > 0) (selectedSeason.toFloat() / totalSeasons.toFloat()) else 0f
    val epProgressFraction = if (totalEpisodes > 0) (watchedAllTimeCount.toFloat() / totalEpisodes.toFloat()).coerceIn(0f, 1f) else 0f
    val epPercentage = (epProgressFraction * 100).toInt()

    var selectedChunk by remember(selectedSeason) { mutableStateOf(0) }
    val chunkSize = 50
    val totalChunks = (episodesInSeason.size + chunkSize - 1) / chunkSize
    val currentEpisodes = if (totalChunks > 1) {
        episodesInSeason.drop(selectedChunk * chunkSize).take(chunkSize)
    } else {
        episodesInSeason
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Loopa.CardShape)
            .background(Loopa.Surface)
            .border(1.dp, Loopa.Border, Loopa.CardShape)
            .padding(14.dp)
    ) {
        // ── 1. Collapsible Header Row with SVG-style Canvas Progress Rings ───────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Progress Tracker",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Loopa.TextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Season Ring Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Loopa.Base)
                        .border(1.dp, Loopa.Border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        drawCircle(color = Color.White.copy(alpha = 0.1f), style = Stroke(width = 3.dp.toPx()))
                        drawArc(
                            color = Loopa.TextSecondary,
                            startAngle = -90f,
                            sweepAngle = 360f * seasonProgressFraction,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                    Text(
                        text = "S $selectedSeason/$totalSeasons",
                        color = Loopa.TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Episode Ring Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Loopa.Base)
                        .border(1.dp, Loopa.Amber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        drawCircle(color = Loopa.Amber.copy(alpha = 0.15f), style = Stroke(width = 3.dp.toPx()))
                        drawArc(
                            color = Loopa.Amber,
                            startAngle = -90f,
                            sweepAngle = 360f * epProgressFraction,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                    Text(
                        text = "TOTAL $watchedAllTimeCount/${if (totalEpisodes > 0) totalEpisodes else "?"}",
                        color = Loopa.TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = Loopa.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── 2. Expanded Dashboard & Checklist ────────────────────────────────
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dual Visual Progress Dashboard Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Loopa.CardShape)
                        .background(Loopa.Base.copy(alpha = 0.6f))
                        .border(1.dp, Loopa.Border, Loopa.CardShape)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Season Progress Segment Bar
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SEASON PROGRESS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Loopa.TextMuted)
                            Text("S$selectedSeason of $totalSeasons", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Loopa.TextPrimary)
                        }
                        Row(modifier = Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val maxDisplay = totalSeasons.coerceAtLeast(1)
                            for (s in 1..maxDisplay) {
                                val isCur = s == selectedSeason
                                val isPastWatched = watchedEpisodes.any { it.seasonNumber == s }
                                val barColor = when {
                                    isCur -> Loopa.Amber
                                    isPastWatched -> Loopa.Amber.copy(alpha = 0.4f)
                                    else -> Loopa.Raised
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(barColor)
                                )
                            }
                        }
                    }

                    // Episode Progress Gradient Bar
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("OVERALL PROGRESS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Loopa.Amber)
                            Text("$watchedAllTimeCount / ${if (totalEpisodes > 0) totalEpisodes else "?"} ($epPercentage%)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Loopa.TextPrimary)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Loopa.Raised)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(epProgressFraction)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Brush.horizontalGradient(listOf(Loopa.Amber, Loopa.AmberStrong)))
                            )
                        }
                    }
                }

                // Season Selector Chips
                if (totalSeasons > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items((1..totalSeasons).toList()) { s ->
                            val isSelected = s == selectedSeason
                            Box(
                                modifier = Modifier
                                    .clip(Loopa.PillShape)
                                    .background(if (isSelected) Loopa.AmberSubtle else Loopa.Raised)
                                    .border(
                                        1.dp,
                                        if (isSelected) Loopa.Amber.copy(alpha = 0.4f) else Loopa.Border,
                                        Loopa.PillShape
                                    )
                                    .clickable {
                                        selectedSeason = s
                                        onSeasonChange(s)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Season $s",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Loopa.Amber else Loopa.TextSecondary
                                )
                            }
                        }
                    }
                }

                if (episodesInSeason.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Episodes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Loopa.TextPrimary
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Loopa.Amber.copy(alpha = 0.15f))
                                .clickable {
                                    val episodeNumbers = episodesInSeason.map { it.episodeNumber }
                                    viewModel.markSeasonWatched(mediaId, mediaType, selectedSeason, episodeNumbers, totalEpisodes)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = "Mark Season", tint = Loopa.Amber, modifier = Modifier.size(12.dp))
                            Text("Mark Season Watched", color = Loopa.Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Chunk Selector for > 50 episodes
                if (totalChunks > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items((0 until totalChunks).toList()) { chunk ->
                            val isSelected = chunk == selectedChunk
                            val startEp = chunk * chunkSize + 1
                            val endEp = minOf((chunk + 1) * chunkSize, episodesInSeason.size)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Loopa.Raised else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) Loopa.Border else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedChunk = chunk }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$startEp-$endEp",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Loopa.TextPrimary else Loopa.TextSecondary
                                )
                            }
                        }
                    }
                }

                // Episode List
                if (isLoading) {
                    Text("Loading episodes...", color = Loopa.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                } else if (currentEpisodes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        currentEpisodes.forEach { ep ->
                            val isWatched = watchedEpisodes.any { it.seasonNumber == selectedSeason && it.episodeNumber == ep.episodeNumber }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Loopa.Base.copy(alpha = 0.5f))
                                    .border(
                                        1.dp,
                                        if (isWatched) Loopa.Amber.copy(alpha = 0.3f) else Loopa.Border,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        viewModel.toggleEpisodeWatched(mediaId, mediaType, selectedSeason, ep.episodeNumber, !isWatched, totalEpisodes)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isWatched) Loopa.Amber else Loopa.Raised)
                                            .border(
                                                1.dp,
                                                if (isWatched) Loopa.Amber else Loopa.BorderMd,
                                                RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isWatched) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Loopa.Base,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${ep.episodeNumber}. ${ep.name ?: "Episode ${ep.episodeNumber}"}",
                                        color = if (isWatched) Loopa.TextMuted else Loopa.TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        textDecoration = if (isWatched) TextDecoration.LineThrough else null
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text("No episode data available.", color = Loopa.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}
