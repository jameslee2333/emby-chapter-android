package com.embychapter.data.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private fun requireValidBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim()
        require(trimmed.isNotBlank()) { "服务器地址不能为空" }
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "服务器地址必须以 http:// 或 https:// 开头，当前值: $trimmed"
        }
        return trimmed.trimEnd('/') + "/"
    }

    fun createEmbyApi(baseUrl: String): EmbyApiService {
        val normalizedUrl = requireValidBaseUrl(baseUrl)
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(createOkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(EmbyApiService::class.java)
    }

    fun createHistoryApi(baseUrl: String): HistoryApiService {
        val normalizedUrl = requireValidBaseUrl(baseUrl)
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(createOkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(HistoryApiService::class.java)
    }
}
