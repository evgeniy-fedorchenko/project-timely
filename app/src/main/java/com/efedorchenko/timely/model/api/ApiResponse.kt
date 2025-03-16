package com.efedorchenko.timely.model.api

import android.util.Log

sealed class ApiResponse<T> {

    data class Success<T>(val data: T? = null) : ApiResponse<T>()

    data class Error<T>(
        val rqUid: String?,
        val apiErrorCode: ApiErrorCode,
        val errorMessage: String? = null,
        val errorData: T? = null // Для случаев с auth ошибкой и данными
    ) : ApiResponse<T>() {

        fun logErr(tag: String, uniquePart: String) {
            with(this@Error) {
                val message = buildString {
                    append("$uniquePart, ")
                    append("requestUid: [$rqUid], ")
                    append("apiErrorCode: [$apiErrorCode], ")
                    append("errorMessage: [$errorMessage], ")
                    append("errorData: [$errorData]")
                }
                Log.e(tag, message)
            }
        }
    }
}
