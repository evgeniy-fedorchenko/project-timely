package com.efedorchenko.timely.data

import com.efedorchenko.timely.model.UserData

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

    fun getUserData(): UserData?

    fun deleteUserData()
}
