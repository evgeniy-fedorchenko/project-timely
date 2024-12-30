package com.efedorchenko.timely.model.api

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val exName: String? = null,
    val sourceExName: String? = null,
    val errorCode: Int = 0,
    val errorMessage: String,
    val details: String? = null
)

