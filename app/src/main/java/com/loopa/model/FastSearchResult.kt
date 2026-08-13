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
    @Json(name = "score") val score: Double?
) {
    fun toTmdbMovie(): TmdbMovie {
        val rawPath = posterUrl ?: ""
        val cleanPosterPath = if (rawPath.startsWith("https://image.tmdb.org/t/p/w154")) {
            rawPath.removePrefix("https://image.tmdb.org/t/p/w154")
        } else if (rawPath.startsWith("https://image.tmdb.org/t/p/w500")) {
            rawPath.removePrefix("https://image.tmdb.org/t/p/w500")
        } else if (rawPath.startsWith("https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w500")) {
            rawPath.removePrefix("https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w500")
        } else {
            rawPath
        }

        return TmdbMovie(
            id = id,
            title = title,
            name = title,
            overview = null,
            posterPath = if (cleanPosterPath.isEmpty()) null else cleanPosterPath,
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
