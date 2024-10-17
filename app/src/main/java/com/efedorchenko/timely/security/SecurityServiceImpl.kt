package com.efedorchenko.timely.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.efedorchenko.timely.model.UserRole
import java.util.UUID


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

        private const val ESP_NAME: String = "auth_data"
        private const val USER_ROLE_KEY: String = "user_role"
        private const val TOKEN_KEY = "user_token"
        private const val ACCESS_KEYS_KEY = "access_keys"
        private const val API_CREDS_KEY: String = "server_api_credentials"
        private const val PAIR_DELIMITER = ":::"
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

    override fun isAuthenticated(): Boolean = encSharedPref.contains(USER_ROLE_KEY)

    override fun isPrivileged(): Boolean = authorize()?.isPrivileged() ?: false

    override fun authorize(): UserRole? {
        val userRoleStr = encSharedPref.getString(USER_ROLE_KEY, null)
        return userRoleStr?.let { UserRole.valueOf(it) }
    }

    override fun getApiCreds(): Pair<String, String>? {
        val creds = encSharedPref.getString("auth_token", null)
        val split = creds?.split(PAIR_DELIMITER)
        if (split != null && split.size == 2) {
            return Pair(split[0], split[1])
        }
        return null;
    }

    override fun setApiCreds(creds: Pair<String, String>) {
        with(encSharedPref.edit()) {
            putString("basic_auth_credentials", creds.first + PAIR_DELIMITER + creds.second)
            apply()
        }
    }

    override fun saveToken(userToken: String, role: UserRole) {
        with(encSharedPref.edit()) {
            putString(TOKEN_KEY, userToken)
            putString(USER_ROLE_KEY, role.name)
            apply()
        }
    }

    override fun removeToken() {
        with(encSharedPref.edit()) {
            remove(TOKEN_KEY)
            remove(USER_ROLE_KEY)
            apply()
        }
    }

    override fun requireAccessKeys(): Pair<String, String> {
        val keysString = encSharedPref.getString(ACCESS_KEYS_KEY, null)
            ?: return generateAndSaveKeys()

        return keysString.split(PAIR_DELIMITER).takeIf { it.size == 2 }?.let {
            Pair(it[0], it[1])
        } ?: generateAndSaveKeys()
    }

    private fun generateAndSaveKeys(): Pair<String, String> {

        val workerKey = "worker" + UUID.randomUUID().toString().substring(8)
        val adminKey = "admin" + UUID.randomUUID().toString().substring(8)

        encSharedPref.edit().putString(ACCESS_KEYS_KEY, "${workerKey}$PAIR_DELIMITER${adminKey}").apply()
        return Pair(workerKey, adminKey)
    }
}
