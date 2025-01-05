package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.SpaceMember
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.auth.AuthResponse
import com.efedorchenko.timely.model.auth.Credentials
import com.efedorchenko.timely.model.auth.RegisterRequest

interface ApiService {

    suspend fun login(credentials: Credentials): ApiResponse<AuthResponse>

    suspend fun register(registerRequest: RegisterRequest): ApiResponse<AuthResponse>

    suspend fun save(data: AbstractData): ApiResponse<AbstractData>

    suspend fun getMembers(): ApiResponse<List<SpaceMember>>
}