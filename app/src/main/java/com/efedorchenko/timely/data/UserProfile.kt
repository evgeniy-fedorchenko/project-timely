package com.efedorchenko.timely.data

import com.efedorchenko.timely.model.auth.SpaceDto
import com.efedorchenko.timely.model.auth.UserData
import com.efedorchenko.timely.model.member.SpaceStatus

interface UserProfile {

    fun getName(): String?

    fun setUserData(userData: UserData)

    fun deleteUserData()

    fun getUserData(): UserData?

    fun setSpace(space: SpaceDto)

    fun getSpaceName(): String?

    fun getSpaceStatus(): SpaceStatus

    fun setSpaceStatus(status: SpaceStatus)

    fun spaceExists(): Boolean

    fun deleteSpace()

    fun detachFromSpace()
}
