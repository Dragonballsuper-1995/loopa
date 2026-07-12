package com.example.network

import com.example.model.JikanAnime
import com.example.model.JikanResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface JikanApiService {
    @GET("top/anime")
    suspend fun getTopAnime(
        @Query("limit") limit: Int = 10
    ): JikanResponse<JikanAnime>
}
