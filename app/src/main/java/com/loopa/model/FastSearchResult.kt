package com.loopa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FastSearchResult(
    @Json(name = "id") val id: Int,
    @Json(name = "mediaType") val mediaType: String?,
    @Json(name = "provider") val provider: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "posterUrl") val posterUrl: String?,
    @Json(name = "year") val year: String?,
    @Json(name = "score") val score: Double?,
    @Json(name = "isAiMatch") val isAiMatch: Boolean? = false,
    @Json(name = "aiReason") val aiReason: String? = null
) {
    fun toTmdbMovie(): TmdbMovie {
        val cleanPosterPath = com.loopa.util.TmdbUrlHelper.cleanPath(posterUrl)

        return TmdbMovie(
            id = id,
            title = title,
            name = title,
            overview = if (isAiMatch == true) "[AI Match] ${aiReason ?: ""}" else null,
            posterPath = if (cleanPosterPath.isNullOrEmpty()) null else cleanPosterPath,
            backdropPath = null,
            voteAverage = score,
            releaseDate = year,
            firstAirDate = year,
            mediaType = mediaType,
            popularity = 0.0,
            genreIds = null
        )
    }
}
