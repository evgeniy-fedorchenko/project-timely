package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.SyncProcess

interface SpaceService {

    suspend fun initMembers(): Boolean

    suspend fun updateMembers(): SyncProcess.UpdateResult

    suspend fun leaveSpace(): Boolean
}