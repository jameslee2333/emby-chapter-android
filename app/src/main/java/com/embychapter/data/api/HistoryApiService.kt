package com.embychapter.data.api

import com.embychapter.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface HistoryApiService {

    @GET("health")
    suspend fun health(): Response<Map<String, String>>

    @GET("history")
    suspend fun getHistory(
        @Query("user_id") userId: String? = null
    ): Response<List<PlayRecord>>

    @GET("continue")
    suspend fun getContinueWatching(): Response<List<PlayRecord>>

    @GET("stats")
    suspend fun getStats(): Response<StatsResponse>

    @GET("recent")
    suspend fun getRecent(): Response<List<PlayRecord>>
}
