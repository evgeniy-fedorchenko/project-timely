package com.efedorchenko.timely.model.member

import com.efedorchenko.timely.model.auth.RoleType
import kotlinx.serialization.Serializable

@Serializable
data class AcceptMember(

    val acceptedUserId: String,
    val newRole: RoleType
)
