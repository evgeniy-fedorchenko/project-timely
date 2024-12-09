package com.efedorchenko.timely.security

import com.efedorchenko.timely.model.auth.RoleType
import java.util.*

interface SecurityService {

    fun isAuthenticated(): Boolean

    fun isPrivileged(): Boolean

    fun authorize(): RoleType?

    fun saveApiToken(token: String)

    fun saveRole(role: RoleType)

    fun removeToken()

    fun removeRole()

    fun getAccessKeys(): Pair<String, String>

    fun saveUserId(userId: UUID)

}