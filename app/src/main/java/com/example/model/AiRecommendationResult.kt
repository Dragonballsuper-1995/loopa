package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AiRecommendationResult(
    val title: String,
    val mediaType: String,
    val genre: String? = null,
    val releaseYear: String? = null,
    val imageUrl: String? = null,
    val reasoning: String? = null
)

data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val recommendations: List<AiRecommendationResult> = emptyList()
)
