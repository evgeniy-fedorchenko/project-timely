package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(

    val register: Boolean,
    val authData: AuthData? = null,
    val userData: UserData? = null,
    val errorCode: AuthErrorCode,
    val errorMessage: String
)