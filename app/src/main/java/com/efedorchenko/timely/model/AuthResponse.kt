package com.efedorchenko.timely.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val status: AuthStatus,
    val role: UserRole? = null,
    val uuid: String? = null
)