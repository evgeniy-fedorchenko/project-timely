package com.efedorchenko.timely.data

import com.efedorchenko.timely.model.auth.AuthData
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.auth.SpaceKeys

interface EncUserProfile {

    fun isAuthenticated(): Boolean

    fun isPrivileged(): Boolean

    fun setAuthData(authData: AuthData)

    fun deleteAuthData()

    fun getApiToken(): String?

    fun getRole(): RoleType?

    fun getSpaceKeys(): SpaceKeys?

    fun getUserUuid(): String?
}