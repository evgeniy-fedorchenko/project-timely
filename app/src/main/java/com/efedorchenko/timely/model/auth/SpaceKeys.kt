package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class SpaceKeys(
    val workerKey: String,
    val bossKey: String
)