package com.embychapter.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemsResponse(
    @SerialName("Items") val items: List<EmbyItem> = emptyList(),
    @SerialName("TotalRecordCount") val totalRecordCount: Int = 0
)

@Serializable
data class EmbyItem(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String?,
    @SerialName("OriginalTitle") val originalTitle: String? = null,
    @SerialName("Type") val type: String? = null,
    @SerialName("ProductionYear") val productionYear: Int? = null,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("Genres") val genres: List<String>? = null,
    @SerialName("ImageTags") val imageTags: ImageTags? = null,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null,
    @SerialName("CommunityRating") val communityRating: Float? = null,
    @SerialName("People") val people: List<Person>? = null
)

@Serializable
data class Person(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("Type") val type: String,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null
)

@Serializable
data class SystemInfo(
    @SerialName("Id") val id: String,
    @SerialName("ServerName") val serverName: String? = null
)
