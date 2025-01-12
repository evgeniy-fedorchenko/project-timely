package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.SpaceMember
import com.efedorchenko.timely.service.SpaceServiceImpl.InitResult
import com.efedorchenko.timely.service.SpaceServiceImpl.UpdateResult

interface SpaceService {

    suspend fun initMembers(): Boolean

    fun downloadMember(member: SpaceMember)

    suspend fun initData(): InitResult

    suspend fun updateData(userId: String?, withMembers: Boolean): UpdateResult
}