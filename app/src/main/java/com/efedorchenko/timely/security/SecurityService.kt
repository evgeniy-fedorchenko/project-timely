package com.efedorchenko.timely.security

import com.efedorchenko.timely.model.UserRole

interface SecurityService {

    fun isUserAuthenticated(): Boolean

    fun authorize(): UserRole?

    fun getApiCreds(): Pair<String, String>?

    fun setApiCreds(creds: Pair<String, String>)

    fun saveToken(userToken: String, role: UserRole)

    fun removeToken()

    fun isPrivileged(): Boolean

}