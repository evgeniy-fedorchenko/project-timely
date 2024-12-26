package com.efedorchenko.timely.model

data class ApiResponse<T>(
    val data: T? = null,
    val isAuthError: Boolean = false,
    val errorMessage: String? = null
)