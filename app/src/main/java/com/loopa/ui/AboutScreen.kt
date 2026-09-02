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
fun AboutScreen(
    navController: NavController
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About Loopa",
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
            // App Branding Hero Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Loopa.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Amber.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Loopa.Amber.copy(alpha = 0.15f))
                                .border(1.dp, Loopa.Amber.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayCircle,
                                contentDescription = "Loopa Logo",
                                tint = Loopa.Amber,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Loopa",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Loopa.TextPrimary
                        )

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Loopa.Amber.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Amber.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Version 2.3",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Loopa.Amber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "Your Personal Media Universe",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Loopa.TextSecondary
                        )
                    }
                }
            }

            // Mission Statement Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Loopa.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Loopa.Amber))
                            Text(
                                text = "Our Mission",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Loopa.TextPrimary
                            )
                        }
                        Text(
                            text = "Loopa was created to make tracking your favorite movies, series, and anime effortless and distraction-free. No intrusive ads, no clunky interfaces — just a fast and elegant home for everything you love to watch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Loopa.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Core Features
            item {
                LoopSectionHeader(
                    title = "Key Features",
                    highlightPrefix = "Key Features",
                    showDivider = false
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureHighlightRow(
                        icon = Icons.Filled.Search,
                        title = "Radar Search",
                        description = "Instant global search across TMDB, AniList, and Kitsu."
                    )
                    FeatureHighlightRow(
                        icon = Icons.Filled.List,
                        title = "My List & Progress",
                        description = "Organize Watching, Planned, and Watched with 1-tap episode logging."
                    )
                    FeatureHighlightRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "AI 'For You'",
                        description = "Curated recommendations that adapt to your taste without annoying algorithms."
                    )
                }
            }

            // Open Data & Technology Credits
            item {
                LoopSectionHeader(
                    title = "Data & Technology Credits",
                    highlightPrefix = "Data & Technology",
                    showDivider = false
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TechCreditItem(title = "TMDB", subtitle = "Movie & TV metadata, backdrops, and cast")
                    TechCreditItem(title = "AniList & Kitsu", subtitle = "Comprehensive anime schedules and posters")
                    TechCreditItem(title = "Supabase", subtitle = "Encrypted cloud database & real-time sync")
                    TechCreditItem(title = "Jetpack Compose", subtitle = "Modern native declarative Android UI")
                }
            }

            // Developer & Community Links
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Loopa.Raised,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Community & Support",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Loopa.TextPrimary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LoopButton(
                                text = "GitHub",
                                onClick = {
                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/Dragonballsuper-1995/loopa")
                                    )
                                    try {
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) { }
                                },
                                isSecondary = true,
                                leadingIcon = {
                                    Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.weight(1f)
                            )

                            LoopButton(
                                text = "Contact",
                                onClick = {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:support@loopa.app")
                                        putExtra(Intent.EXTRA_SUBJECT, "Hello from Loopa User")
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(emailIntent, "Contact Us"))
                                    } catch (_: Exception) { }
                                },
                                isSecondary = false,
                                leadingIcon = {
                                    Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Loopa.Amber.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Loopa.Amber,
                    modifier = Modifier.size(18.dp)
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

@Composable
private fun TechCreditItem(
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Loopa.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Loopa.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Loopa.TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
