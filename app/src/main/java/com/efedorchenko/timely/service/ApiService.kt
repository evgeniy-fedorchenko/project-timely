package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.AuthRequest
import com.efedorchenko.timely.model.AuthResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.UUID

class ApiService {

    companion object {
        private const val RQUID = "RqUID"
        private const val AUTHORIZATION = "Authorization"
        private const val BASE_URL = "http://77.222.46.190:8080/"
        private const val USERNAME = "timely-application"
        private const val PASSWORD = "47db77ee-117c-40b7-9593-62ffce0e05d4"
        private const val LOGIN_PATH = "auth/login"
        private val APPLICATION_JSON = "application/json".toMediaType()
    }

    private val client = OkHttpClient()

    suspend fun login(authRequest: AuthRequest): AuthResponse? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BASE_URL + LOGIN_PATH)
            .header(RQUID, UUID.randomUUID().toString())
            .header(AUTHORIZATION, Credentials.basic(USERNAME, PASSWORD))
            .post(Json.encodeToString(authRequest).toRequestBody(APPLICATION_JSON))
            .build()

        val response: Response
        try {
            response = client.newCall(request).execute()
        } catch (e: Exception) {
            // Здесь можно обработать исключение, например, логировать его
            e.printStackTrace()
            return@withContext null
        }

        if (!response.isSuccessful) {
            // Можно добавить логику обработки ошибок здесь
            return@withContext null
        }

        response.body?.let { body ->
            Json.decodeFromString<AuthResponse>(body.string())
        }
    }
}