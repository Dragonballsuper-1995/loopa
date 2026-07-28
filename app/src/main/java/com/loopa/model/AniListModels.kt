package com.loopa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JikanResponse<T>(
    @Json(name = "data") val data: List<T>
)

@JsonClass(generateAdapter = true)
data class JikanAnime(
    @Json(name = "mal_id")   val malId: Int,
    @Json(name = "title")    val title: String,
    @Json(name = "synopsis") val synopsis: String?,
    @Json(name = "images")   val images: JikanImages?,
    @Json(name = "score")    val score: Double?
)

@JsonClass(generateAdapter = true)
data class JikanImages(
    @Json(name = "jpg") val jpg: JikanJpg?
)

@JsonClass(generateAdapter = true)
data class JikanJpg(
    @Json(name = "image_url")       val imageUrl: String?,
    @Json(name = "large_image_url") val largeImageUrl: String?
)

// ── Step 3.6 — Full anime detail types ────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class JikanAnimeDetailResponse(
    @Json(name = "data") val data: JikanAnimeDetail
)

@JsonClass(generateAdapter = true)
data class JikanAnimeDetail(
    @Json(name = "mal_id")   val malId: Int,
    @Json(name = "title")    val title: String,
    @Json(name = "synopsis") val synopsis: String?,
    @Json(name = "images")   val images: JikanImages?,
    @Json(name = "score")    val score: Double?,
    @Json(name = "episodes") val episodes: Int?,
    @Json(name = "genres")   val genres: List<JikanGenre>?,
    @Json(name = "status")   val status: String?,
    @Json(name = "year")     val year: Int?
)

@JsonClass(generateAdapter = true)
data class JikanGenre(
    @Json(name = "name") val name: String
)
