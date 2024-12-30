package com.efedorchenko.timely.data

import android.content.Context
import com.efedorchenko.timely.model.UserData

class ProfileStorageImpl(context: Context) : ProfileStorage {

    companion object {
        private const val PSP_NAME = "profile_storage"

        private const val NAME_KEY = "user_name"
        private const val RATE_KEY = "user_rate"
        private const val POSITION_KEY = "user_position"
    }

    private val sharedPref by lazy {
        context.getSharedPreferences(PSP_NAME, Context.MODE_PRIVATE)

    }

    override fun saveName(name: String) {
        with(sharedPref.edit()) {
            putString(NAME_KEY, name)
            apply()
        }
    }

    override fun deleteName() {
        with(sharedPref.edit()) {
            remove(NAME_KEY)
            apply()
        }
    }

    override fun getName(): String? = sharedPref.getString(NAME_KEY, null)

    override fun saveRate(rate: Int) {
        with(sharedPref.edit()) {
            putInt(RATE_KEY, rate)
            apply()
        }
    }

    override fun deleteRate() {
        with(sharedPref.edit()) {
            remove(RATE_KEY)
            apply()
        }
    }

    override fun getRate(): Int? {
        val value = sharedPref.getInt(RATE_KEY, -1)
        return if (value != -1) value else null
    }

    override fun savePosition(position: String) {
        with(sharedPref.edit()) {
            putString(POSITION_KEY, position)
            apply()
        }
    }

    override fun deletePosition() {
        with(sharedPref.edit()) {
            remove(POSITION_KEY)
            apply()
        }
    }

    override fun getPosition(): String? = sharedPref.getString(POSITION_KEY, null)

    override fun getUserData(): UserData? {
        with(sharedPref) {
            val name = getString(NAME_KEY, null)
            val position = getString(POSITION_KEY, null)
            val rate = getInt(RATE_KEY, -1)

            if (name != null && position != null) {
                return UserData(name, position, rate)
            }
            return null
        }
    }

    override fun deleteUserData() {
        with(sharedPref.edit()) {
            remove(NAME_KEY)
            remove(POSITION_KEY)
            remove(RATE_KEY)
            apply()
        }
    }
}
