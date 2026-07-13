package com.loopa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JikanResponse<T>(
    @Json(name = "data") val data: List<T>
)

@JsonClass(generateAdapter = true)
data class JikanAnime(
    @Json(name = "mal_id") val malId: Int,
    @Json(name = "title") val title: String,
    @Json(name = "synopsis") val synopsis: String?,
    @Json(name = "images") val images: JikanImages?,
    @Json(name = "score") val score: Double?
)

@JsonClass(generateAdapter = true)
data class JikanImages(
    @Json(name = "jpg") val jpg: JikanJpg?
)

@JsonClass(generateAdapter = true)
data class JikanJpg(
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "large_image_url") val largeImageUrl: String?
)
