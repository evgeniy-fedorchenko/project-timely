package com.efedorchenko.timely.model.member

import kotlinx.serialization.Serializable

@Serializable
data class AcceptMemberResult(
    val result: AcceptMemberResultType
)
