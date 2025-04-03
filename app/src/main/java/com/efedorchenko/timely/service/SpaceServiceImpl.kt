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
        return doUpdateMembers(srcRole, srcSpaceStatus) { apiService.getMembers(srcRole.isPrivileged(), since) }
    }

    override suspend fun acceptRemote(userId: String, role: RoleType): AcceptMemberResultType {
        return when (val response =  apiService.acceptMember(AcceptMember(userId, role))) {
            is ApiResponse.Success -> response.data?.result ?: AcceptMemberResultType.FAIL
            is ApiResponse.Error -> {
                val errMess = "Request to accept user [$userId], role [$role] was filed, current user: [${getMyId()}]"
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
                val errMess = "Request for disconnect user was filed: userId: [$userId]. current user: [${getMyId()}]"
                response.logErr(NE_TAG, errMess)
                return false
            }
        }
    }

    /**
     * Если есть изменения в профилях юзера - новые свойства сохраняются тут, так же репозиторию юзеров
     * корректируется тут. Если юзер был исключен из пространства - информация просто передается выше
     */
    private suspend fun doUpdateMembers(
        srcRole: RoleType, srcSpaceStatus: SpaceStatus, requestFunc: suspend () -> ApiResponse<MembersResult>
    ): UpdateResult {

            return when (val response = requestFunc.invoke()) {
            is ApiResponse.Success -> {
                response.data?.let {

                      /* Если юзер раньше был в пространстве, а сейчас не входит в него - говорим об этом,
                      *  если юзер и раньше и сейчас не сходит в пространство - ничего нового, просто SUCCESS */
                    if (it.spaceStatus != SpaceStatus.MEMBER) {
                        userProfile.setSpaceStatus(it.spaceStatus)
                        return if (userProfile.spaceExists()) UpdateResult.NOT_CONSIST_IN_SPACE
                        else UpdateResult.SUCCESS
                    }

                    processMembers(srcSpaceStatus, it, srcRole)
                    UpdateResult.SUCCESS
                } ?: run { UpdateResult.FAIL }
            }
            is ApiResponse.Error -> {
                response.logErr(NE_TAG, "Cannot download members from server, current user: [${getMyId()}]")
                return UpdateResult.FAIL
            }
        }
    }

    private fun processMembers(srcSpaceStatus: SpaceStatus, membersResult: MembersResult, srcRole: RoleType) {
        val currentRole = when (srcSpaceStatus) {
            membersResult.spaceStatus -> srcRole
            SpaceStatus.PENDING_BOSS -> RoleType.BOSS
            else -> RoleType.WORKER
        }
        if (srcSpaceStatus != membersResult.spaceStatus) userProfile.setSpaceStatus(membersResult.spaceStatus)
        if (srcRole != currentRole) encUserProfile.setRole(currentRole)

        membersResult.space?.let { space -> userProfile.setSpace(space) }
        memberRepository.deleteIfNotContains(membersResult.actualIds)
        if (membersResult.members.isNotEmpty()) {
            memberRepository.save(filterMembers(membersResult.members, currentRole))
            spaceViewModel.updateMembers()
        }
    }

    /**
     * Работник видит только других работников, кроме себя и не видит никакие заявки.
     * Руководители видят всех работников (не видят других руководителей, в тч себя), в тч заявки работников.
     * Создатель пространства видит всех юзеров, кроме себя. В тч люббые заявки // TODO 31.03.2025 20:30: добавить указание роли при показе креатору
     */
    private fun filterMembers(members: MutableList<SpaceMember>, currentRole: RoleType): MutableList<SpaceMember> {
        if (members.isEmpty()) return mutableListOf()

        val iam = getMyId()

        when (currentRole) {
            RoleType.CREATOR -> members.removeAt(members.indexOfFirst { it.isCreator() })
            RoleType.BOSS -> members.removeIf { it.userUuid == iam || it.isPrivileged() || it.isPendingBoss() }
            RoleType.WORKER -> members.removeIf { it.userUuid == iam || it.isPrivileged() || it.isPendingMember() }
        }
        return members
    }

    private fun getMyId() = encUserProfile.getUserUuid()
}
