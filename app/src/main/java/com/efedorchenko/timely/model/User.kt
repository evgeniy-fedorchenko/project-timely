package com.efedorchenko.timely.model

import com.efedorchenko.timely.model.auth.RoleType
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val name: String,
    val email: String,
    val position: String,
    val ratePerHour: Int?,
    val role: RoleType,
    val events: List<Event>?,
    val fines: List<Fine>?,
    val adminData: Pair<String, String>?
)
