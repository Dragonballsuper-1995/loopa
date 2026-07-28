package com.loopa.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "watched_episodes",
    primaryKeys = ["mediaId", "mediaType", "seasonNumber", "episodeNumber"],
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id", "mediaType"],
            childColumns = ["mediaId", "mediaType"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mediaId", "mediaType"])
    ]
)
data class WatchedEpisodeEntity(
    val mediaId: Int,
    val mediaType: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    // FIXED: was Long (Unix ms). Now ISO 8601 string — "2026-07-22T06:00:00Z"
    // Matches Supabase TIMESTAMPTZ and Web's new Date().toISOString()
    val watchedAt: String = java.time.Instant.now().toString()
)
