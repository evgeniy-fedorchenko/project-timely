package com.efedorchenko.timely.model.auth

data class AuthData(
    val userUuid: String,
    val jwtToken: String,
    val role: RoleType,
    val generatedSpaceKeys: SpaceKeys?
)
