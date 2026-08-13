package com.loopa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KitsuResponse(
    val data: List<KitsuData>?
)

@JsonClass(generateAdapter = true)
data class KitsuData(
    val id: String,
    val attributes: KitsuAttributes?
)

@JsonClass(generateAdapter = true)
data class KitsuAttributes(
    val titles: KitsuTitles?,
    val canonicalTitle: String?,
    @Json(name = "posterImage") val posterImage: KitsuImage?,
    val startDate: String?,
    val averageRating: String?,
    val status: String?,
    val episodeCount: Int?,
    val synopsis: String?
)

@JsonClass(generateAdapter = true)
data class KitsuTitles(
    val en: String?,
    val en_jp: String?,
    val ja_jp: String?
)

@JsonClass(generateAdapter = true)
data class KitsuImage(
    val original: String?,
    val large: String?,
    val small: String?,
    val tiny: String?
)
