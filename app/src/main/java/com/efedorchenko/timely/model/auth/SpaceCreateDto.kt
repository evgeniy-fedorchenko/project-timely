package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class SpaceCreateDto(
    val name: String
)
