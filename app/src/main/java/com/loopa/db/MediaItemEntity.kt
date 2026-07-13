package com.loopa.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items", primaryKeys = ["id", "mediaType"])
data class MediaItemEntity(
    val id: Int, // The TMDB/Jikan ID
    val title: String,
    val imageUrl: String?,
    val date: String?,
    val score: Double?,
    val listName: String, // "Watched", "To Watch", or custom lists
    val mediaType: String, // "movie", "tv", "anime"
    val currentSeason: Int = 1,
    val currentEpisode: Int = 0,
    val totalEpisodes: Int = 0,
    val totalSeasons: Int = 0,
    val progressString: String? = null,
    val userRating: Int? = null,
    val personalNotes: String? = null
)
