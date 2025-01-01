package com.efedorchenko.timely.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthData(

    @SerialName("userId")
    val userUuid: String,
    val jwtToken: String,
    val role: RoleType,
    val generatedSpaceKeys: SpaceKeys? = null
)
