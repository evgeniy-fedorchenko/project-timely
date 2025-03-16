package com.efedorchenko.timely.model.api

import com.efedorchenko.timely.model.auth.AuthData
import com.efedorchenko.timely.model.auth.UserData

sealed class Resource<T> {

    data class Success<T>(val authData: AuthData, val userData: UserData) : Resource<T>()
    data class Error<T>(val message: String) : Resource<T>()
}