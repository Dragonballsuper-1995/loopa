package com.loopa.network

import com.loopa.model.TmdbMovie
import com.loopa.model.TmdbResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("3/trending/all/week")
    suspend fun getTrendingAll(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbMovie>

    @GET("3/search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>

    @GET("3/movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>

    @GET("3/tv/popular")
    suspend fun getPopularTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>

    @GET("3/movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>

    @GET("3/movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>

    @GET("3/tv/top_rated")
    suspend fun getTopRatedTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>

    @GET("3/tv/airing_today")
    suspend fun getAiringTodayTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>

    @GET("3/tv/{tv_id}/season/{season_number}")
    suspend fun getTvSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): com.loopa.model.TmdbSeasonResponse

    @GET("3/movie/popular")
    suspend fun getPopularMoviesByRegion(
        @Query("api_key") apiKey: String,
        @Query("region") region: String = "IN",
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMovie>

    @GET("3/movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") appendToResponse: String = "credits",
        @Query("language") language: String = "en-US"
    ): com.loopa.model.TmdbMovieDetailResponse

    @GET("3/tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") appendToResponse: String = "credits",
        @Query("language") language: String = "en-US"
    ): com.loopa.model.TmdbTvDetailResponse

    @GET("3/movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbMovie>

    @GET("3/tv/{tv_id}/similar")
    suspend fun getSimilarTv(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<TmdbMovie>
}
