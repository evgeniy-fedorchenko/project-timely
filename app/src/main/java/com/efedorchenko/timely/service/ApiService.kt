package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.DataRangeRequest
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.UserDataModifyDto
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.auth.AuthResponse
import com.efedorchenko.timely.model.auth.Credentials
import com.efedorchenko.timely.model.auth.RegisterRequest
import com.efedorchenko.timely.model.auth.SpaceConnectResponse
import com.efedorchenko.timely.model.member.AcceptMember
import com.efedorchenko.timely.model.member.AcceptMemberResult
import com.efedorchenko.timely.model.member.MembersResult
import org.threeten.bp.Instant

interface ApiService {

    suspend fun login(credentials: Credentials): ApiResponse<AuthResponse>

    suspend fun register(registerRequest: RegisterRequest): ApiResponse<AuthResponse>

    suspend fun save(data: AbstractData): ApiResponse<AbstractData>

    suspend fun change(data: UserDataModifyDto): ApiResponse<Unit>

    suspend fun getMembers(withJoinRequests: Boolean, since: Instant? = null): ApiResponse<MembersResult>

    suspend fun getRange(dataRangeRequest: DataRangeRequest, dataType: DataType): ApiResponse<List<AbstractData>>

    suspend fun getUpdates(userId: String?, dataType: DataType, since: Instant?): ApiResponse<List<AbstractData>>

    suspend fun requestConnectToSpace(key: String): ApiResponse<SpaceConnectResponse>

    suspend fun detachFromSpace(userId: String? = null): ApiResponse<Boolean>

    suspend fun acceptMember(requestBody: AcceptMember): ApiResponse<AcceptMemberResult>
}