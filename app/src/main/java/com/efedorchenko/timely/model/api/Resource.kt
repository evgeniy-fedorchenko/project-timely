package com.efedorchenko.timely.model.api

import com.efedorchenko.timely.model.auth.RoleType

sealed class Resource<T> {

    data class Success<T>(val spacePresent: Boolean, val role: RoleType?) : Resource<T>()
    data class Error<T>(val message: String) : Resource<T>()
}