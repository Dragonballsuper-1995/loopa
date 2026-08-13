package com.loopa.network

import com.loopa.model.KitsuResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Headers

interface KitsuApiService {
    @GET("anime")
    suspend fun searchAnime(
        @Query("filter[text]") query: String,
        @Query("page[limit]") limit: Int = 12
    ): KitsuResponse
}
