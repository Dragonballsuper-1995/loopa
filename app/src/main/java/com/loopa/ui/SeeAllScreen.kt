package com.loopa.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loopa.model.TmdbMovie
import com.loopa.ui.components.RecommendationCard
import com.loopa.viewmodel.MediaViewModel

data class SeeAllCategory(
    val title: String,
    val items: List<TmdbMovie>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeAllScreen(
    category: SeeAllCategory,
    onBack: () -> Unit,
    viewModel: MediaViewModel
) {
    // Intercept hardware and gesture back navigation
    BackHandler {
        onBack()
    }

    var hoverMovie by remember { mutableStateOf<TmdbMovie?>(null) }
    var selectedMovieForDetail by remember { mutableStateOf<TmdbMovie?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Loopa.Base)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Header Bar ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Loopa.Surface)
                        .border(1.dp, Loopa.Border, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Loopa.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Loopa.TextPrimary
                    )
                    Text(
                        text = "${category.items.size} Titles",
                        fontSize = 12.sp,
                        color = Loopa.TextSecondary
                    )
                }
            }

            HorizontalDivider(
                color = Loopa.Border,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ── Grid Content ─────────────────────────────────────────────
            if (category.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoopEmptyState(message = "No items available in this category.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(category.items.size) { index ->
                        val movie = category.items[index]
                        RecommendationCard(
                            movie = movie,
                            viewModel = viewModel,
                            onLongPress = { hoverMovie = it },
                            onRelease = { hoverMovie = null }
                        )
                    }
                }
            }
        }

        // ── Detail Sheet ───────────────────────────────────────────────────
        selectedMovieForDetail?.let { movie ->
            MediaDetailSheet(
                initialMovie = movie,
                viewModel = viewModel,
                onDismiss = { selectedMovieForDetail = null }
            )
        }
    }
}
