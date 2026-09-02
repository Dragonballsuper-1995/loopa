package com.loopa.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class FaqItem(
    val id: String,
    val icon: ImageVector,
    val question: String,
    val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpFeedbackScreen(
    navController: NavController
) {
    val context = LocalContext.current
    var expandedFaqId by remember { mutableStateOf<String?>(null) }

    val faqs = remember {
        listOf(
            FaqItem(
                id = "free",
                icon = Icons.Filled.AccountBalanceWallet,
                question = "Is Loopa completely free?",
                answer = "Yes! Loopa is 100% free with no ads, paywalls, or hidden subscriptions."
            ),
            FaqItem(
                id = "offline",
                icon = Icons.Filled.WifiOff,
                question = "Does Loopa work offline?",
                answer = "Yes! Your watchlist is stored right on your device. Any changes made offline will automatically sync once you reconnect to the internet."
            ),
            FaqItem(
                id = "ai",
                icon = Icons.Filled.AutoAwesome,
                question = "How do AI recommendations work?",
                answer = "In the 'For You' tab, our AI analyzes the genres, movies, and series in your watchlist to find personalized gems you will love."
            ),
            FaqItem(
                id = "export",
                icon = Icons.Filled.FileDownload,
                question = "Can I export or backup my watchlist?",
                answer = "Yes! Go to Settings -> Data Portability to export your full list as a JSON or CSV file anytime."
            ),
            FaqItem(
                id = "sync",
                icon = Icons.Filled.Sync,
                question = "How do I sync between phone and web?",
                answer = "Simply sign in with the same email account on both your Android app and the Loopa website."
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help & Feedback",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Loopa.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Loopa.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Loopa.Base
                )
            )
        },
        containerColor = Loopa.Base
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
        ) {
            // Header Intro Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Loopa.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Loopa.Amber.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Help,
                                    contentDescription = null,
                                    tint = Loopa.Amber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "How can we help?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Loopa.TextPrimary
                            )
                        }
                        Text(
                            text = "Find quick solutions, explore how features work, or send us a message directly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Loopa.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Getting Started Section
            item {
                LoopSectionHeader(
                    title = "Getting Started in 3 Steps",
                    highlightPrefix = "Getting Started",
                    showDivider = false
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StarterStepItem(
                        number = "1",
                        title = "Search Anything",
                        description = "Tap Radar at the bottom to find any movie, TV show, or anime instantly."
                    )
                    StarterStepItem(
                        number = "2",
                        title = "Add to My List",
                        description = "Pick Watching, Plan to Watch, or Watched to save titles to your vault."
                    )
                    StarterStepItem(
                        number = "3",
                        title = "Track & Sync",
                        description = "Log watched episodes with one tap and sync across your devices."
                    )
                }
            }

            // FAQ Section
            item {
                LoopSectionHeader(
                    title = "Frequently Asked Questions",
                    highlightPrefix = "Frequently Asked",
                    showDivider = false
                )
            }

            items(faqs.size) { index ->
                val faq = faqs[index]
                val isExpanded = expandedFaqId == faq.id
                val arrowRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "arrow_rotation"
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Loopa.Surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isExpanded) Loopa.Amber.copy(alpha = 0.35f) else Loopa.Border
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            expandedFaqId = if (isExpanded) null else faq.id
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    faq.icon,
                                    contentDescription = null,
                                    tint = Loopa.Amber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = faq.question,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Loopa.TextPrimary
                                )
                            }
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = Loopa.TextMuted,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(arrowRotation)
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp, start = 28.dp)) {
                                HorizontalDivider(color = Loopa.Border, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = faq.answer,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Loopa.TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quick Troubleshooting
            item {
                LoopSectionHeader(
                    title = "Quick Troubleshooting",
                    highlightPrefix = "Quick Troubleshooting",
                    showDivider = false
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Loopa.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.SyncProblem, contentDescription = null, tint = Loopa.Amber, modifier = Modifier.size(16.dp))
                                Text("Sync Issue?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Loopa.TextPrimary)
                            }
                            Text(
                                "Go to Settings and tap Sync Data to manually refresh your watchlist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Loopa.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Loopa.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.ImageNotSupported, contentDescription = null, tint = Loopa.Amber, modifier = Modifier.size(16.dp))
                                Text("Poster Issue?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Loopa.TextPrimary)
                            }
                            Text(
                                "Posters come from TMDB and AniList. Check your connection or search alternative titles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Loopa.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Contact & Send Feedback Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Loopa.Raised,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Amber.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Send Us Feedback",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Loopa.TextPrimary
                        )
                        Text(
                            text = "Found a bug, want a new feature, or have a question? Let us know!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Loopa.TextSecondary,
                            lineHeight = 18.sp
                        )

                        LoopButton(
                            text = "Email Support",
                            onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@loopa.app")
                                    putExtra(Intent.EXTRA_SUBJECT, "Loopa Android App Feedback")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "\n\n---\nApp Version: 2.3\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}"
                                    )
                                }
                                try {
                                    context.startActivity(Intent.createChooser(emailIntent, "Send Feedback"))
                                } catch (_: Exception) { }
                            },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterStepItem(
    number: String,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Loopa.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Loopa.Amber.copy(alpha = 0.15f))
                    .border(1.dp, Loopa.Amber.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontWeight = FontWeight.Bold,
                    color = Loopa.Amber,
                    fontSize = 14.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Loopa.TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Loopa.TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
