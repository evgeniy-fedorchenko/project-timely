package com.efedorchenko.timely.model.api

sealed class ApiResponse<T> {

    data class Success<T>(val data: T? = null) : ApiResponse<T>()

    data class Error<T>(
        val apiErrorCode: ApiErrorCode,
        val errorMessage: String? = null,
        val errorData: T? = null // Для случаев с auth ошибкой и данными
    ) : ApiResponse<T>()

}
