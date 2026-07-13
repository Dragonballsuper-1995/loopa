package com.loopa.network

import com.loopa.model.TmdbMovie
import com.loopa.model.TmdbResponse
import retrofit2.http.GET
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
}
