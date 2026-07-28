package com.loopa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loopa.db.MediaItemEntity
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditMediaDialog(
    item: MediaItemEntity,
    onDismiss: () -> Unit,
    onSave: (MediaItemEntity) -> Unit,
    onDelete: () -> Unit,
    genres: List<String> = emptyList()   // fetched from TMDB in the hosting screen
) {
    var watchStatus by remember {
        mutableStateOf(
            when (item.listName) {
                "To Watch", "Want" -> "To Watch"
                "Watching", "Active" -> "Watching"
                else -> "Watched"
            }
        )
    }
    var seasonInput by remember { mutableStateOf(item.currentSeason.toString()) }
    var episodeInput by remember { mutableStateOf(item.currentEpisode.toString()) }
    var rating by remember { mutableStateOf(item.userRating?.toFloat() ?: 0f) }
    var notes by remember { mutableStateOf(item.personalNotes ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(Loopa.DialogShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Loopa.Surface)
                    .border(1.dp, Loopa.Border, Loopa.DialogShape)
            ) {
                // Amber top accent bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Brush.horizontalGradient(listOf(Loopa.Amber, Loopa.AmberStrong)))
                )

                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Bar with Red Delete Button on the Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Loopa.TextPrimary,
                            maxLines = 2,
                            lineHeight = 24.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(10.dp))

                        // Red Delete Button — right aligned with title
                        Row(
                            modifier = Modifier
                                .clip(Loopa.PillShape)
                                .background(Loopa.Error.copy(alpha = 0.12f))
                                .border(1.dp, Loopa.Error.copy(alpha = 0.35f), Loopa.PillShape)
                                .clickable(onClick = onDelete)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Delete, "Delete", tint = Loopa.Error, modifier = Modifier.size(14.dp))
                            Text("Delete", color = Loopa.Error, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = Loopa.Border)

                    // ── Genre chips ────────────────────────────────────────
                    if (genres.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement   = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            genres.forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Loopa.Raised)
                                        .border(1.dp, Loopa.Border, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        color = Loopa.TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Loopa.Border)
                    }

                    // ── Watch Status ───────────────────────────────────────
                    Column {
                        SectionLabel("Watch Status")
                        Spacer(Modifier.height(8.dp))

                        val mediaViewModel: com.loopa.viewmodel.MediaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        val statuses = listOf("To Watch", "Watching", "Watched")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            statuses.forEach { status ->
                                val isSelected = watchStatus == status
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(Loopa.PillShape)
                                        .background(if (isSelected) Loopa.Amber else Loopa.Raised)
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.Transparent else Loopa.BorderMd,
                                            Loopa.PillShape
                                        )
                                        .clickable {
                                            watchStatus = status
                                            if (status == "Watched" && (item.mediaType == "tv" || item.mediaType == "anime")) {
                                                mediaViewModel.markAllEpisodesWatched(item.id, item.mediaType, item.totalSeasons ?: 1)
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = status,
                                        color = if (isSelected) Loopa.Base else Loopa.TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // ── Unified User Control Panel (Rating & Notes) ──────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Loopa.CardShape)
                            .background(Loopa.Surface)
                            .border(1.dp, Loopa.Border, Loopa.CardShape)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Rating Header & 5-Star Row
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionLabel("Your Rating")
                                Text(
                                    text = if (rating > 0) "${rating.roundToInt()} / 10" else "Unrated",
                                    fontWeight = FontWeight.Bold,
                                    color = if (rating > 0) Loopa.Amber else Loopa.TextMuted,
                                    fontSize = 13.sp
                                )
                            }

                            // 5 Interactive Star Icons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                for (i in 1..5) {
                                    val starValue = i * 2f
                                    val isActive = rating >= starValue - 1f
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Star $i",
                                        tint = if (isActive) Loopa.Amber else Loopa.Raised,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable {
                                                rating = if (rating == starValue) 0f else starValue
                                            }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Loopa.Border)

                        // Notes Field
                        Column {
                            SectionLabel("Personal Notes")
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                placeholder = { Text("Add notes, favorite quotes, or thoughts…", color = Loopa.TextMuted, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().height(58.dp),
                                shape = Loopa.InputShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Loopa.Raised,
                                    focusedContainerColor   = Loopa.Raised,
                                    unfocusedBorderColor    = Loopa.Border,
                                    focusedBorderColor      = Loopa.Amber,
                                    unfocusedTextColor      = Loopa.TextPrimary,
                                    focusedTextColor        = Loopa.TextPrimary
                                )
                            )
                        }
                    }

                    // ── Season & Episode Progress (TV / Anime only) ─────────────────
                    if (item.mediaType == "tv" || item.mediaType == "anime") {
                        com.loopa.ui.components.EpisodeProgressSection(
                            mediaId = item.id,
                            mediaType = item.mediaType,
                            totalSeasons = item.totalSeasons ?: 1,
                            currentSeason = seasonInput.toIntOrNull() ?: 1,
                            viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                            onSeasonChange = { newSeason ->
                                seasonInput = newSeason.toString()
                            }
                        )
                    }

                    HorizontalDivider(color = Loopa.Border)

                    // ── Action Buttons ─────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LoopButton(text = "Cancel", onClick = onDismiss, isSecondary = true)
                            LoopButton(
                                text = "Save",
                                onClick = {
                                    onSave(
                                        item.copy(
                                            listName = watchStatus,
                                            currentSeason = seasonInput.toIntOrNull() ?: 1,
                                            currentEpisode = episodeInput.toIntOrNull() ?: 0,
                                            userRating = rating.roundToInt(),
                                            personalNotes = notes
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Internal helpers ──────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = Loopa.TextSecondary,
        letterSpacing = 0.4.sp
    )
}
