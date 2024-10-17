package com.efedorchenko.timely.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val status: AuthStatus,
    val uuid: String? = null,
    val user: User? = null
)