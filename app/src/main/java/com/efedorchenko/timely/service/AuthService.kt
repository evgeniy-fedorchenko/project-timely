package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.api.Resource
import com.efedorchenko.timely.model.auth.Credentials
import com.efedorchenko.timely.model.auth.RegisterRequest

interface AuthService {

    suspend fun tryLogin(credentials: Credentials): Resource<Unit>

    suspend fun tryRegister(registerRequest: RegisterRequest): Resource<Unit>
}