package com.efedorchenko.timely.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys


class SecurityService(private val baseContext: Context) {

    companion object {
        private const val ESP_NAME: String = "auth_data"
        private const val USER_ROLE_KEY: String = "user_role"
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
        return false
//        return encSharedPref.contains(USER_ROLE_KEY)
    }

    fun authorize(): UserRole? {
        val userRoleStr = encSharedPref.getString(USER_ROLE_KEY, null)
        return userRoleStr?.let { UserRole.valueOf(it) }
    }

}
