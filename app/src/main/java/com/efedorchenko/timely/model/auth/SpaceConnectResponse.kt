package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class SpaceConnectResponse(

    val success: Boolean,
    val newRole: RoleType? = null,
    val space: SpaceDto? = null
)
