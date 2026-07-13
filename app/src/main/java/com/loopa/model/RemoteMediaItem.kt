package com.loopa.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class RemoteMediaItem(
    @SerialName("id") val id: Int,
    @SerialName("user_id") val userId: String,
    @SerialName("title") val title: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("score") val score: Double? = null,
    @SerialName("list_name") val listName: String,
    @SerialName("media_type") val mediaType: String,
    @SerialName("current_season") val currentSeason: Int = 1,
    @SerialName("current_episode") val currentEpisode: Int = 0,
    @SerialName("total_episodes") val totalEpisodes: Int = 0,
    @SerialName("total_seasons") val totalSeasons: Int = 0,
    @SerialName("progress_string") val progressString: String? = null,
    @SerialName("user_rating") val userRating: Int? = null,
    @SerialName("personal_notes") val personalNotes: String? = null
)
