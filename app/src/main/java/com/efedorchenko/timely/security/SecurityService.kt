package com.efedorchenko.timely.security

import com.efedorchenko.timely.model.auth.AuthData
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.auth.SpaceKeys

interface SecurityService {

    // TODO: Разобраться какие методы не нужны

    fun isAuthenticated(): Boolean

    fun isPrivileged(): Boolean

    fun saveAuthData(authData: AuthData)

    fun deleteUserData()

    fun saveApiToken(token: String)

    fun deleteApiToken()

    fun getApiToken(): String?

    fun saveRole(role: RoleType)

    fun deleteRole()

    fun getRole(): RoleType?

    fun setSpaceKeys(keys: SpaceKeys)

    fun deleteSpaceKeys()

    fun getSpaceKeys(): SpaceKeys?

    fun getSpaceBossKey(): String?

    fun getSpaceWorkerKey(): String?

    fun saveUserUuid(userUuid: String)

    fun deleteUserUuid()

    fun getUserUuid(): String?
}