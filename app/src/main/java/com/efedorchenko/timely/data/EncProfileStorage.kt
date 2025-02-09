package com.efedorchenko.timely.data

import com.efedorchenko.timely.model.auth.AuthData
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.auth.SpaceKeys

interface EncProfileStorage {

    fun isAuthenticated(): Boolean

    fun isPrivileged(): Boolean

    fun saveAuthData(authData: AuthData)

    fun deleteAuthData()

    fun getApiToken(): String?

    fun saveRole(role: RoleType)

    fun getRole(): RoleType?

    fun getSpaceKeys(): SpaceKeys?

    fun getUserUuid(): String?
}