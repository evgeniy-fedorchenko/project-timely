package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.SpaceMember

interface SpaceService {

    suspend fun initMembers()

    fun downloadMember(member: SpaceMember)
}