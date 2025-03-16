package com.efedorchenko.timely.model.auth

import com.efedorchenko.timely.model.member.SpaceConnectResultType
import com.efedorchenko.timely.model.member.SpaceStatus
import kotlinx.serialization.Serializable

@Serializable
data class SpaceConnectResponse(

    val result: SpaceConnectResultType? = null,
    val newSpaceStatus: SpaceStatus = SpaceStatus.NONE
)
