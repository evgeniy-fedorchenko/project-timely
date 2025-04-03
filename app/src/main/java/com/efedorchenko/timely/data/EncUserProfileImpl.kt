package com.efedorchenko.timely.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.efedorchenko.timely.model.auth.AuthData
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.auth.SpaceKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import okio.IOException
import javax.inject.Inject

class EncUserProfileImpl @Inject constructor(@ApplicationContext context: Context) : EncUserProfile {

    companion object {
        private const val ESP_NAME = "encrypted_profile_storage"

        private const val ROLE_KEY = "user_role"
        private const val USER_ID_KEY = "user_id"
        private const val API_TOKEN_KEY = "api_token"
        private const val SPACE_BOSS_KEY_KEY = "space_access_boss_key"
        private const val SPACE_WORKER_KEY_KEY = "space_access_worker_key"
    }

    private val encSharedPref by lazy {
        try {
            create(context)
        } catch (ex: IOException) {
            context.getSharedPreferences(ESP_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            create(context)
        }
    }

    private fun create(context: Context) = EncryptedSharedPreferences.create(
        ESP_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun isAuthenticated(): Boolean = encSharedPref.contains(ROLE_KEY)

    override fun isPrivileged(): Boolean = getRole().isPrivileged()

    override fun detachFromSpace() {
        with(encSharedPref.edit()) {
            putString(ROLE_KEY, RoleType.WORKER.toString())
            remove(SPACE_BOSS_KEY_KEY)
            remove(SPACE_WORKER_KEY_KEY)
            apply()
        }
    }

    override fun setAuthData(authData: AuthData) {
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

    override fun getApiToken(): String? = encSharedPref.getString(API_TOKEN_KEY, null)

    override fun getRole(): RoleType {
        val userRoleStr = encSharedPref.getString(ROLE_KEY, null)
        return userRoleStr?.let { RoleType.valueOf(it) } ?: run { RoleType.WORKER }
    }

    override fun setRole(roleType: RoleType) {
        encSharedPref.edit().putString(ROLE_KEY, roleType.toString()).apply()
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

    override fun getUserUuid(): String? = encSharedPref.getString(USER_ID_KEY, null)

}
