package com.embychapter.data.repository

import com.embychapter.data.api.RetrofitProvider
import com.embychapter.data.model.PlayRecord
import com.embychapter.data.model.StatsResponse

class HistoryRepository {

    private var apiService: com.embychapter.data.api.HistoryApiService? = null

    fun setBaseUrl(url: String) {
        apiService = RetrofitProvider.createHistoryApi(url)
    }

    private fun requireApi() = apiService ?: throw IllegalStateException("HistoryApiService not initialized")

    suspend fun getHistory(userId: String? = null): Result<List<PlayRecord>> {
        return try {
            val response = requireApi().getHistory(userId)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
            else Result.failure(Exception("获取历史失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getContinueWatching(): Result<List<PlayRecord>> {
        return try {
            val response = requireApi().getContinueWatching()
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
            else Result.failure(Exception("获取继续观看失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStats(): Result<StatsResponse> {
        return try {
            val response = requireApi().getStats()
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
            else Result.failure(Exception("获取统计失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecent(): Result<List<PlayRecord>> {
        return try {
            val response = requireApi().getRecent()
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
            else Result.failure(Exception("获取最近播放失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
