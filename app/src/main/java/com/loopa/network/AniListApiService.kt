package com.loopa.network

import com.loopa.model.JikanAnime
import com.loopa.model.JikanAnimeDetailResponse
import com.loopa.model.JikanResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JikanApiService {
    @GET("top/anime")
    suspend fun getTopAnime(
        @Query("limit") limit: Int = 10
    ): JikanResponse<JikanAnime>

    @GET("seasons/upcoming")
    suspend fun getUpcomingAnime(
        @Query("limit") limit: Int = 10
    ): JikanResponse<JikanAnime>

    // Step 3.1 — Live anime search; mirrors api.js searchAnime()
    @GET("anime")
    suspend fun searchAnime(
        @Query("q")     query: String,
        @Query("limit") limit: Int = 12,
        @Query("sfw")   sfw: Boolean = true
    ): JikanResponse<JikanAnime>

    // Step 3.6 — Full anime detail fetch; mirrors api.js fetchDetails() for anime
    @GET("anime/{id}/full")
    suspend fun getAnimeDetails(
        @Path("id") id: Int
    ): JikanAnimeDetailResponse
}
