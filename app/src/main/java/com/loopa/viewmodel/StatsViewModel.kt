package com.loopa.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loopa.db.DatabaseProvider
import com.loopa.db.MediaItemEntity
import com.loopa.repository.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatsState(
    val totalWatchTimeStr: String = "0H",
    val topGenre: String = "N/A",
    val topDirector: String = "N/A",
    val totalTitles: Int = 0
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaItemDao = DatabaseProvider.getDatabase(application).mediaItemDao()
    private val watchedEpisodeDao = DatabaseProvider.getDatabase(application).watchedEpisodeDao()
    private val pendingOpDao = DatabaseProvider.getDatabase(application).pendingOpDao()
    private val repository = MediaRepository(mediaItemDao, pendingOpDao, watchedEpisodeDao)

    val statsState: StateFlow<StatsState> = repository.allMediaItems.map { items: List<MediaItemEntity> ->
        calculateStats(items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsState()
    )

    private fun calculateStats(watchlist: List<MediaItemEntity>): StatsState {
        if (watchlist.isEmpty()) return StatsState()

        var totalRuntimeMinutes = 0
        val genreCounts = mutableMapOf<String, Int>()
        val directorCounts = mutableMapOf<String, Int>()

        watchlist.forEach { item ->
            if (item.listName == "Completed" || item.listName == "Watched") {
                if (item.mediaType == "movie") {
                    totalRuntimeMinutes += (item.runtime ?: 120)
                } else {
                    val eps = if ((item.totalEpisodes ?: 0) > 0) item.totalEpisodes!! else if ((item.currentEpisode ?: 0) > 0) item.currentEpisode!! else 12
                    totalRuntimeMinutes += (eps * (item.runtime ?: 24))
                }
            } else if ((item.currentEpisode ?: 0) > 0) {
                totalRuntimeMinutes += (item.currentEpisode!! * (item.runtime ?: 24))
            }

            // Genre aggregation
            item.genres?.split(",")?.forEach { g ->
                val genre = g.trim()
                if (genre.isNotEmpty()) {
                    genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
                }
            }

            // Director/Studio aggregation
            item.directorStudio?.trim()?.let { dir ->
                if (dir.isNotEmpty()) {
                    directorCounts[dir] = (directorCounts[dir] ?: 0) + 1
                }
            }
        }

        val totalHours = totalRuntimeMinutes / 60
        val days = totalHours / 24
        val hours = totalHours % 24
        val watchTimeStr = if (days > 0) "${days}D ${hours}H" else "${totalHours}H"

        val topGenre = genreCounts.entries.maxByOrNull { it.value }?.key ?: "N/A"
        val topDirector = directorCounts.entries.maxByOrNull { it.value }?.key ?: "N/A"

        return StatsState(
            totalWatchTimeStr = watchTimeStr,
            topGenre = topGenre,
            topDirector = topDirector,
            totalTitles = watchlist.size
        )
    }
}
