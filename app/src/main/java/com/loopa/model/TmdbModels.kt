package com.loopa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbResponse<T>(
    @Json(name = "page") val page: Int,
    @Json(name = "results") val results: List<T>,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int
)

@JsonClass(generateAdapter = true)
data class TmdbMovie(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "overview") val overview: String?,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    @Json(name = "vote_average") val voteAverage: Double?,
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "first_air_date") val firstAirDate: String?,
    @Json(name = "media_type") val mediaType: String?,
    @Json(name = "popularity") val popularity: Double?,
    @Json(name = "genre_ids") val genreIds: List<Int>?
)

@JsonClass(generateAdapter = true)
data class TmdbSeasonResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String?,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "episodes") val episodes: List<TmdbEpisode>?
)

@JsonClass(generateAdapter = true)
data class TmdbEpisode(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String?,
    @Json(name = "episode_number") val episodeNumber: Int,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "air_date") val airDate: String?
)

@JsonClass(generateAdapter = true)
data class TmdbGenre(@Json(name = "name") val name: String)

@JsonClass(generateAdapter = true)
data class TmdbCrew(@Json(name = "job") val job: String, @Json(name = "name") val name: String)

@JsonClass(generateAdapter = true)
data class TmdbCredits(@Json(name = "crew") val crew: List<TmdbCrew>?)

@JsonClass(generateAdapter = true)
data class TmdbMovieDetailResponse(
    @Json(name = "runtime") val runtime: Int?,
    @Json(name = "genres") val genres: List<TmdbGenre>?,
    @Json(name = "credits") val credits: TmdbCredits?
)

@JsonClass(generateAdapter = true)
data class TmdbCreator(@Json(name = "name") val name: String)

@JsonClass(generateAdapter = true)
data class TmdbTvDetailResponse(
    @Json(name = "episode_run_time") val episodeRunTime: List<Int>?,
    @Json(name = "genres") val genres: List<TmdbGenre>?,
    @Json(name = "created_by") val createdBy: List<TmdbCreator>?,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int?,
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int?
)
