package com.efedorchenko.timely.model.auth

import com.efedorchenko.timely.model.member.SpaceStatus
import kotlinx.serialization.Serializable

@Serializable
data class UserData(

    val name: String,
    val position: String,
    val rate: Int? = null,
    val spaceName: String? = null,
    val spaceStatus: SpaceStatus = SpaceStatus.NONE
)
