package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(

    val register: Boolean,

    @Contextual
    @SerialName("userId")
    val userUuid: String? = null,
    val jwtToken: String? = null,
    val role: RoleType? = null,
    val generatedSpaceKeys: SpaceKeys? = null,
    val errorCode: AuthErrorCode? = null,
    val errorMessage: String? = null
)