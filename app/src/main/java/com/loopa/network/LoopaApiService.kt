package com.loopa.network

import com.loopa.model.FastSearchResult
import com.loopa.model.TmdbMovie
import retrofit2.http.GET
import retrofit2.http.Query

interface LoopaApiService {
    @GET("api/search/fast")
    suspend fun searchFast(
        @Query("q") query: String
    ): List<FastSearchResult>

    @GET("api/media/details")
    suspend fun getMediaDetails(
        @Query("id") id: Int,
        @Query("type") type: String?,
        @Query("provider") provider: String?
    ): TmdbMovie
}
