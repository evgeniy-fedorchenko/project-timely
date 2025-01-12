package com.efedorchenko.timely.model

import kotlinx.serialization.Serializable

@Serializable
data class MembersResult(

    val consistInSpace: Boolean,
    val members: List<SpaceMember>
)
