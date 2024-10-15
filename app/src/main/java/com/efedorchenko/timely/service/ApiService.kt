package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.AuthRequest
import com.efedorchenko.timely.model.AuthResponse

interface ApiService {

    suspend fun login(authRequest: AuthRequest): AuthResponse?
}