package com.efedorchenko.timely.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val username: String,
    val password: String
) {
    constructor(loginData: Pair<String, String>) : this(loginData.first, loginData.second)
}