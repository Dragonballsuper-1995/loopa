package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AiRecommendationResult
import com.example.viewmodel.MediaViewModel
import androidx.compose.animation.core.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowUpward
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRecommendationsScreen(viewModel: MediaViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isLoading by viewModel.isLoadingAiRecs.collectAsState()
    var inputText by remember { androidx.compose.runtime.mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.fetchAiRecommendations()
    }

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Loopa.Base)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Page Header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoopSectionHeader(
                title = "Loopa AI",
                subtitle = "Discover your next obsession",
                titleSize = 26,
                showDivider = false,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp).weight(1f)
            )
            IconButton(
                onClick = { viewModel.clearAiChat() },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(CircleShape)
                    .background(Loopa.Surface)
                    .border(1.dp, Loopa.Border, CircleShape)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "New Chat", tint = Loopa.TextPrimary, modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider(color = Loopa.Border, modifier = Modifier.padding(horizontal = 16.dp))


        
        // ── Chat Log ───────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(chatHistory) { msg ->
                val isUser = msg.role == "user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        if (isUser) {
                            Text("You", style = MaterialTheme.typography.labelMedium, color = Loopa.TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.size(24.dp).background(Loopa.Surface, CircleShape).border(1.dp, Loopa.Border, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = Loopa.TextSecondary, modifier = Modifier.size(14.dp))
                            }
                        } else {
                            Box(modifier = Modifier.size(24.dp).background(Loopa.Surface, CircleShape).border(1.dp, Loopa.Amber.copy(alpha=0.3f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Loopa.Amber, modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Loopa AI", style = MaterialTheme.typography.labelMedium, color = Loopa.TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isUser) Loopa.Raised.copy(alpha=0.6f) else Loopa.Surface.copy(alpha=0.6f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 16.dp
                                )
                            )
                            .border(
                                1.dp,
                                Loopa.Border,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 16.dp
                                )
                            )
                            .padding(16.dp)
                            .widthIn(max = 300.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = Loopa.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }

                    if (msg.recommendations.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 16.dp)
                        ) {
                            items(msg.recommendations) { rec ->
                                Box(modifier = Modifier.width(300.dp)) {
                                    RecommendationCard(
                                        rec = rec,
                                        onAddToList = {
                                            viewModel.addMediaItem(rec.title.hashCode(), rec.title, rec.imageUrl, rec.releaseYear, null, "To Watch", rec.mediaType.lowercase())
                                            viewModel.showToast("${rec.title} added to To Watch")
                                        },
                                        onAlreadyWatched = {
                                            viewModel.addMediaItem(rec.title.hashCode(), rec.title, rec.imageUrl, rec.releaseYear, null, "Watched", rec.mediaType.lowercase())
                                            viewModel.showToast("${rec.title} marked as Watched")
                                        },
                                        onDismiss = { viewModel.dislikeRecommendation(rec.title) },
                                        onLike = { viewModel.likeRecommendation(rec.title) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (isLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                        Box(modifier = Modifier.size(24.dp).background(Loopa.Surface, CircleShape).border(1.dp, Loopa.Amber.copy(alpha=0.3f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Loopa.Amber, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Thinking...", color = Loopa.Amber, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // ── Chat Input ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 140.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask for recommendations...", color = Loopa.TextSecondary, fontSize = 14.sp) },
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Loopa.Surface,
                    unfocusedContainerColor = Loopa.Surface,
                    unfocusedBorderColor = Loopa.Border,
                    focusedBorderColor = Loopa.Amber.copy(alpha = 0.5f),
                    focusedTextColor = Loopa.TextPrimary,
                    unfocusedTextColor = Loopa.TextPrimary,
                    cursorColor = Loopa.Amber
                ),
                trailingIcon = {
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !isLoading) {
                                    viewModel.sendAiChatMessage(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .background(Loopa.Amber, CircleShape)
                        ) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Send", tint = Loopa.Base, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true
            )
        }
    }
}

@Composable
fun RecommendationCard(
    rec: AiRecommendationResult,
    onAddToList: () -> Unit,
    onAlreadyWatched: () -> Unit,
    onDismiss: () -> Unit,
    onLike: () -> Unit
) {
    LoopCard(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
            // Poster + Info row
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = rec.imageUrl,
                    contentDescription = rec.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp, 120.dp)
                        .clip(Loopa.CardShape)
                        .background(Loopa.Raised)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    LoopBadge(
                        text = rec.mediaType,
                        textColor = Loopa.Amber,
                        borderColor = Loopa.Amber.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = rec.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Loopa.TextPrimary,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${rec.genre ?: ""} • ${rec.releaseYear ?: ""}",
                        fontSize = 12.sp,
                        color = Loopa.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // AI Reasoning box — warm, not neon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(Loopa.CardShape)
                    .background(Loopa.AmberSubtle)
                    .border(1.dp, Loopa.Amber.copy(alpha = 0.2f), Loopa.CardShape)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Star, null, tint = Loopa.Amber, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = rec.reasoning ?: "",
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = Loopa.TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like / Dislike
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.ThumbDown, "Not Interested", tint = Loopa.TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onLike, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.ThumbUp, "Like", tint = Loopa.TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
                // Watched / Add buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LoopButton(text = "Watched", onClick = onAlreadyWatched, isSecondary = true)
                    LoopButton(text = "Add", onClick = onAddToList)
                }
            }
        }
    }
}

// ── Warm loading screen replacing TerminalLoadingScreen ──────────────────────
@Composable
fun LoopLoadingScreen(modifier: Modifier = Modifier) {
    val allLines = remember {
        listOf(
            "Connecting to recommendation engine…",
            "Reading your watchlist…",
            "Analyzing your taste profile…",
            "Scanning interaction history…",
            "Calculating cold-start vectors…",
            "Requesting Gemini sync…",
            "Decoding AI recommendation matrix…",
            "Fetching artwork…",
            "Enriching metadata…",
            "Recommendations ready."
        )
    }

    val displayedLines = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        for (line in allLines) {
            displayedLines.add(line)
            delay(350L)
            if (displayedLines.isNotEmpty()) listState.animateScrollToItem(displayedLines.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Animated amber indicator
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulse_alpha"
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Loopa.Amber.copy(alpha = alpha)))
            Text("Thinking…", color = Loopa.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayedLines) { line ->
                val isHighlight = line.contains("ready") || line.contains("Recommendations")
                Text(
                    text = "· $line",
                    color = if (isHighlight) Loopa.Amber else Loopa.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// Compatibility shim for code that still references TerminalLoadingScreen
@Composable
fun TerminalLoadingScreen(modifier: Modifier = Modifier) = LoopLoadingScreen(modifier)
