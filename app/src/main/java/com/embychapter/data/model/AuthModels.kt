package com.embychapter.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    @SerialName("Username") val username: String,
    @SerialName("Pw") val password: String
)

@Serializable
data class AuthResponse(
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("User") val user: EmbyUser
)

@Serializable
data class EmbyUser(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String
)
