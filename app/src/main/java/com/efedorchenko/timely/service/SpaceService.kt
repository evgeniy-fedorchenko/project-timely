package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.SpaceMember

interface SpaceService {

    suspend fun initMembers(): Boolean

    fun downloadMember(member: SpaceMember)

    suspend fun initData(): SpaceServiceImpl.InitResult
}