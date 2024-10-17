package com.efedorchenko.timely.model

import kotlinx.serialization.Serializable

@Serializable
data class UserMenuInfo(
    val name: String,
    val position: String,
    val ratePerHour: Int,
)
