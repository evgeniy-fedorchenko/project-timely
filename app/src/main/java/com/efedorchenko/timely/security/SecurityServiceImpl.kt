package com.efedorchenko.timely.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.efedorchenko.timely.model.auth.RoleType
import java.util.*


class SecurityServiceImpl private constructor(baseContext: Context) : SecurityService {

    companion object {

        @Volatile
        private var _instance: SecurityServiceImpl? = null

        fun getInstance(context: Context): SecurityServiceImpl =
            _instance ?: synchronized(this) {
                _instance ?: SecurityServiceImpl(context.applicationContext).also { _instance = it }
            }

        fun requireInstance(): SecurityService {
            return _instance!!
        }

        private const val ESP_NAME: String = "security_data"
        private const val USER_ROLE_KEY: String = "user_role"
        private const val API_TOKEN_KEY = "user_api_token"
        private const val USER_ID_KEY = "user_id"
        private const val SPACE_ACCESS_KEYS_KEY = "access_keys"
//        private const val PAIR_DELIMITER = ":::"
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

    override fun isAuthenticated(): Boolean {
        return encSharedPref.contains(USER_ROLE_KEY)
    }

    override fun isPrivileged(): Boolean = authorize()?.isPrivileged() == true

    override fun authorize(): RoleType? {
        val userRoleStr = encSharedPref.getString(USER_ROLE_KEY, null)
        return userRoleStr?.let { RoleType.valueOf(it) }
    }

    override fun saveApiToken(token: String) {
        with(encSharedPref.edit()) {
            putString(API_TOKEN_KEY, token)
            apply()
        }
    }

    override fun saveRole(role: RoleType) {
        TODO("Not yet implemented")
    }

    override fun removeToken() {
        with(encSharedPref.edit()) {
            remove(API_TOKEN_KEY)
            apply()
        }
    }

    override fun removeRole() {
        TODO("Not yet implemented")
    }

    override fun getAccessKeys(): Pair<String, String> {
        TODO("Not yet implemented")
    }

    override fun saveUserId(userId: UUID) {
        with(encSharedPref.edit()) {
            putString(USER_ID_KEY, userId.toString())
            apply()
        }
    }

//    override fun getAccessKeys(): Pair<String, String> {
//        val keysString = encSharedPref.getString(SPACE_ACCESS_KEYS_KEY, null)
//            ?: return generateAndSaveKeys()
//
//        return keysString.split(PAIR_DELIMITER).takeIf { it.size == 2 }?.let {
//            Pair(it[0], it[1])
//        } ?: generateAndSaveKeys()
//    }
}
