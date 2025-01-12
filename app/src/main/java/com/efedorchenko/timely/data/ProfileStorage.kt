package com.efedorchenko.timely.data

import com.efedorchenko.timely.model.auth.UserData

interface ProfileStorage {

    fun saveName(name: String)

    fun deleteName()

    fun getName(): String?

    fun saveRate(rate: Int)

    fun deleteRate()

    fun getRate(): Int?

    fun savePosition(position: String)

    fun deletePosition()

    fun getPosition(): String?

    fun saveUserData(userData: UserData)

    fun deleteUserData()

    fun getUserData(): UserData?

    fun getSpaceName(): String?

    fun spaceExists(): Boolean

    fun deleteSpace()
}
