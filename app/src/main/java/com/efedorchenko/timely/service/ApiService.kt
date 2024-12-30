package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.auth.AuthRequest
import com.efedorchenko.timely.model.auth.AuthResponse
import com.efedorchenko.timely.model.auth.RegisterRequest

interface ApiService {

    suspend fun login(authRequest: AuthRequest): ApiResponse<AuthResponse>

    suspend fun register(registerRequest: RegisterRequest): ApiResponse<AuthResponse>

}