package com.efedorchenko.timely.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.efedorchenko.timely.model.auth.AuthData
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.auth.SpaceKeys
import okio.IOException

// TODO: Возможно не стоит создавать DI-модули а помечать класс @Singleton,
//  а контекст в конструкторе как @ApplicationContext
class EncProfileStorageImpl private constructor(baseContext: Context) : EncProfileStorage {

    companion object {

        // TODO: Не нужно ручное создание синглота, теперь этим занимается Hilt
        @Volatile
        private var _instance: EncProfileStorageImpl? = null

        fun getInstance(context: Context): EncProfileStorageImpl =
            _instance ?: synchronized(this) {
                _instance ?: EncProfileStorageImpl(context.applicationContext).also { _instance = it }
            }

        fun requireInstance(): EncProfileStorage {
            return _instance!!
        }

        private const val ESP_NAME = "encrypted_profile_storage"

        private const val ROLE_KEY = "user_role"
        private const val USER_ID_KEY = "user_id"
        private const val API_TOKEN_KEY = "api_token"
        private const val SPACE_BOSS_KEY_KEY = "space_access_boss_key"
        private const val SPACE_WORKER_KEY_KEY = "space_access_worker_key"
    }

    private val encSharedPref by lazy {
        try {
            create(baseContext)
        } catch (ex: IOException) {
            baseContext.getSharedPreferences(ESP_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            create(baseContext)
        }
    }

    private fun create(baseContext: Context) = EncryptedSharedPreferences.create(
        ESP_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        baseContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun isAuthenticated(): Boolean = encSharedPref.contains(ROLE_KEY)

    override fun isPrivileged(): Boolean = getRole()?.isPrivileged() == true

    override fun saveAuthData(authData: AuthData) {
        with(encSharedPref.edit()) {
            putString(API_TOKEN_KEY, authData.jwtToken)
            putString(ROLE_KEY, authData.role.name)
            putString(USER_ID_KEY, authData.userUuid)
            authData.generatedSpaceKeys?.let {
                putString(SPACE_BOSS_KEY_KEY, authData.generatedSpaceKeys.bossKey)
                putString(SPACE_WORKER_KEY_KEY, authData.generatedSpaceKeys.workerKey)
            }
            apply()
        }
    }

    override fun deleteAuthData() {
        with(encSharedPref.edit()) {
            remove(API_TOKEN_KEY)
            remove(ROLE_KEY)
            remove(SPACE_BOSS_KEY_KEY)
            remove(SPACE_WORKER_KEY_KEY)
            remove(USER_ID_KEY)
            apply()
        }
    }

    override fun saveApiToken(token: String) {
        with(encSharedPref.edit()) {
            putString(API_TOKEN_KEY, token)
            apply()
        }
    }

    override fun deleteApiToken() {
        with(encSharedPref.edit()) {
            remove(API_TOKEN_KEY)
            apply()
        }
    }

    override fun getApiToken(): String? = encSharedPref.getString(API_TOKEN_KEY, null)

    override fun saveRole(role: RoleType) {
        with(encSharedPref.edit()) {
            putString(ROLE_KEY, role.toString())
            apply()
        }
    }

    override fun deleteRole() {
        with(encSharedPref.edit()) {
            remove(ROLE_KEY)
            apply()
        }
    }

    override fun getRole(): RoleType? {
        val userRoleStr = encSharedPref.getString(ROLE_KEY, null)
        return userRoleStr?.let { RoleType.valueOf(it) }
    }

    override fun setSpaceKeys(keys: SpaceKeys) {
        with(encSharedPref.edit()) {
            putString(SPACE_BOSS_KEY_KEY, keys.bossKey)
            putString(SPACE_WORKER_KEY_KEY, keys.workerKey)
            apply()
        }
    }

    override fun deleteSpaceKeys() {
        with(encSharedPref.edit()) {
            remove(SPACE_BOSS_KEY_KEY)
            remove(SPACE_WORKER_KEY_KEY)
            apply()
        }
    }

    override fun getSpaceKeys(): SpaceKeys? {
        with(encSharedPref) {
            val workerKey = getString(SPACE_WORKER_KEY_KEY, null)
            val bossKey = getString(SPACE_BOSS_KEY_KEY, null)
            if (workerKey != null && bossKey != null) {
                return SpaceKeys(workerKey, bossKey)
            }
            return null
        }
    }

    override fun getSpaceBossKey(): String? =
        encSharedPref.getString(SPACE_BOSS_KEY_KEY, null)

    override fun getSpaceWorkerKey(): String? =
        encSharedPref.getString(SPACE_WORKER_KEY_KEY, null)


    override fun saveUserUuid(userUuid: String) {
        with(encSharedPref.edit()) {
            putString(USER_ID_KEY, userUuid)
            apply()
        }
    }

    override fun deleteUserUuid() {
        with(encSharedPref.edit()) {
            remove(USER_ID_KEY)
            apply()
        }
    }

    override fun getUserUuid(): String? = encSharedPref.getString(USER_ID_KEY, null)

}
