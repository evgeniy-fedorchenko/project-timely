package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.SaveResult

interface DataService {

    suspend fun loadData(userUuid: String? = null): Boolean

    suspend fun updateData(userUuid: String?): Boolean

    suspend fun saveData(data: AbstractData, userUuid: String? = null): SaveResult

    suspend fun sendData(data: AbstractData, userUuid: String? = null): SaveResult
}