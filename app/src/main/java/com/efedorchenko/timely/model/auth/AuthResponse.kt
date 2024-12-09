package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class AuthResponse(
    val isRegister: Boolean,
    @Contextual val userId: UUID?,
    val jwtToken: String?,
    val role: RoleType?,
    val spaceKeys: SpaceKeys?,
    val errorCode: AuthErrorCode?,
    val errorMessage: String?
)