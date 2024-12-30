package com.efedorchenko.timely.model.api

fun <T> ApiResponse<T>.onSuccess(block: (data: T?) -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Success) {
        block(data)
    }
    return this
}

fun <T> ApiResponse<T>.onError(
    block: (apiErrorCode: ApiErrorCode, errorMessage: String?, errorData: T?) -> Unit
): ApiResponse<T> {
    if (this is ApiResponse.Error) {
        block(apiErrorCode, errorMessage, errorData)
    }
    return this
}
