package com.efedorchenko.timely.model.auth

import com.efedorchenko.timely.model.other.SpaceCreateDto
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(

    val username: String,
    val password: String,
    val role: RoleType,
    val name: String,
    val position: String,
    val rate: Int,
    val creatingSpace: SpaceCreateDto,
    val spaceKey: String,
)