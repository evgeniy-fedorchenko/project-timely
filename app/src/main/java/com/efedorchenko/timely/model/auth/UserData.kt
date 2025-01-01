package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class UserData(

    val name: String,
    val position: String,
    val rate: Int? = null,
    val spaceName: String
)
