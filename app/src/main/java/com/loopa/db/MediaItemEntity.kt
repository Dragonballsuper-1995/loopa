package com.loopa.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "media_items",
    primaryKeys = ["id", "mediaType"],
    indices = [
        Index(value = ["listName"]),
        Index(value = ["mediaType"]),
        Index(value = ["updatedAt"])
    ]
)
data class MediaItemEntity(
    val id: Int,           // The TMDB/Jikan ID
    val title: String,
    val imageUrl: String?,
    val date: String?,
    val score: Double?,
    val listName: String,  // "Watched", "Watching"
    val mediaType: String, // "movie", "tv", "anime"
    val currentSeason: Int    = 1,
    val currentEpisode: Int   = 0,
    val totalEpisodes: Int    = 0,
    val totalSeasons: Int     = 0,
    val progressString: String? = null,
    val userRating: Int?      = null,
    val personalNotes: String? = null,
    // New fields for Stats Dashboard
    val runtime: Int?         = null,
    val genres: String?       = null,
    val directorStudio: String? = null,
    val progressBackup: String? = null,
    // ISO 8601 timestamp from Supabase — used for Last-Write-Wins conflict resolution.
    // Null for items inserted before the schema migration.
    val updatedAt: String?    = null
)
