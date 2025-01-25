package com.efedorchenko.timely.service

interface DataService {

    suspend fun loadData(userUuid: String? = null): Boolean

    suspend fun updateData(userUuid: String?): Boolean
}