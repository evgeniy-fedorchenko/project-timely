package com.efedorchenko.timely.service

import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.UserProfile
import com.efedorchenko.timely.data.repository.MemberRepository
import com.efedorchenko.timely.model.SyncOperator.UpdateResult
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.member.AcceptMember
import com.efedorchenko.timely.model.member.AcceptMemberResultType
import com.efedorchenko.timely.model.member.MembersResult
import com.efedorchenko.timely.model.member.SpaceMember
import com.efedorchenko.timely.model.member.SpaceStatus
import javax.inject.Inject

class SpaceServiceImpl @Inject constructor(
    private val apiService: ApiService,
    private val memberRepository: MemberRepository,
    private val encUserProfile: EncUserProfile,
    private val userProfile: UserProfile,
    private val spaceViewModel: SpaceViewModel
) : SpaceService {

    companion object {
        private const val NE_TAG = "SSI Network error"
    }

    override suspend fun updateMembers(srcRole: RoleType, srcSpaceStatus: SpaceStatus): UpdateResult {
        val since = memberRepository.getMaxChangedAt()
        return doUpdateMembers { apiService.getMembers(withJoinRequests, since) }
    }

    override suspend fun acceptRemote(userId: String, role: RoleType): AcceptMemberResultType {
        return when (val response =  apiService.acceptMember(AcceptMember(userId, role))) {
            is ApiResponse.Success -> response.data?.result ?: AcceptMemberResultType.FAIL
            is ApiResponse.Error -> {
                val errMess = "Request to accept user [$userId], role [$role] was filed, current user: [${getAuthId()}]"
                response.logErr(NE_TAG, errMess)
                AcceptMemberResultType.FAIL
            }
        }
    }

    override suspend fun rejectRemote(userId: String): Boolean {
        return detachFromSpace({ apiService.detachFromSpace(userId) })
    }

    override suspend fun leaveSpace(): Boolean {
        return detachFromSpace({ apiService.detachFromSpace() })
    }

    private suspend fun detachFromSpace(
        requestFunc: suspend () -> ApiResponse<Boolean>, userId: String? = null
    ): Boolean {
        return when (val response = requestFunc.invoke()) {
            /*
            * Информация о статусе, которая хранится в хранилище обновляется кодом, который
            * вызывает этот метод, так как только там есть информация об обрабатываемом юзере.
            * Если обновляется текущий юзер - должен обновиться profileStorage()
            * Если это админ обновляет какого-то юзера - обновляется БД + spaceViewModel
            */
            is ApiResponse.Success -> {
                response.data ?: false
            }
            is ApiResponse.Error -> {
                val errMess = "Request for disconnect user was filed: userId: [$userId]. current user: [${getAuthId()}]"
                response.logErr(NE_TAG, errMess)
                return false
            }
        }
    }

    private suspend fun doUpdateMembers(requestFunc: suspend () -> ApiResponse<MembersResult>): UpdateResult {
            when (val response = requestFunc.invoke()) {
            is ApiResponse.Success -> {
                response.data?.let {
                    if (it.spaceStatus != SpaceStatus.MEMBER) {
                        return UpdateResult.NOT_CONSIST_IN_SPACE
                    }
                    userProfile.setSpace(it.space)
                    userProfile.setSpaceStatus(it.spaceStatus)
                    memberRepository.deleteIfNotContains(it.actualIds)
                    if (it.members.isNotEmpty()) {
                        memberRepository.save(filterMembers(it.members))
                        spaceViewModel.updateMembers()
                    }
                    return UpdateResult.SUCCESS
                }
                return UpdateResult.FAIL
            }
            is ApiResponse.Error -> {
                response.logErr(NE_TAG, " members from server, userId: [${getAuthId()}]")
                return UpdateResult.FAIL
            }
        }
    }

    private fun filterMembers(members: MutableList<SpaceMember>): MutableList<SpaceMember> {
        if (members.isEmpty()) return mutableListOf()
       /*
       * Работник видит только работников, кроме себя
       * Руководители видят всех работников
       * Создатель пространства видит всех юзеров, кроме себя
       */
        members.removeIf { it.userUuid == getAuthId() || it.isCreator() }

        if (encUserProfile.getRole() != RoleType.CREATOR) {
            members.removeIf { it.isBoss() }
        }
        if (!encUserProfile.isPrivileged()) {
            members.removeIf { it.isPendingMember() }
        }
        return members
    }

    private fun getAuthId() = encUserProfile.getUserUuid()
}
