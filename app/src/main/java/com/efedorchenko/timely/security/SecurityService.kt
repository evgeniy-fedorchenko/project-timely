package com.efedorchenko.timely.security

import com.efedorchenko.timely.model.UserRole

interface SecurityService {

    fun isAuthenticated(): Boolean

    fun isPrivileged(): Boolean

    fun authorize(): UserRole?

    fun getApiCreds(): Pair<String, String>?

    fun setApiCreds(creds: Pair<String, String>)

    fun saveToken(userToken: String, role: UserRole)

    fun removeToken()

    fun requireAccessKeys(): Pair<String, String>

}