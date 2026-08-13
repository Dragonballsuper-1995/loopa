package com.loopa.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AniListApiService {
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST(".")
    suspend fun query(@Body request: AniListRequest): AniListResponse
}

@JsonClass(generateAdapter = true)
data class AniListRequest(
    val query: String,
    val variables: Map<String, Int>?
)

@JsonClass(generateAdapter = true)
data class AniListResponse(
    val data: AniListData?
)

@JsonClass(generateAdapter = true)
data class AniListData(
    val Media: AniListMedia?,
    val Page: AniListPage?
)

@JsonClass(generateAdapter = true)
data class AniListPage(
    val media: List<AniListMedia>?
)

@JsonClass(generateAdapter = true)
data class AniListMedia(
    val id: Int,
    val title: AniListTitle?,
    val coverImage: AniListCoverImage?,
    val bannerImage: String?,
    val startDate: AniListFuzzyDate?,
    val meanScore: Int?,
    val description: String?,
    val genres: List<String>?,
    val episodes: Int?,
    val status: String?,
    val nextAiringEpisode: AniListNextAiringEpisode?,
    val recommendations: AniListRecommendations?
)

@JsonClass(generateAdapter = true)
data class AniListTitle(
    val english: String?,
    val romaji: String?,
    val userPreferred: String?
)

@JsonClass(generateAdapter = true)
data class AniListCoverImage(
    val extraLarge: String?,
    val large: String?
)

@JsonClass(generateAdapter = true)
data class AniListFuzzyDate(
    val year: Int?
)

@JsonClass(generateAdapter = true)
data class AniListNextAiringEpisode(
    val episode: Int
)

@JsonClass(generateAdapter = true)
data class AniListRecommendations(
    val nodes: List<AniListRecommendationNode>?
)

@JsonClass(generateAdapter = true)
data class AniListRecommendationNode(
    val mediaRecommendation: AniListMedia?
)
