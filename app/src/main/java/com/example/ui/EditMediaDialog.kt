package com.example.ui

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
import com.example.db.MediaItemEntity
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMediaDialog(
    item: MediaItemEntity,
    onDismiss: () -> Unit,
    onSave: (MediaItemEntity) -> Unit,
    onDelete: () -> Unit
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
                // Amber top accent bar — warm, not orange
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
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Title
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Loopa.TextPrimary,
                        maxLines = 2,
                        lineHeight = 26.sp
                    )

                    HorizontalDivider(color = Loopa.Border)

                    // ── Watch Status ───────────────────────────────────────
                    Column {
                        SectionLabel("Watch Status")
                        Spacer(Modifier.height(10.dp))

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
                                        .clickable { watchStatus = status }
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

                    // ── Season & Episode (TV / Anime only) ─────────────────
                    if (item.mediaType == "tv" || item.mediaType == "anime") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Season
                            Column(modifier = Modifier.weight(1f)) {
                                SectionLabel("Season")
                                Spacer(Modifier.height(8.dp))
                                CounterRow(
                                    value = seasonInput.toIntOrNull() ?: 1,
                                    onDecrease = {
                                        val v = seasonInput.toIntOrNull() ?: 1
                                        if (v > 1) seasonInput = (v - 1).toString()
                                    },
                                    onIncrease = {
                                        val v = seasonInput.toIntOrNull() ?: 1
                                        seasonInput = (v + 1).toString()
                                    }
                                )
                            }
                            // Episode
                            Column(modifier = Modifier.weight(1f)) {
                                SectionLabel("Episode")
                                Spacer(Modifier.height(8.dp))
                                CounterRow(
                                    value = episodeInput.toIntOrNull() ?: 0,
                                    onDecrease = {
                                        val v = episodeInput.toIntOrNull() ?: 0
                                        if (v > 0) episodeInput = (v - 1).toString()
                                    },
                                    onIncrease = {
                                        val v = episodeInput.toIntOrNull() ?: 0
                                        episodeInput = (v + 1).toString()
                                    }
                                )
                            }
                        }
                    }

                    // ── Rating Slider ──────────────────────────────────────
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionLabel("Rating")
                            Text(
                                text = "${rating.roundToInt()} / 10",
                                fontWeight = FontWeight.Bold,
                                color = Loopa.Amber,
                                fontSize = 14.sp
                            )
                        }
                        Slider(
                            value = rating,
                            onValueChange = { rating = it },
                            valueRange = 0f..10f,
                            steps = 9,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Loopa.Amber,
                                activeTrackColor = Loopa.Amber,
                                inactiveTrackColor = Loopa.Raised
                            )
                        )
                    }

                    // ── Notes ──────────────────────────────────────────────
                    Column {
                        SectionLabel("Notes")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Add a note…", color = Loopa.TextMuted) },
                            modifier = Modifier.fillMaxWidth().height(90.dp),
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

                    HorizontalDivider(color = Loopa.Border)

                    // ── Action Buttons ─────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Delete button — error tint
                        Box(
                            modifier = Modifier
                                .clip(Loopa.PillShape)
                                .background(Loopa.Error.copy(alpha = 0.1f))
                                .border(1.dp, Loopa.Error.copy(alpha = 0.4f), Loopa.PillShape)
                                .clickable(onClick = onDelete)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Delete, "Delete", tint = Loopa.Error, modifier = Modifier.size(18.dp))
                        }

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

@Composable
private fun CounterRow(value: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Loopa.CardShape)
            .background(Loopa.Raised)
            .border(1.dp, Loopa.Border, Loopa.CardShape)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Remove, "Decrease", tint = Loopa.TextSecondary, modifier = Modifier.size(16.dp))
        }
        Text(
            text = value.toString(),
            fontWeight = FontWeight.Bold,
            color = Loopa.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Add, "Increase", tint = Loopa.Amber, modifier = Modifier.size(16.dp))
        }
    }
}
