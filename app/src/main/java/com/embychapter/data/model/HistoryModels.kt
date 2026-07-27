package com.embychapter.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayRecord(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("item_id") val itemId: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("duration") val duration: Int? = null,
    @SerialName("position") val position: Int? = null,
    @SerialName("progress") val progress: Int? = null,
    @SerialName("is_finished") val isFinished: Int? = null,
    @SerialName("play_session_id") val playSessionId: String? = null,
    @SerialName("client") val client: String? = null,
    @SerialName("device") val device: String? = null,
    @SerialName("played_at") val playedAt: String? = null,
    @SerialName("last_played_at") val lastPlayedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("actual_duration") val actualDuration: Int? = null,
    @SerialName("event_time") val eventTime: String? = null,
    @SerialName("event_type") val eventType: String? = null
)

@Serializable
data class StatsResponse(
    @SerialName("total_plays") val totalPlays: Int = 0,
    @SerialName("finished_plays") val finishedPlays: Int = 0,
    @SerialName("total_duration") val totalDuration: Int = 0,
    @SerialName("actual_total_duration") val actualTotalDuration: Int = 0
)
