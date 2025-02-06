package com.efedorchenko.timely.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDataModifyDto(

    val modifyingUserId: String? = null,
    val newData: AbstractData
)
