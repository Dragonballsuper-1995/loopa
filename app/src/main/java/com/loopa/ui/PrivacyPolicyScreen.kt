package com.loopa.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    navController: NavController
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy",
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
            // Header Banner
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
                                    Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = Loopa.Amber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Your Privacy, Plain and Simple",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Loopa.TextPrimary
                            )
                        }
                        Text(
                            text = "We believe privacy policies should be crystal clear. Here is exactly how Loopa respects your data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Loopa.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Pillar 1: What We Keep
            item {
                PrivacyPillarCard(
                    icon = Icons.Filled.FolderOpen,
                    iconTint = Loopa.Amber,
                    title = "1. What We Keep",
                    points = listOf(
                        "Your email address (only if you create an account to sync).",
                        "Your watchlist (movies, series, and anime you save).",
                        "Your watch progress (episodes logged and personal ratings)."
                    ),
                    footerNote = "If you use Loopa in Guest mode, everything stays on your phone only."
                )
            }

            // Pillar 2: What We NEVER Do
            item {
                PrivacyPillarCard(
                    icon = Icons.Filled.Block,
                    iconTint = Loopa.Error,
                    title = "2. What We NEVER Do",
                    points = listOf(
                        "We never sell or rent your personal information to anyone.",
                        "We never track your activity across other apps or websites.",
                        "We never display third-party advertisements."
                    ),
                    footerNote = "Your watchlist is personal and belongs to you."
                )
            }

            // Pillar 3: Storage & Cloud
            item {
                PrivacyPillarCard(
                    icon = Icons.Filled.Storage,
                    iconTint = Color(0xFF64B5F6),
                    title = "3. Where Your Data Lives",
                    points = listOf(
                        "On your device: Secure local SQLite database for instant offline access.",
                        "In the Cloud: Encrypted Supabase database synced only when you log in."
                    )
                )
            }

            // Pillar 4: Public APIs
            item {
                PrivacyPillarCard(
                    icon = Icons.Filled.CloudSync,
                    iconTint = Color(0xFFBA68C8),
                    title = "4. Public Helpers (APIs)",
                    points = listOf(
                        "TMDB: Provides movie and TV metadata and posters.",
                        "AniList & Kitsu: Provides anime schedules and episode listings."
                    ),
                    footerNote = "No personal user information is ever shared with these providers."
                )
            }

            // Pillar 5: Your Superpowers
            item {
                PrivacyPillarCard(
                    icon = Icons.Filled.VpnKey,
                    iconTint = Loopa.Success,
                    title = "5. You Have Full Control",
                    points = listOf(
                        "1-Click Export: Download your entire watchlist as JSON or CSV anytime.",
                        "Data Deletion: Delete individual items or clear your entire history whenever you wish."
                    )
                )
            }

            // Formal Policy Card
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Loopa.Raised,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Legal Summary",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Loopa.TextSecondary
                        )
                        Text(
                            text = "This policy covers Loopa Android and Loopa Web. All network communication with Supabase and TMDB uses HTTPS/TLS encryption. For inquiries, email privacy@loopa.app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Loopa.TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyPillarCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    points: List<String>,
    footerNote: String? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Loopa.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Loopa.TextPrimary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                points.forEach { point ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = iconTint,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodySmall,
                            color = Loopa.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            if (!footerNote.isNullOrEmpty()) {
                Text(
                    text = footerNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = Loopa.TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
