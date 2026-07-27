package com.embychapter.data.repository

import com.embychapter.data.api.RetrofitProvider
import com.embychapter.data.model.*

class EmbyRepository {

    private var apiService: com.embychapter.data.api.EmbyApiService? = null

    fun setBaseUrl(url: String) {
        apiService = RetrofitProvider.createEmbyApi(url)
    }

    private fun requireApi() = apiService ?: throw IllegalStateException("EmbyApiService not initialized. Call setBaseUrl() first.")

    suspend fun authenticate(serverUrl: String, username: String, password: String): Result<AuthResponse> {
        setBaseUrl(serverUrl)
        return try {
            val response = requireApi().authenticate(request = AuthRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("登录失败: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSessions(token: String): Result<List<SessionsResponse>> {
        return try {
            val response = requireApi().getSessions(token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("获取会话失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItems(token: String): Result<List<EmbyItem>> {
        return try {
            val response = requireApi().getItems(token = token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.items)
            } else {
                Result.failure(Exception("获取影片列表失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChapters(token: String, itemId: String): Result<ChaptersResponse> {
        return try {
            val response = requireApi().getChapters(token, itemId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("获取章节失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addChapter(token: String, itemId: String, name: String, time: String): Result<Unit> {
        return try {
            val response = requireApi().updateChapters(
                token = token,
                itemId = itemId,
                action = "add",
                type = "chapter",
                name = name,
                time = time
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("添加章节失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChapters(token: String, itemId: String, indexList: String): Result<Unit> {
        return try {
            val response = requireApi().updateChapters(
                token = token,
                itemId = itemId,
                action = "remove",
                indexList = indexList
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("删除章节失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getImageUrl(serverUrl: String, itemId: String, imageTag: String, token: String, maxWidth: Int = 360): String {
        val base = serverUrl.trimEnd('/')
        return "$base/emby/Items/$itemId/Images/Primary?maxWidth=$maxWidth&quality=90&tag=$imageTag&api_key=$token"
    }

    fun getItemWebUrl(serverUrl: String, itemId: String): String {
        val base = serverUrl.trimEnd('/')
        return "$base/web/index.html#!/item?id=$itemId"
    }
}
