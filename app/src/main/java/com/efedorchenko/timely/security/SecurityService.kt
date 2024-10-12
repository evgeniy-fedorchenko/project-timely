package com.efedorchenko.timely.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.efedorchenko.timely.model.UserRole


class SecurityService(private val baseContext: Context) {

    companion object {
        private const val ESP_NAME: String = "auth_data"
        private const val USER_ROLE_KEY: String = "user_role"
        private const val API_CREDS: String = "server_api_credentials"
        private const val CREDS_DELIMETER = ":::"
        private const val TOKEN_KEY = "user_token"

    }

    private val encSharedPref by lazy {
        EncryptedSharedPreferences.create(
            ESP_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            baseContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isUserAuthenticated(): Boolean {
        return encSharedPref.contains(USER_ROLE_KEY)
    }

    fun authorize(): UserRole? {
        val userRoleStr = encSharedPref.getString(USER_ROLE_KEY, null)
        return userRoleStr?.let { UserRole.valueOf(it) }
    }

    fun getApiCreds(): Pair<String, String>? {
        val creds = encSharedPref.getString("auth_token", null)
        val split = creds?.split(CREDS_DELIMETER)
        if (split != null && split.size == 2) {
            return Pair(split[0], split[1])
        }
        return null;
    }

    fun setApiCreds(creds: Pair<String, String>) {
        with(encSharedPref.edit()) {
            putString("basic_auth_credentials", creds.first + CREDS_DELIMETER + creds.second)
            apply()
        }
    }

    fun saveToken(userToken: String, role: UserRole) {
        with(encSharedPref.edit()) {
            putString(TOKEN_KEY, userToken)
            putString(USER_ROLE_KEY, role.name)
            apply()
        }
    }

    fun removeToken() {
        with(encSharedPref.edit()) {
            remove(TOKEN_KEY)
            remove(USER_ROLE_KEY)
            apply()
        }
    }
}
