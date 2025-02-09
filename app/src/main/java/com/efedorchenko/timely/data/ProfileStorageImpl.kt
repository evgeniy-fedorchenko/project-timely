package com.efedorchenko.timely.data

import android.content.Context
import com.efedorchenko.timely.model.auth.SpaceDto
import com.efedorchenko.timely.model.auth.UserData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ProfileStorageImpl @Inject constructor(@ApplicationContext context: Context) : ProfileStorage {

    companion object {
        private const val PSP_NAME = "profile_storage"
        private const val NAME_KEY = "user_name"
        private const val RATE_KEY = "user_rate"
        private const val POSITION_KEY = "user_position"
        private const val SPACE_NAME_KEY = "space_name_where_user_consist"
    }

    private val sharedPref by lazy {
        context.getSharedPreferences(PSP_NAME, Context.MODE_PRIVATE)
    }

    override fun getName(): String? = sharedPref.getString(NAME_KEY, null)

    override fun saveUserData(userData: UserData) {
        with(sharedPref.edit()) {
            putString(NAME_KEY, userData.name)
            putString(POSITION_KEY, userData.position)
            userData.spaceName?.let { putString(SPACE_NAME_KEY, userData.spaceName) }
            userData.rate?.let { putInt(RATE_KEY, it) }
            apply()
        }
    }

    override fun deleteUserData() {
        with(sharedPref.edit()) {
            remove(NAME_KEY)
            remove(POSITION_KEY)
            remove(RATE_KEY)
            remove(SPACE_NAME_KEY)
            apply()
        }
    }

    override fun getUserData(): UserData? {
        with(sharedPref) {
            val name = getString(NAME_KEY, null)
            val position = getString(POSITION_KEY, null)
            val rate = getInt(RATE_KEY, -1)
            val spaceName = getString(SPACE_NAME_KEY, null)

            if (name != null && position != null) {
                val nullableRate = if (rate == -1) null else rate
                return UserData(name, position, nullableRate, spaceName)
            }
            return null
        }
    }

    override fun saveSpace(space: SpaceDto) {
        with(sharedPref.edit()) {
            putString(SPACE_NAME_KEY, space.name)
            apply()
        }
    }

    override fun getSpaceName(): String? = sharedPref.getString(SPACE_NAME_KEY, null)

    override fun spaceExists() = sharedPref.contains(SPACE_NAME_KEY)

    override fun deleteSpace() {
        with(sharedPref.edit()) {
            remove(SPACE_NAME_KEY)
            apply()
        }
    }
}
