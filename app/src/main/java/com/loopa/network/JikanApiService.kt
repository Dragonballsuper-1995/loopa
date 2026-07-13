package com.loopa.network

import com.loopa.model.JikanAnime
import com.loopa.model.JikanResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface JikanApiService {
    @GET("top/anime")
    suspend fun getTopAnime(
        @Query("limit") limit: Int = 10
    ): JikanResponse<JikanAnime>
}
