package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Serializable

@Serializable
enum class AuthStatus {
    SUCCESS, FAIL
}
