package com.efedorchenko.timely.model.api

sealed class Resource<T> {

    data class Success<T>(val spacePresent: Boolean) : Resource<T>()
    data class Error<T>(val message: String) : Resource<T>()
}