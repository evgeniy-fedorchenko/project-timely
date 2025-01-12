package com.efedorchenko.timely.model

import kotlinx.serialization.Serializable

@Serializable
enum class EventStatus {

    SUCCESS, MISSED
}
