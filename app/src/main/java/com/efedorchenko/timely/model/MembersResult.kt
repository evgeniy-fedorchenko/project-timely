package com.efedorchenko.timely.model

import kotlinx.serialization.Serializable

@Serializable
data class MembersResult(

    val youConsistInSpace: Boolean,
    val members: MutableList<SpaceMember>,

//    @Serializable(with = UUIDSerializer::class)
    val actualIds: List<String>
)
