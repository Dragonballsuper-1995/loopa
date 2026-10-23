package com.loopa.ui.components

import androidx.compose.runtime.Composable
import com.loopa.viewmodel.MediaViewModel

// Episode tracking removed per design refresh
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
    // No-op: Episode tracking removed completely
}
