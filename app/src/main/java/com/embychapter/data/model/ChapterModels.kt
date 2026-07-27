package com.embychapter.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChaptersResponse(
    @SerialName("item_info") val itemInfo: ItemInfo? = null,
    @SerialName("chapters") val chapters: List<Chapter> = emptyList()
)

@Serializable
data class ItemInfo(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String
)

@Serializable
data class Chapter(
    @SerialName("Name") val name: String,
    @SerialName("StartTime") val startTime: String,
    @SerialName("Index") val index: Int
)
