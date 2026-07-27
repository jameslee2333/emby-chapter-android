package com.embychapter.data.api

import com.embychapter.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface EmbyApiService {

    @POST("emby/Users/AuthenticateByName")
    suspend fun authenticate(
        @Header("X-Emby-Client") client: String = "Android Chapter Pro",
        @Header("X-Emby-Device-Name") deviceName: String = "Android",
        @Header("X-Emby-Device-Id") deviceId: String = "android-chapter-pro",
        @Header("X-Emby-Client-Version") version: String = "1.0.0",
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @GET("emby/Sessions")
    suspend fun getSessions(
        @Header("X-Emby-Token") token: String
    ): Response<List<SessionsResponse>>

    @GET("emby/Items")
    suspend fun getItems(
        @Header("X-Emby-Token") token: String,
        @Query("Recursive") recursive: Boolean = true,
        @Query("IncludeItemTypes") includeItemTypes: String = "Movie",
        @Query("Fields") fields: String = "BasicSyncInfo,PrimaryImageAspectRatio,ProductionYear,Overview,Genres,OriginalTitle,CommunityRating,People",
        @Query("SortBy") sortBy: String = "PremiereDate,ProductionYear,SortName",
        @Query("SortOrder") sortOrder: String = "Descending",
        @Query("Limit") limit: Int = 10000
    ): Response<ItemsResponse>

    @GET("emby/System/Info")
    suspend fun getSystemInfo(
        @Header("X-Emby-Token") token: String
    ): Response<SystemInfo>

    @GET("emby/chapter_api/get_chapters")
    suspend fun getChapters(
        @Header("X-Emby-Token") token: String,
        @Query("id") itemId: String,
        @Query("stamp") stamp: Long = System.currentTimeMillis()
    ): Response<ChaptersResponse>

    @GET("emby/chapter_api/update_chapters")
    suspend fun updateChapters(
        @Header("X-Emby-Token") token: String,
        @Header("X-Emby-Client") client: String = "Emby Web",
        @Header("X-Emby-Device-Name") deviceName: String = "Microsoft Edge Windows",
        @Header("X-Emby-Device-Id") deviceId: String = "android-chapter-pro",
        @Header("X-Emby-Client-Version") version: String = "4.9.3.0",
        @Query("id") itemId: String,
        @Query("action") action: String,
        @Query("index_list") indexList: String? = null,
        @Query("type") type: String? = null,
        @Query("name") name: String? = null,
        @Query("time") time: String? = null,
        @Query("stamp") stamp: Long = System.currentTimeMillis()
    ): Response<Unit>
}
