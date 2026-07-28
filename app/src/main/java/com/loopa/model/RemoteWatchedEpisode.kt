package com.loopa.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Supabase-serialisable representation of a watched_episodes row.
 * Maps 1:1 with the Postgres schema created in the SQL migration.
 *
 * watched_at / updated_at are ISO 8601 strings — matching both:
 *   - Supabase TIMESTAMPTZ column
 *   - Web JS: new Date().toISOString()  (supabase.js:249)
 */
@Serializable
data class RemoteWatchedEpisode(
    @SerialName("media_id")       val mediaId: Int,
    @SerialName("user_id")        val userId: String,
    @SerialName("media_type")     val mediaType: String,
    @SerialName("season_number")  val seasonNumber: Int,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("watched_at")     val watchedAt: String  = java.time.Instant.now().toString(),
    @SerialName("updated_at")     val updatedAt: String? = null
)
