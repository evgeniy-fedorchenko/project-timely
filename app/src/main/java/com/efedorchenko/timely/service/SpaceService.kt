package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.SyncOperator
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.member.AcceptMemberResultType
import com.efedorchenko.timely.model.member.SpaceStatus

interface SpaceService {

    suspend fun updateMembers(srcRole: RoleType, srcSpaceStatus: SpaceStatus): SyncOperator.UpdateResult

    suspend fun acceptRemote(userId: String, role: RoleType): AcceptMemberResultType
    
    suspend fun rejectRemote(userId: String): Boolean
    
    suspend fun leaveSpace(): Boolean
}