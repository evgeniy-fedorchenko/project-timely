package com.efedorchenko.timely.data

import com.efedorchenko.timely.model.auth.SpaceDto
import com.efedorchenko.timely.model.auth.UserData

interface ProfileStorage {

    fun getName(): String?

    fun saveUserData(userData: UserData)

    fun deleteUserData()

    fun getUserData(): UserData?

    fun saveSpace(space: SpaceDto)

    fun getSpaceName(): String?

    fun spaceExists(): Boolean

    fun deleteSpace()
}
