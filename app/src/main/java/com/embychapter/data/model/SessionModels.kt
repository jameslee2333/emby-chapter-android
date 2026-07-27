package com.embychapter.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionsResponse(
    @SerialName("Id") val id: String? = null,
    @SerialName("DeviceName") val deviceName: String? = null,
    @SerialName("NowPlayingItem") val nowPlayingItem: NowPlayingItem? = null,
    @SerialName("PlayState") val playState: PlayState? = null
)

@Serializable
data class NowPlayingItem(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String?,
    @SerialName("MediaType") val mediaType: String?,
    @SerialName("ImageTags") val imageTags: ImageTags? = null,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null
)

@Serializable
data class ImageTags(
    @SerialName("Primary") val primary: String? = null
)

@Serializable
data class PlayState(
    @SerialName("PositionTicks") val positionTicks: Long? = null
)
