package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.AuthRequest
import com.efedorchenko.timely.model.auth.AuthResponse
import com.efedorchenko.timely.model.auth.RegisterRequest

interface ApiService {

    suspend fun login(authRequest: AuthRequest): Result<AuthResponse>

    suspend fun register(registerRequest: RegisterRequest): Result<AuthResponse>

}