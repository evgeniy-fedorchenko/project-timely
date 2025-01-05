package com.efedorchenko.timely.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpaceMember(

    @SerialName("userId")
    val userUuid: String,

    val name: String,

    val position: String
)
