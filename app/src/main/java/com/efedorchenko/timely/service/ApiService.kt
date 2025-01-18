package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.DataRangeRequest
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.MembersResult
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.auth.AuthResponse
import com.efedorchenko.timely.model.auth.Credentials
import com.efedorchenko.timely.model.auth.RegisterRequest
import com.efedorchenko.timely.model.auth.SpaceConnectResponse
import org.threeten.bp.Instant

interface ApiService {

    suspend fun login(credentials: Credentials): ApiResponse<AuthResponse>

    suspend fun register(registerRequest: RegisterRequest): ApiResponse<AuthResponse>

    suspend fun save(data: AbstractData): ApiResponse<AbstractData>

    suspend fun getMembers(): ApiResponse<MembersResult>

    suspend fun getMembers(since: Instant?): ApiResponse<MembersResult> // TODO: посмотреть, может можно соединить с предыдущей функцией

    suspend fun getRange(dataRangeRequest: DataRangeRequest, dataType: DataType): ApiResponse<List<AbstractData>>

    suspend fun getUpdates(userId: String?, dataType: DataType, since: Instant?): ApiResponse<List<AbstractData>>

    suspend fun connectToSpace(key: String): ApiResponse<SpaceConnectResponse>

    suspend fun leaveSpace(): ApiResponse<Boolean>
}